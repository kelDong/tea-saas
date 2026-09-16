# HANDOFF.md — 交接状态快照

> 写于：M6（视觉规范主题层落地）。接手者：Claude Code（或其他 AI 工具）。
> 配套入口：`CLAUDE.md`（必读）。

## 1. Git 位置（tea-saas 仓库）

```
b881113 feat(admin-web): 视觉规范主题层——原型暖茶调对齐token/EP主题/布局壳/三范例页
5df7911 feat(framework+member): Review批次R3——C端接口会话化：wx-login签发会员token，/api/app统一Bearer鉴权 F-MEM-01
ccbfc40 fix(inventory+member): Review批次R2——库存/积分流水幂等唯一键DB兜底+积分抵扣悲观锁 F-INV-02 F-MEM-02
988558b docs(handoff): 进度快照更新至M5——Review批次R1落地+端到端冒烟全链路闭环（含冒烟环境事实与剩余R2/R3计划）
9aedbcb fix(payment): Review批次R1——支付回调租户上下文修复+重试状态守卫 F-PAY-01
ca10542 docs(review): 全项目Review修复方案v1.0——回调租户上下文/C端会话化/幂等唯一索引等四项待拍板
64ec365 fix(admin-web): 登录昵称持久化localStorage——刷新后顶栏不再显示未命名用户 F-SYS-03
23b4e46 fix(backend): 端到端回测四项修复——Jackson雪花ID防JS精度丢失404/角色表豁免租户插件/登录注入租户上下文/JDBC字符集UTF-8 F-SYS-03 F-FRA-01
527d94c chore(tools): 飞书任务清单同步——批次7平台前端两页t100028完成记录
9e09965 feat(admin-web): F7平台两页接后端——品牌租户CRUD启停/套餐列表 R1专属 F-SYS-01
4a1065b chore(tools): 飞书任务清单同步——批次6门店端前端五页t100027完成记录
7777d37 feat(admin-web): F6门店端五页接后端——门店工作台/接单流转/POS收银下单收款/门店库存台账/日结对账 F-ORD-02~04
9f9f086 chore(tools): 飞书任务清单同步——批次5会员营销前端两页t100026完成记录
9182461 feat(admin-web): F5会员营销两页接后端——会员列表/积分储值规则配置/券模板满减折扣创建停用 F-MEM-01~03 F-MKT-01
cb5b102 chore(tools): 飞书任务清单同步——批次4加盟+报表前端五页t100025完成记录
3767f40 feat(admin-web): F4加盟+报表五页接后端——加盟商调级开账号/门店营业开关/工作台/经营趋势日结/品牌排行热销库存总览 F-FRA-01~02 F-RPT-01~02
0bacdeb chore(tools): 飞书任务清单同步——批次3供应链前端四页t100024完成记录
e05ccbc feat(admin-web): F3供应链四页接后端——供应商供货绑定/等级定价/采购状态机/库存台账盘点预警 F-SUP-01~04 F-INV-01~03
4f3e347 fix(admin-web): PageResult 字段 records→list 对齐后端 PageResult.java（骨架类型笔误，会导致所有分页页空白）
672368e chore(tools): 飞书任务清单同步——批次2产品线四页t100023完成记录
fb17e2a feat(admin-web): F2产品线四页接后端——原料/配方毛利试算/商品规格/门店上架缺料校验 F-BOM-01~03 F-PRD-01~03
b0ebc01 chore(tools): 飞书任务清单同步——批次1系统+订单前端三页t100022完成记录
e2499a7 feat(admin-web): F1前端三页接后端——用户/角色/操作日志/订单查询流转 + api/types/constants 工程层
2427235 feat(system): F-SYS-01补缺——品牌租户CRUD/启停+套餐列表（/api/admin/v1，R1 Service内校验，平台表无租户隔离）
a065809 docs(handoff): 进度快照更新至M3批次4收官（更早 M3 记录见 git log：cfc037d 会员营销报表 / 57e204b 订单支付 / 0b526f5 商品库存 / 754668b MapperScan 修复 / 913a5e8 BOM / 96cdb60 加盟 / 7f3f3b5 供应链）
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

## 2.5 M4 —— ✅ 全部收官（2026-09-15）

**admin-web 前端 22 页全部接通后端**（动态路由 component 自动匹配，无 Placeholder 残留）+ F-SYS-01 后端补缺：

| 批次 | 页面 | 接口 | commit | 飞书 |
|---|---|---|---|---|
| 补缺 | 平台后端 tenants/packages | F-SYS-01 | 2427235 | — |
| F1 | system/Users（用户/角色）、system/OpLogs、biz/Orders | F-SYS-02/03 F-ORD-02 | e2499a7 | t100022 ✅ |
| F2 | product/ 四页（原料/配方/商品/上架） | F-BOM-01~03 F-PRD-01~03 | fb17e2a | t100023 ✅ |
| F3 | supply/ 四页（供应商/等级价/采购/库存） | F-SUP-01~04 F-INV-01~03 | e05ccbc | t100024 ✅ |
| F4 | franchise/ 两页 + biz/ 报表三页 | F-FRA-01/02 F-RPT-01/02 | 3767f40 | t100025 ✅ |
| F5 | biz/Members（列表+积分储值规则）、biz/Coupons | F-MEM-01~03 F-MKT-01 | 9182461 | t100026 ✅ |
| F6 | store/ 五页（工作台/接单/POS收银/库存/日结） | F-ORD-02~04 | 7777d37 | t100027 ✅ |
| F7 | platform/Tenants、platform/Packages | F-SYS-01 | 9e09965 | t100028 ✅ |

**落地要点**：
- **前端工程层**：`api/*.ts`（13 个模块）+ `types/*.ts`（15 个）+ `utils/format.ts`（fen2yuan/formatDateTime）+ http 补 patch；全部 `<script setup lang="ts">`，构建 `vue-tsc && vite build` 通过
- **PageResult 关键修正**（4f3e347）：后端字段是 `list` 不是 `records`——骨架 types/api.ts 笔误会致所有分页页空白，已全量替换
- **金额红线前端版**：展示层一律 fen2yuan(分)；输入层元↔分换算仅在表单提交前 `Math.round(x*100)`（券/POS 实收）；TierPrices/供货绑定直接以“分”为单位输入并标注
- **平台前缀**：/api/admin/v1/**（R1）与 baseURL /api/v1 不同——request.ts 支持 url 以 `/api/` 开头即视为完整路径不拼接 baseURL
- **POS 收银流程**（store/Pos）：菜单取 `GET /listings?storeId`（sellable=0 置灰）→ 购物车 → `POST /pos/orders`（即时扣库存）→ 收款 dialog（渠道 1现金/2微信Native/3储值禁用；现金找零=实收-应收前端预览）→ `POST /pos/orders/{id}/pay`；POS 渠道编号与订单表编号不同已在 types/order.ts 注释
- **权限**：v-permission 仅用于菜单种子里真实存在的 perms（order:*/pos:create/inv:*/purchase:*/bom:*/product:*/listing:*）；加盟/报表/会员/营销/平台模块后端无按钮 perms，前端按菜单可见性控制（与后端一致）
- **门店选择**：store/ 五页与 Listings/Purchases 各自内嵌门店下拉（默认第一店），watch(storeId) 联动刷新
- **订单状态机前端**：2已支付→接单→3制作中→制作完成→4待取餐→核销→5已完成；6取消/7退款终态（constants/order.ts Map 驱动，biz/Orders 与 store/Orders 复用）
- **角色缺口说明**（页内 alert/注释标注）：后台无会员余额/流水接口（C 端 /me 域）、券模板无 GET /{id}（编辑行内数据回填）、订单取消/退款无管理端 endpoint（按钮权限有接口无——不渲染按钮）、角色-菜单授权无回显接口（Users 角色页只读提示）

