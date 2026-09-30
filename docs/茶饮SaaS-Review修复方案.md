# 茶饮SaaS — Review 修复方案

> 版本：v1.0（2026-09-14）
> 来源：全项目 review（红线静态扫描 + 批次3/4核心链路精读）
> 状态：**待评审，未动任何代码**
> 关联提交：cfc037d（会员/营销/报表）、57e204b + bc080d8（订单/支付）

## 0. 背景摘要

红线静态检查 9/9 通过（金额分/Redis前缀/幂等/BizException/构造器注入/事务/script setup/Result/多租户），
`mvn -q compile` 通过。问题集中在两类盲区：

1. **无登录态上下文**（网关回调、C 端白名单）下的多租户与身份缺失；
2. **应用层幂等**（查重-后写）缺数据库唯一约束兜底，以及个别竞态窗口。

---

## 1. [P1] 支付回调租户上下文缺失

### 1.1 问题

- `/api/notify/**` 在 SaToken 白名单（`SaTokenConfig.java:32`），微信网关回调无登录态；
- `SaInterceptor` 仅在 `StpUtil.isLogin()` 时注入 `TenantContext`（`SaTokenConfig.java:44-47`）；
- 租户插件对 null 上下文兜底 `tenant_id = 0`（`MybatisPlusConfig.java:47-50`）；
- `PayPaymentServiceImpl.handleCallback:187` 首句按 `payment_no` 查流水，被插件改写为
  `WHERE payment_no = ? AND tenant_id = 0` → 真实流水（tenant_id ≠ 0）永远查不到 →
  抛 `PAYMENT_NOT_FOUND` → 微信重推 → 永远失败。

**为何至今未暴露**：登录会话下手工调 NotifyController 时上下文恰好有租户，能跑通；
纯网关调用（无 token）必挂。`retryPendingTasks` 按租户迭代注入上下文（:237），不受影响，
即"重试能成功、即时处理必失败"的隐性状态。

### 1.2 修复方案（推荐 A）

**方案 A：按单号反查租户，全链路显式注入**

1. `PayPaymentMapper` 新增（或复用）按 `payment_no` 查询方法，标注
   `@InterceptorIgnore(tenantLine = "true")` + 注释声明（对齐
   `SysUserMapper.selectByUsernameForLogin` 的既有评审惯例）；
2. `handleCallback` 改用该方法查流水 → 拿到 `payment.getTenantId()` 后
   `TenantContext.setTenantId(...)` → 后续即时处理（processTask / markPaymentSuccess /
   积分发放）全部在正确租户上下文内执行；
3. 回调入口包 try/finally `TenantContext.clear()`（防线程复用串租户，多租户红线）；
4. 落 `pay_callback_task` 时 task.tenantId 取自流水（现有代码已如此，:197），重试链路无感知。

- 影响面：仅 `PayPaymentServiceImpl.handleCallback` + Mapper 一处声明，不动表结构；
- 风险：低。绕过通道唯一化且注释声明，符合规范 6.1 的例外管理。

**方案 B（备选，不建议）**：回调统一改造为"仅落表不应答处理"，即时处理逻辑全部交给
`retryPendingTasks`。改动更小但回调延迟从毫秒变分钟级，体验下降。

### 1.3 验证清单

- [ ] 未登录态（无 token、无 X-Tenant-Id）POST `/api/notify/wx-pay/callback`，
      断言订单/流水正确置位、积分发放到正确租户会员；
- [ ] 回调后 ThreadLocal 清理（压测并发回调 + 紧随的 B 端请求，断言无串租户）；
- [ ] 金额不符 / 流水不存在仍按原错误码拒绝。

---

## 2. [P1] C 端会员身份纯靠 X-Member-Id 头（会话化提前）

### 2.1 问题

