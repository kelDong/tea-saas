# HANDOFF.md — 交接状态快照

> 写于：M3 批次 4 收官（会员/营销/报表已提交，契约 F-XXX 后端接口全量落地）。接手者：Claude Code（或其他 AI 工具）。
> 配套入口：`CLAUDE.md`（必读）。

## 1. Git 位置（tea-saas 仓库）

```
d5c5190 chore(tools): 飞书任务清单同步——批次4会员/营销/报表t100021完成记录
cfc037d feat(member/marketing/report): 会员/营销/报表三模块——券积分储值联动下单支付全链路 F-MEM-01~03/F-MKT-01/F-RPT-01~02
bc080d8 fix(payment): 回调处理自调用绕过事务代理——改TransactionTemplate显式包事务 F-PAY-01（review 发现：handleCallback/retryPendingTasks 直调 this.processTask 致事务注解失效）
57e204b feat(order/payment/job): 订单状态机+下单扣库存+POS收银；支付mock网关/回调本地表重试/整单退款/日终对账 F-ORD-01~04 F-PAY-01~03
0b526f5 feat(product/inventory): 商品/上架/库存按契约落地——可售判定13.2实时SQL+下单扣减13.1条件更新幂等+盘点留痕+采购入库联动 F-PRD-01~03/F-INV-01~03
37a5c4e docs(handoff): 进度快照更新至M3批次2启动——批次1三模块+framework修复已入档
06aa282 chore(tools): 飞书任务清单同步——M3批次1三任务完成记录（BOM/加盟/供应链）
754668b fix(framework): 补@MapperScan跨模块mapper注册——主类包外扫描盲区致启动失败（M2遗留，冒烟暴露） M3
7f3f3b5 feat(supply): 供应商/分级价格/采购单——等级计价快照+条件更新状态机（R6发起P1-1/R4确认发货/R6R7入库） F-SUP-01~04
96cdb60 feat(franchise): 加盟商/门店档案+开通账号R6绑定+C端附近门店（X-Tenant-Id手动租户注入） F-FRA-01/02
913a5e8 feat(bom): 原料/配方/毛利试算按契约落地——版本递增/明细全量重绑/复制递推/发布 F-BOM-01/02/03
（更早：5f1dc72 docs入库 / 2b78e41 generator / 34b9fb2 tea-system / admin-web 骨架等）
```

## 2. 里程碑进度

### M2 —— ✅ 全部收官

| 任务 | 状态 | 飞书 task_id |
|---|---|---|
| Maven 15 模块骨架 / SQL 39 表 / tea-system / 生成器 / admin-web 骨架 | ✅ 已提交 | t100001/t100007/t100008/t100009 ✅ |
| 并行：种子访谈 + 微信资质 | ⬜ Owner 侧（提醒） | t100010/t100011 |

### M3 批次 1 —— ✅ 已提交（2026-09-09）

| 模块 | 接口 | commit | 飞书 |
|---|---|---|---|
| BOM | F-BOM-01/02/03 | 913a5e8 | t100013 ✅ |
| 加盟门店 | F-FRA-01/02 | 96cdb60 | t100014 ✅ |
| 供应链 | F-SUP-01~04 | 7f3f3b5 | t100015 ✅ |
| framework 修复 | @MapperScan 跨模块注册 | 754668b | — |

### M3 批次 2 —— ✅ 已提交（2026-09-10）

| 模块 | 接口 | commit | 飞书 |
|---|---|---|---|
| 商品（分类/商品/规格/上架/C端商品） | F-PRD-01/02/03 | 0b526f5 | t100016 ✅ |
| 库存（台账/盘点/预警/总览/扣减内核） | F-INV-01/02/03 | 0b526f5 | t100017 ✅ |

**落地要点**（细节见 commit diff）：
- 可售判定：`PrdListingMapper.xml` 实时 SQL（13.2，`NOT EXISTS` 配方明细缺口）；`sellable_flag` 一期不落地
- 扣减内核：`InvStockService`（tea-inventory）——deduct/restore/inbound，`biz_no+biz_type` 查日志幂等 + 条件更新 `stock_qty >= need`（13.1）；**批次 3 tea-order 直接注入调用，勿重复造**
- 盘点：实盘覆盖 + version+1 + 流水留痕（bizType=5，StockBizType 常量 1~5）
- 采购入库联动：`SupPurchaseOrderServiceImpl.receive()` 已接 `invStockService.inbound(...)`（批次 1 遗留 TODO 清偿）
- C 端公开接口：`GET /api/app/v1/stores/{id}/products`（X-Tenant-Id 注入，白名单已加）
- 数量字段 DECIMAL(10,3) → BigDecimal（红线只禁**金额**用 BigDecimal）；金额一律 Integer 分
- 模块依赖新增：tea-product +tea-franchise；tea-supply +tea-inventory（均无环）
- 删除 4 个无契约脚手架 Controller（规格/选项×2、库存台账/流水×2），端点统一收敛到契约路径