## 2.8 M5 —— ✅ Review 批次 R1/R2/R3 全部落地 + 端到端冒烟收官（2026-09-16）

**Review 修复方案批次 R1 落地**（`docs/茶饮SaaS-Review修复方案.md` §1 + §3.2，commit 9aedbcb）：

- `PayPaymentMapper.selectByPaymentNoForCallback`：`@InterceptorIgnore` 绕过租户插件按全局唯一单号反查（规范 6.1 声明式例外，对齐 `selectByUsernameForLogin` 惯例）
- `handleCallback`：反查流水 → 显式 `TenantContext.setTenantId(payment.getTenantId())` → 主体逻辑抽到 `processCallback` → try/finally clear（与 TenantCleanupInterceptor 双保险）
- `scheduleRetry`：追加 `.eq(status, TASK_PENDING)` 状态守卫
- **修复前**：纯网关回调（无登录态）被插件改写 `tenant_id=0` 查不到流水必挂；**修复后冒烟验证一次成功**

**端到端冒烟全链路闭环**（brand01/fra01/store01 三角色 + C 端会员）：

| 环节 | 结果 |
|---|---|
| brand01(R2) 开 fra01(R6) 加盟商账号 | userId=2100026300419776514 ✓ |
| store01(R7) 盘点入库存（珍珠/红茶/牛奶） | 10000/100000/50000 ✓ |
| fra01(R6) 上架商品（珍珠奶茶 1600 分） | listing=2100026540141027329 ✓ |
| POS 现金：2 杯应收 3200 实收 4000 | **找零 800** ✓ |
| POS 微信 Native：即时确认落账 | 流水 MOCKN 前缀 ✓ |
| C 端微信单（X-Tenant-Id/X-Member-Id 头） | wx-jsapi 出 payParams ✓ |
| **无登录态回调**（不带任何头 POST /api/notify/wx-pay/callback） | 订单 1→2、流水 1→2、task tenant_id=9001 status=2、积分 +16 ✓ |
| 库存精确扣减 4 杯用量 | 珍珠 9800/红茶 98800/牛奶 49200 ✓ |
| 报表/日结回流 | 3 单 6400 分、微信 3200、热销 4 杯、看板/排行/库存总览全对 ✓ |