`/api/app/v1/members|orders|coupons|payments/**` 全在白名单，身份即请求头。
伪造 `X-Tenant-Id + X-Member-Id` 可：查任意会员钱包、**花其储值下单**、领券核销、看积分。
mock 阶段可接受，但钱包是资金语义，HANDOFF 已列为二期（"C 端会话化"），建议提前。

### 2.2 修复方案（轻量 token，不动微信侧）

1. `POST /api/app/v1/auth/wx-login`（已有，mock：code=openid 直换 memberId）在返回
   memberId 的同时生成**临时 token**：`UUID`，写 Redis
   `teasaas:member:token:{token}` → `{tenantId, memberId}`，TTL 7 天（可配置）；
2. 新增 C 端拦截器（或扩展现有 `SaTokenConfig` 逻辑）：
   - 白名单收缩为：`wx-login`、`stores/nearby`、`stores/*/products`、`/api/notify/**`；
   - 其余 `/api/app/**` 请求要求 `Authorization: Bearer <token>`（或自定义头 `X-Token`），
     校验通过后注入 `TenantContext` + `MemberContext`（新增 ThreadLocal，含清理拦截器）；
   - 兼容期（一周或一个迭代）同时接受 X-Tenant-Id/X-Member-Id 并打 WARN 日志，
     之后硬切；
3. C 端各 App*Controller 删除手动 `TenantContext.setTenantId(tenantId)` 与
   `X-Member-Id` 解析，统一从 `MemberContext` 取（约 6 处：AppOrder/AppPayment/
   AppMember/AppAuth/AppCoupon/PrdListing 的 C 端方法）；
4. `mini-program/` 未初始化，无前端改造负担；Swagger 调试改用 wx-login 先换 token。

- 影响面：新增 1 个拦截器 + MemberContext + Redis 键（走 `RedisConstants`，红线 2），
  改 6 个 Controller 的身份来源；**对外契约变化**（新增必填头），需同步接口需求文档；
- 风险：中低。注意 token 与 SaToken 体系并存互不干扰（C 端独立拦截器，不进 StpUtil）。

### 2.3 验证清单

- [ ] 无 token / 过期 token / 伪造 token → 401（业务码对齐 NotLoginException 口径）；
- [ ] 正常 token → 租户/会员注入正确，下单扣储值归属正确；
- [ ] Redis 键前缀 `teasaas:`；TTL 生效；
- [ ] 兼容期内 X-Member-Id 仍可用且打 WARN。

---

## 3. [P2] 幂等唯一索引 + 竞态守卫（一组小改）

### 3.1 流水表唯一键兜底（库存/积分）

**现状**：`InvStockServiceImpl.alreadyLogged` 与 `MemPointsServiceImpl.alreadyLogged`
均为 check-then-act，`inv_stock_log` 仅普通索引 `idx_stock_log_biz(tenant_id, biz_no)`
（01_schema.sql:476），`mem_points_log` 无键；并发重复调用可穿透 → 库存双扣/积分双记。

**方案**：

```sql
-- 01_schema.sql 增量（容器同步执行 ALTER）
ALTER TABLE inv_stock_log
  ADD UNIQUE KEY uk_stock_log_biz (tenant_id, biz_no, biz_type);
ALTER TABLE mem_points_log
  ADD UNIQUE KEY uk_points_log_biz (tenant_id, biz_no, biz_type);
```

- MySQL 唯一索引对 NULL biz_no 允许多行，不影响无业务号的流水；
- 代码侧：两处 `writeLog/insert` 捕获 `DuplicateKeyException` 按"已处理"静默收尾
  （对齐 `getOrCreateWallet` 的既有兜底风格）；`alreadyLogged` 查重保留（省一次异常开销）；
- 盘点（bizType=5）与采购入库（bizType=4）的 biz_no 生成规则需确认同号不复用
  （盘点单号/采购单号天然唯一，无冲突）。

### 3.2 scheduleRetry 状态守卫