### M3 批次 3 —— ✅ 已提交（2026-09-10）

| 模块 | 接口 | commit | 飞书 |
|---|---|---|---|
| 订单（C端下单/试算/取消/B端状态机/POS收银） | F-ORD-01~04 | 57e204b | t100018 ✅ |
| 支付（mock网关/回调重试/退款/对账）+ tea-job | F-PAY-01~03 | 57e204b | t100019 ✅ |

**落地要点**（细节见 commit diff）：
- 下单即扣库存：tea-order 注入 `InvStockService.deduct`（bizNo=orderNo，幂等内建）；超时取消/退款回补走 `OrdOrderService.restoreOrderStock(orderId, bizType)`（按明细 formula 快照反算，供 tea-payment 复用）
- 状态机全条件更新（`.eq(status, from)`），affected=0 即 ORDER_STATUS_NOT_ALLOWED；1→2→3→4→5，6/7 终态
- 支付成功顺序：**订单先 1→2，流水后 1→2**；订单已取消后款到 → 流水关闭(4) 留对账人工退款
- 回调（P1-3 不引 MQ）：验签(mock跳过)→金额核对→落 `pay_callback_task`→即时处理；失败指数退避 2^n 分钟，max_retry=5 置终态；处理待重试也答 SUCCESS（防微信重推）
- mock 网关：`tea.pay.mock=true`（yml）；JSAPI 返回 payParams、Native 返回 codeUrl；退款即时成功；**正式接入=t100011 资质下发后置 false**
- POS 渠道映射：入参 1现金/2微信Native/3储值 → 订单表 1微信/2储值/3现金（现金含找零校验）
- 对账无状态按日实时：已支付订单(pay_time) vs 成功流水(callback_time) 逐单匹配 + 库存 SUM(change_qty)=stock_qty（`ReconcileMapper.xml` join 靠租户插件自动拼）
- tea-job：超时取消每分钟/回调重试错峰 30s/日终对账 02:30，`@EnableScheduling`，job 自管 TenantContext 无会话依赖；超时逐单用 TransactionTemplate（避免自调用事务失效）
- C 端桥接：X-Tenant-Id（租户）+ X-Member-Id（会员，**批次 4 登录落地后替换**）；白名单已加 `/api/app/v1/orders|payments/**`
- 模块依赖新增：tea-order +tea-bom；tea-payment +tea-inventory（均无环）
- 暂缓：储值/优惠券/积分（批次 4 钱包）、WebSocket /ws/orders（P1-2，store-terminal 未初始化）、微信 V3 验签

### M3 批次 4 —— ✅ 已提交（2026-09-11）

| 模块 | 接口 | commit | 飞书 |
|---|---|---|---|
| 会员（wx登录/档案/钱包/储值/积分/规则配置） | F-MEM-01~03 | cfc037d | t100021 ✅ |
| 营销（券模板 CRUD/领券/券包/核销） | F-MKT-01 | cfc037d | t100021 ✅ |
| 报表（门店趋势/日结/看板/热销/排行/库存总览） | F-RPT-01/02 | cfc037d | t100021 ✅ |

**落地要点**（细节见 commit diff，82 文件 +3323）：
- **积分**：`MemPointsService`——available=SUM(mem_points_log.change_points)（QueryWrapper 字符串 select 聚合，LambdaQueryWrapper 无此重载）；total_points 用 `setSql` 自增防读改写竞争；bizType 1得/2抵扣/3回补/4过期；幂等键 bizNo(orderNo)+bizType；抵扣上限 `capPoints(payable)=payable/pointValue` 截断（preview 与 appCreate 同规则防试算-下单不一致）
- **钱包**：`MemWalletService.payForOrder` 条件更新 `balance >= amount` 防超扣；充值赠送=amount×rechargeBonusPermille/1000；getOrCreateWallet 捕获 DuplicateKey 兜底
- **储值即时支付（P0-4）**：C端下单 payChannel=2 → 钱包扣款+订单 1→2+payTime+earnOnPaid 全在 appCreate 事务内；**储值单不写 pay_payment** → `ReconcileServiceImpl` 对账 orders 查询加 payChannel=1 过滤（储值消费在钱包 total_consume 落迹）
- **储值退款**：`PayRefundServiceImpl.apply` payment 为空时校验 order.payChannel=2 走钱包回补（refundType=2）；**DDL：pay_refund.payment_id 已放开为 NULL**（容器已 ALTER，01_schema.sql 与库设计文档已同步）
- **券**：claim 校验 status/有效期/totalQty(count 近似)/perLimit；`useCoupon` 先 calcDiscount 再条件更新 status 1→2 防并发重复核销；折扣券金额 `orderAmount×discount` FLOOR；**取消/退款不退券**（PRD）
- **新表 mem_member_config**（积分/储值规则，uk(tenant_id)，容器已建表）；规则读 config、写为 R2/R5（MemberConfigController GET 回显为契约外补充）
- **C端登录**：`POST /api/app/v1/auth/wx-login`（mock：code=openid 直换 memberId）；但 C 端请求**仍用 X-Member-Id 头传身份**（会话化二期）；白名单新增 `/api/app/v1/members/**`、`/api/app/v1/coupons/**`
- **报表**：无中间表实时聚合；营业口径 status IN(2,3,4,5) 按 pay_time；粒度 choose 白名单防 SQL 注入；R6/R7 经 sys_user_store 校验名下门店；库存数量字段 BigDecimal 对齐实体先例（红线只禁金额）
- **角色口径**：会员列表 R2/R5/R6（10.1）与规则配置 R2/R5（10.2）已拆分 requireManageRole/requireConfigRole
- **遗留**：mkt_coupon_scope（指定商品可用）二期；POS 储值收款仍缺会员身份入参（拒绝分支注释保留）；F-RPT-03 导出二期