**冒烟环境事实**（本地 Docker 演示库）：冒烟账号 brand01/fra01/store01 密码均为 `Smoke2026x`；门店"回测门店"ST-001（id=2099772853422260225）；会员茶友4077（openid=smoke-openid-001）。后端启动：`cd backend-api && mvn -q -DskipTests install && java -jar tea-server/target/tea-server-1.0.0-SNAPSHOT.jar`（多模块下 `mvn -pl tea-server spring-boot:run` 会在根 POM 错位执行，勿用）。

**Review 批次 R2 落地**（方案 §3.1 + §3.3，commit ccbfc40，飞书 t100030）：

- `inv_stock_log` 加四列唯一键 `uk_stock_log_biz(tenant_id,biz_no,biz_type,material_id)`、`mem_points_log` 加 `uk_points_log_biz(tenant_id,biz_no,biz_type)`（方案原稿三列会撞"一单多原料流水"，修正为四列含 material_id；容器已同步 ALTER，01_schema.sql 与数据库设计文档 v1.2 已同步）
- `InvStockServiceImpl.writeLog` / `MemPointsServiceImpl.writeLog`：insert 捕获 `DuplicateKeyException` → 抛 `BizException` 回滚（**不静默收尾**——会破坏 SUM(log)=stock 对账口径）；实测 1062 拦截、NULL biz_no 豁免（期初/盘点流水）
- `MemPointsServiceImpl.useForOrder`：`sumPoints` 前先 `memMemberMapper.lockById(memberId)`（FOR UPDATE 悲观锁）防并发超抵

**Review 批次 R3 落地**（方案 §2 C 端会话化，commit 5df7911，飞书 t100031）：

- `MemberTokenService`（framework/security/context）：token=UUID，Redis `teasaas:member:token:{token} → "tenantId:memberId"`，TTL 7 天（`tea.app.member-token-ttl-days` 可配）；与 Sa-Token 并存不进 StpUtil
- `AppMemberInterceptor` 注册于 `/api/app/**`（排除 wx-login/stores 两公开）：① `Authorization: Bearer` → 解析注入 MemberContext+TenantContext；② 兼容期（一个迭代后硬切）双头 `X-Tenant-Id`/`X-Member-Id` → WARN 放行；③ 皆无 → NotLoginException → HTTP 401 code=2（对齐 B 端）
- wx-login 事务提交后签发 token（WxLoginVO 加 token 字段）；members/orders/payments/coupons 四 Controller 删手动头解析改读 `MemberContext.getMemberId()`（preview 匿名语义取消——小程序进入即静默登录）
- **注意**：javadoc 里写 `stores/*/products` 会被 `*/` 提前关闭注释块导致编译"非法字符"——通配路径在 javadoc 里要转义写法
- 接口需求文档 v1.1（1.1 节 C 端鉴权契约：公开接口/兼容期/401 口径）；已验证：Bearer 查会员/preview 试算 200、无 token 与伪 token 401、双头放行且 WARN 落日志