**现状**：`PayPaymentServiceImpl.scheduleRetry:395-403` 按 id 无条件 update，
竞态下可把已成功（status=2）任务覆盖 retry_count，极端时误置 TASK_FAILED。

**方案**：wrapper 追加 `.eq(PayCallbackTask::getStatus, TASK_PENDING)`（一行）。

### 3.3 积分防并发刷负

**现状**：`MemPointsServiceImpl.useForOrder:77` 先 SUM 再判余额再写流水，同会员两笔
并发单可双双通过 → SUM 为负。券（条件更新）/钱包（balance>=amount）均有保护，唯独积分没有。

**方案（推荐 A）**：

- **A. 会员行悲观锁**：`useForOrder` 开头
  `SELECT id FROM mem_member WHERE id=? FOR UPDATE`（Mapper 注解一句），事务内串行化，
  改动最小；会员单行热点低，锁冲突可忽略。
- B. 冗余可用积分字段条件更新：`mem_member` 加 `available_points`，
  `SET available_points = available_points - N WHERE available_points >= N`。
  读性能最好但引入余额字段后 `SUM(log)` 与余额需对账，复杂度上升——一期不建议。

**验证清单**：

- [ ] 并发 20 线程同 bizNo 调 `deduct`/`useForOrder`，断言仅一次生效；
- [ ] 并发双单同会员各抵扣 90% 余额，断言第二笔按不足拒绝、SUM ≥ 0；
- [ ] 回调重试与即时处理并发，断言任务状态不回退。

---

## 4. [P3] 次要项（随迭代顺带）

| # | 位置 | 问题 | 建议 |
|---|---|---|---|
| 1 | `PayPaymentServiceImpl.genNo:429` | ~~6位随机日容量90万，撞号仅重试一次~~ | ✅ 已修（M12 247a32f：两处改重试循环上限3次） |
| 2 | `MemWalletServiceImpl.payForOrder:91` | ~~`setSql("... - " + amount)` 拼接~~ | ✅ 已修（M12 247a32f：钱包三处+积分累计共四处改 `{0}` 占位） |
| 3 | `MemWalletServiceImpl.addBalance:118` | ~~balanceAfter 读改写近似值~~ | ✅ 已修（M12 247a32f：update 后 selectById 回读真实值） |
| 4 | 全后端 | ~~零测试覆盖~~ | ✅ 已补（M12 247a32f：tea-server/src/test 资金链路并发幂等集成测试 19 例——并暴露修复钱包 RR 快照兜底、积分扣减锁时序两处真缺陷） |

---

## 5. 实施顺序与批次建议

| 批次 | 内容 | 动表 | 动契约 |
|---|---|---|---|
| 批次 R1 | §1 回调租户修复 + §3.2 状态守卫 | 否 | 否 |
| 批次 R2 | §3.1 唯一索引 + §3.3 积分锁（方案A） | 是（2 条 ALTER） | 否 |
| 批次 R3 | §2 C 端会话化 | 否（Redis） | **是**（接口需求文档需升版） |
| 随迭代 | §4 次要项 | — | — |

每批次收尾走 CLAUDE.md 固定流程：`mvn -q compile` → 红线审查 →
`git commit -m "fix(<scope>): <subject> F-XXX"` → 飞书同步 → 摘要输出。
R1/R2 建议挂在 F-PAY-01/F-INV-02/F-MEM-02 既有编号下；R3 涉及契约升版，
建议独立编号（如 F-AUTH-02）并在接口需求文档补 1.x 章节。

## 6. 评审关注点（请拍板）

1. §1 方案 A 的绕过通道（回调专用 `@InterceptorIgnore`）是否放行；
2. §2 token 有效期（7 天）与兼容期时长（一个迭代？）；
3. §3.1 两条 ALTER 上容器的时间窗（是否随 R1 一起做，减少一次发版）；
4. §3.3 选 A（悲观锁）还是 B（冗余字段）——本方案推荐 A。