## 3. 关键实现事实（接手者容易踩的坑）

1. **@MapperScan**：`MybatisPlusConfig` 上 `@MapperScan(basePackages="com.teasaas", annotationClass=Mapper.class)`——新增模块 mapper 接口记得标 `@Mapper`，否则不注册
2. **登录查用户的租户绕过**：`SysUserMapper.selectByUsernameForLogin` 带 `@InterceptorIgnore` + 注释声明——业务查询禁止模仿（规范 6.1）
3. **C 端公开接口租户注入**：X-Tenant-Id 请求头 + `TenantContext` 手动 set（参考 franchise 附近门店实现）；SaTokenConfig 白名单放行 `/api/app/**`；会员身份当前用 X-Member-Id 头传递（wx-login 已出 memberId，**C 端会话化二期**再收敛成 token）
4. **实体 id**：`IdType.ASSIGN_ID`；平台表与关联/流水表不继承 BaseEntity（字段与表严格对齐）；生成器按 DDL 自动路由模块/判断继承
5. **飞书同步**：`E:\DeepSeekHarness\tools\feishu-task.ps1`（create/complete/list，PS5.1 兼容）；本地映射 `tools/feishu-tasks.json`；凭据 `tools/feishu.config.json`（**禁止提交**）
6. **pre-commit hook**：`tea-saas/.git/hooks/pre-commit`（克隆需重装；内容可从 `.harness/skills/code-review-commit/SKILL.md` 恢复）
7. **MySQL/Redis 均已就绪（Docker）**：`tea-mysql`（8.4.11，宿主 **3307**）+ `tea-redis`（宿主 **6379** 无密码）；`tea_saas` **40 表**（批次 4 新增 mem_member_config + pay_refund.payment_id 放开 NULL）+种子已初始化；application.yml 本地默认直连，后端免环境变量启动。3306 是 phpStudy 的 MySQL 5.7（跑不了 02_data.sql 窗口函数，禁止用于本项目）
8. **前端 npm 陷阱**：`npm install --prefix` 会解析到父目录，必须 `cd admin-web` 后执行；Node v24 / npm 11 / npmmirror
9. **代码生成器**：`java -cp tea-generator/target/classes com.teasaas.generator.CodeGenerator --ddl=sql/init/01_schema.sql --out=. --tables=prd_*,inv_*`（先 `mvn -pl tea-generator -am package -DskipTests` 或复用已编译 classes；跳过已存在文件）

## 4. 接手后的建议顺序

1. **admin-web 前端逐模块接接口**（后端契约 F-XXX 已全量落地，Swagger `/swagger-ui.html` 可对照）——登录/加盟/BOM/供应链/商品/库存/订单/会员/营销/报表
2. 前端逐模块替换 `views/error/Placeholder.vue`（`component` 字段如 `product/Materials` → `src/views/product/Materials.vue`，动态路由自动匹配）
3. 二期 backlog（按优先级）：C 端会话化（token 替换 X-Member-Id）、mkt_coupon_scope 指定商品券、POS 储值收款会员身份入参、F-RPT-03 报表导出、WebSocket /ws/orders（store-terminal）、微信 V3 验签
4. 提醒 Owner：t100010（种子访谈）/ t100011（微信支付资质）仍在待办——**t100011 下发后 `tea.pay.mock` 置 false**

## 5. 权威文档索引（冲突时以此为准）

`docs/`（人读权威）> `.harness/`（AI 投影）> 本文件（状态快照，会过时）