**Review 方案剩余**：仅 §4 次要项（genNo 重试循环、setSql `{0}` 占位风格、balance_after 回读、资金链路集成测试）——详见方案文档 §5/§6。

## 2.9 M6 —— ✅ 视觉规范主题层落地（2026-09-16，commit b881113）

对齐本地 HTML 原型（`E:\DeepSeekHarness\prototype\admin-panel.html`，暖茶调：奶油底 `#FAF6EE`/琥珀 `#D97B29`/深棕侧栏 `#2E251B`），**规范文档 `docs/茶饮SaaS-视觉规范.md` 为唯一视觉事实源**：

- `styles/` 三层落地：tokens.css（`--ts-*` 品牌 token）→ theme.css（EP 2.9.1 变量覆盖+组件补丁）→ patterns.css（`.page-toolbar/.stat-card/.mono` 等共享模式类，经 index.css 接线）
- AdminLayout 重造（深棕侧栏+琥珀激活轨，**删 el-menu 内联颜色 props**——内联 style 压过一切 CSS）；LoginView 品牌化；范例页 ×3（Workspace/Materials/Coupons）
- 其余 ~19 页经 EP 变量层免费获得约 80% 新观感，**待分批收敛**（每页仅：工具栏类名换 `.page-toolbar`、删 scoped 重复、数值列加 `.mono`、按需 `.page-note`；清单见规范 §7）
- **实测大坑（规范 §3.2 陷阱 4）**：EP 组件级变量（`--el-table-*/--el-card-*/--el-dialog-*/--el-input-*/--el-pagination-*`）定义在**组件选择器**上而非 `:root`，`:root` 覆盖完全无效——必须在同名组件选择器上覆盖（theme.css 已按此写，改样式前必读）
- **待 Owner 拍板**：warning 金黄 `#C9982F` 为提案色（原型未定义），满减券类型标签在用，备选 `#A98E3B` 橄榄金
- 已浏览器实测：登录/工作台/原料/优惠券四页 computed-style 级验证（表头暖底/斑马纹/胶囊标签/弹窗 16px 圆角/主按钮琥珀/侧栏激活轨全部生效）；`npm run build` 通过

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

1. **admin-web 剩余 ~19 页视觉收敛**（M6 后续，量小）：照范例页（Materials=表格页/Coupons=表格+弹窗/Workspace=仪表盘）批量套模式类，清单见视觉规范 §7；改 EP 样式前必读 §3.2 陷阱记录（尤其陷阱 1/4）
2. **端到端冒烟**（推荐先做）：`cd admin-web && npm run dev`（proxy → localhost:8080）+ 后端起 tea-server，用种子账号走一遍：R2 登录 → BOM 建配方 → 商品发布 → 门店上架 → store/Pos 收银下单收款 → 报表/日结查看数字回流
3. 二期 backlog（按优先级）：管理端订单取消/退款 endpoint（按钮权限已种）、后台会员余额/流水查询接口、券模板 GET /{id}、角色-菜单授权回显接口、C 端会话化（token 替换 X-Member-Id）、mkt_coupon_scope 指定商品券、POS 储值收款会员身份入参、F-RPT-03 报表导出、WebSocket /ws/orders（store-terminal 未初始化）、微信 V3 验签
4. mini-program / store-terminal 两端仓库仍未初始化（CLAUDE.md 已注）
5. 提醒 Owner：t100010（种子访谈）/ t100011（微信支付资质）仍在待办——**t100011 下发后 `tea.pay.mock` 置 false**；另 warning 提案色 `#C9982F` 待确认（M6）

## 5. 权威文档索引（冲突时以此为准）

`docs/`（人读权威）> `.harness/`（AI 投影）> 本文件（状态快照，会过时）
