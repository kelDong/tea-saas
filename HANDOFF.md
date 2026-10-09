# HANDOFF.md — 交接状态快照

> 写于：M13（C 端接口补缺：订单详情/再来一单/门店商品分类，接口需求 §13 契约全量对齐）。接手者：Claude Code（或其他 AI 工具）。
> 配套入口：`CLAUDE.md`（必读）。

## 1. Git 位置（tea-saas 仓库）

```
2a6794a feat(order+product+framework): C端接口补缺——订单详情/再来一单/门店商品分类，§13契约全量对齐；appDetail复用buildDetail+requireOwner本人校验，reorder按明细重建购物车(下架/缺货批量校验拆skipped)，categories公开白名单+AppProductVO补categoryName F-APP-02 F-APP-03
247a32f fix(payment+member+job): Review§4次要四项+集成测试暴露两处并发真缺陷——genNo撞号重试上限3/setSql{0}占位/balance_after回读真实值/tea.job.enabled开关；钱包并发开卡FOR UPDATE兜底(RR旧快照误判不存在致静默失败)+积分扣减锁先于一致性读(双单刷负SUM)；新增资金链路并发集成测试19例(库存/积分/钱包/券/回调) F-PAY-01 F-MEM-02 F-MEM-03 F-INV-02
290d87f fix(marketing+admin-web): 券详情出参字段名对齐契约 scope——products≠§11.1 契约名致编辑回显空+启停模板误清适用范围；POS 会员下拉标签去重复¥（fen2yuan 已含货币符） F-MKT-01 F-ORD-04
a500da3 docs(handoff): 进度快照更新至M11——二期补缺批次B三项入档（指定商品券+POS储值收款+原料消耗趋势），backlog划线收口并勘误"F-RPT-03导出"错记
9e9e81a feat(marketing+order+member+payment+report): 二期补缺批次B——指定商品券+POS储值收款+原料消耗趋势 F-MKT-01 F-ORD-04 F-RPT-03
3a6be01 feat(member+marketing+system): 二期补缺批次A——后台会员资产/流水+券模板详情+角色菜单授权回显 F-MEM-01 F-MKT-01 F-SYS-02
81385ef feat(order): B端订单取消/退款闭环——新增POST /orders/{id}/cancel（待支付1→6回补库存，R2/R6/R7）+退款按钮前端接线（后端F-PAY-02已有） F-ORD-02 F-PAY-02
bdf1bdd fix(framework): SaInterceptor显式checkLogin——自定义auth函数会替换默认登录校验，B端接口曾可未登录访问（冒烟发现，修复后401）
af57518 docs(handoff): 进度快照更新至M8——视觉收敛收官(23/23页)+权限种子两轮对齐PRD 5.2/5.3
f773eb0 fix(system): R4种子去掉inv只读越权按钮——PRD 5.2库存台账R4仅👁；运行库补R4采购三钮对齐PRD 5.3 F-SUP-03
541aba5 fix(system): 按钮权限种子对齐PRD 5.3——R6补订单五钮/R7补盘点与门店钮/R8去取消越权 F-ORD-02
d9c881a feat(admin-web): 23页视觉收敛——工具栏/mono/统计卡统一模式类+scoped去重（视觉规范§7清单收官）
cd5b050 feat(system): MinIO图片上传+商品表单图传 F-PRD-01
95c5cb2 fix(supply+system): R2采购链路权限对齐PRD——种子收敛R3∪R4∪R5、服务层R2蕴含R4 F-SUP-03 F-SUP-04
46a1140 feat(admin-web): 工作台升级——待办/门店排行/库存预警面板+退款列表接线 F-RPT-02 F-PAY-02
5daceae feat(admin-web): 采购发起弹窗联动库存低预警——选店带出缺口一键补货 F-SUP-03
7786426 docs(handoff): 进度快照更新至M6——视觉规范主题层落地（token/EP主题/布局壳/三范例页+19页收敛指引）
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

## 2.10 M7 —— ✅ 增量功能批次（2026-09-16，M6 后 4 commits）

| commit | 内容 | 接口 | 飞书 |
|---|---|---|---|
| 5daceae | 采购发起弹窗联动库存低预警：选店带出缺口材料一键补齐数量（Purchases.vue） | F-SUP-03 | — |
| 46a1140 | 工作台升级：待办事项/门店排行/库存预警面板 + 退款列表接线（Workspace.vue + api/types） | F-RPT-02 F-PAY-02 | — |
| 95c5cb2 | R2 采购链路权限对齐 PRD（B 端浏览器验证：R6 发起→R2 确认/发货→R6 收货入库全链路 ✓） | F-SUP-03 F-SUP-04 | 无匹配未同步 |
| cd5b050 | MinIO 图片上传 + 商品表单图传（E2E：上传→保存→DB→缩略图→回显 ✓） | F-PRD-01 | 无匹配未同步 |

**R2 权限修复要点**（95c5cb2）：
- 种子 `02_data.sql`：R2 授权收敛为 R3∪R4∪R5（38 行，页 200-699 + 产品页按钮 3021-3043 + 供应链 4032/4033/4035/4041/4042）；R3 修正为含按钮（12 行）；R6 补 4035（purchase:cancel，PRD 5.3 未确认前可取消）
- 服务层 `isHqSupply() = hasRole(R4) || hasRole(R2)` 贯穿 page/checkStoreAccess/confirm/ship/cancel（R2 品牌管理员蕴含总部供应链职能）
- 运行库已同步种子（R2=38/R3=12 行，零越权泄漏）；死代码 requireRole 已删

**MinIO 图片上传要点**（cd5b050）：
- `POST /api/v1/files/upload`（multipart，SaInterceptor 登录态）：扩展名白名单 jpg/jpeg/png/webp/gif + contentType `image/` 前缀双校验 + ≤5MB（`tea.oss.max-size`）
- 新文件：tea-system `oss/`（OssProperties/MinioConfig/OssService）+ `FileController`；ErrorCode 通用区新增 4/5/6；根 pom 管理 minio 8.5.17
- objectKey=`{tenantId}/{yyyyMM}/{uuid32}.{ext}`（会话租户隔离、无原始文件名防中文坑）；返回永久公开 URL 存 `prd_product.image`（复用既有字段，零 schema 变更）
- 桶 `tea-saas` 公共读（anonymous download，小程序 `<img>` 直连）；启动 `afterPropertiesSet` 幂等建桶+设策略
- **本地环境**：复用本机自有 `minio` 容器（9000 API/9001 控制台，minioadmin/minioadmin，bind mount `C:\Users\lenovo\minio\data`）；部署用 `OSS_ENDPOINT/OSS_ACCESS_KEY/OSS_SECRET_KEY/OSS_BUCKET` env 覆盖
- Products.vue：图片 URL 输入升级为上传卡片（el-upload `:http-request`）+ URL 粘贴兜底，`form.image` 唯一事实源；表格加缩略图列；request.ts 增 `http.upload`（FormData 不手动设 Content-Type，60s 超时）
- multipart 陷阱：Spring 默认 1MB 会先拦掉请求（yml 已放 max-file-size 10MB / max-request-size 12MB）
- **bean 循环依赖陷阱**：`MinioConfig.afterPropertiesSet` 内调自身 `minioClient()` @Bean 方法 → 启动即环；改用一次性本地客户端做桶自愈
- 原料（bom_material）按设计无图片字段，本期不涉及

## 2.11 M8 —— ✅ 视觉收敛收官 + 按钮权限种子两轮对齐 PRD（2026-09-20，M7 后 3 commits）

| commit | 内容 | 飞书 |
|---|---|---|
| d9c881a | 23 页视觉收敛收官：工具栏/统计卡换 `.page-toolbar/.stat-card/.mono` 模式类 + scoped 重复删除（+475/-652，视觉规范 §7 清单全部完成） | 无匹配未同步 |
| 541aba5 | 按钮权限种子对齐 PRD 5.3：R6 补订单五钮 7021~7025、R7 补盘点/流水 4041/4042、R8 去越权退款 7024/取消 7025 F-ORD-02 | 无匹配未同步 |
| f773eb0 | R4 种子去 inv 越权钮（PRD 5.2 库存台账 R4 仅👁，收回 4041/4042）；运行库补 R4 采购三钮（4032/4033/4035）F-SUP-03 | 无匹配未同步 |

**要点**：
- **视觉收敛**（M6 遗留清偿）：admin-web 全部 23 页已套模式类，纯类名替换 + 删 scoped 重复，无功能变更；`npm run build` 通过
- **权限种子三轮收敛后口径**（02_data.sql + 本地运行库均已同步）：
  - R4 供应链：采购处理钮 4032（确认）/4033（发货）/4035（取消），**无** purchase:submit、**无** inv 按钮（库存台账只读）
  - R6 加盟商：订单五钮 7021~7025（accept/make/finish/refund/cancel）+ 原 listing/inv/purchase 钮
  - R7 店长：门店端全部 + 盘点 4041/流水 4042（挂菜单 404 下）；退款店长有、店员无（P2 决策）
  - R8 店员：仅 accept/make/finish/pos（7021/7022/7023/7031），无退款/取消/日结/盘点
- 后续对齐权限时以 **PRD 5.2/5.3 权限矩阵为准**，改种子必须同步刷运行库 `sys_role_menu`（本地 Docker tea_saas）

## 2.12 M9 —— ✅ 登录校验修复 + B 端订单取消/退款闭环（2026-09-24，M8 后 2 commits）

| commit | 内容 | 接口 | 飞书 |
|---|---|---|---|
| bdf1bdd | SaInterceptor 显式 `StpUtil.checkLogin()`——自定义 auth 函数会**替换**默认登录校验，B 端接口曾可未登录访问（冒烟发现，修复后 401） | 框架级 | 无匹配未同步 |
| 81385ef | B 端取消订单 `POST /api/v1/orders/{id}/cancel` + biz/Orders、store/Orders 退款按钮前端接线（退款后端 F-PAY-02 已有） | F-ORD-02 F-PAY-02 | 无匹配未同步 |

**要点**：
- **鉴权陷阱（后续改 SaTokenConfig 必读）**：`SaInterceptor` 传入自定义 auth 函数后**不再自动 checkLogin**——必须显式 `StpUtil.checkLogin()`；修复前未登录请求可直接穿透到 Controller（tenantId 注入逻辑 `if (StpUtil.isLogin())` 只是静默跳过）。C 端 `/api/app/**` 不受影响（AppMemberInterceptor 自行校验）
- 取消端点：body 可选 `reason`（空默认"门店取消"）；条件更新 `status 1→6`（affected=0 → ORDER_STATUS_NOT_ALLOWED，防并发）+ `restoreOrderStock(TIMEOUT_RESTORE)` 回补库存 + `refundPointsQuietly` 积分回补（bizNo=orderNo 幂等）；**券不退还**（PRD 一期口径）
- 取消权限：R2 全租户 / R6/R7 名下门店 / **R8 无**（PRD 5.3，M8 种子已去 R8 越权取消钮 7025，前后端口径闭环）；`@OpLog` 留痕
- 前端：biz/Orders（品牌侧）与 store/Orders（门店侧）退款/取消按钮接线，api/order.ts + types/order.ts 补 cancel/refund
- 接口需求文档升 v1.2：8.2 节补门店取消行（权限 R2 全/R6/R7 名下门店）

## 2.13 M10 —— ✅ 二期补缺批次A（2026-09-29，commit 3a6be01）

| 项 | 后端 | 前端 | 接口 |
|---|---|---|---|
| 后台会员资产/流水 | `GET /api/v1/members/{id}/assets`（余额/累计充值消费/可用积分）+ `/{id}/logs?type=points\|wallet`（积分明细/充值记录，R2/R5/R6 requireManageRole） | Members.vue 资产 drawer（descriptions + 双 tab 流水分页） | F-MEM-01（10.1） |
| 券模板详情 | `GET /api/v1/coupon-templates/{id}`（requireViewRole） | Coupons.vue openEdit 改真详情回填 | F-MKT-01（11.1） |
| 角色菜单授权回显 | 新增 `GET /api/v1/menus/tree`（SysMenuController/Service/Impl 全量树含按钮）+ `GET /api/v1/roles/{id}/menus`（已绑 ID 回显） | Users.vue 角色页授权弹窗（el-tree check-strictly 精确绑定） | F-SYS-02（2.3） |

**要点**：
- **SysRoleServiceImpl 补 R2 守卫**：list/grantMenus 原先无权限校验，现统一 `requireR2()`；sys_role_menu 写 `tenant_id=0`（平台共享表语义，对齐种子，替代原会话租户写入——种子本就 tenant_id=0，写会话租户会导致其他品牌查不到授权）
- **SysMenuServiceImpl 独立组树**：授权树口径（含按钮 menuType=3 叶子、不过滤 status/visible）≠ 登录菜单树（AuthServiceImpl.MenusAssembler：按钮归权限码不进树）——**不复用 MenusAssembler** 避免登录菜单回归；MenuTreeVO.children 已初始化空列表
- **前端 check-strictly（父子不联动）是刻意的**：种子存在"父级授权而子级未授权"（R1 有菜单无按钮），级联勾选往返保存会丢授权，所见即所存；保存提示重新登录生效
- 会员流水钱包 tab = `mem_recharge_record` 充值记录（一期口径，无消费流水表）；积分 availablePoints 复用 `sumAvailablePoints`（SUM 流水）与 C 端 /me 同口径
- 接口需求文档升 v1.3（2.3/10.1/11.1 三节补行）

## 2.14 M11 —— ✅ 二期补缺批次B（2026-09-29，commit 9e9e81a）

| 项 | 后端 | 前端 | 接口 |
|---|---|---|---|
| 指定商品券 | 模板入参/详情 +`scopeType/productIds`（mkt_coupon_scope 表启用，无 DDL）；calcDiscount/useCoupon 以**适用商品小计 base** 为基数；CouponVO +scopeType | Coupons.vue 范围表单（远程搜商品、编辑经详情回显商品名） | F-MKT-01（11.1） |
| POS 储值收款 | 新增 `GET /api/v1/pos/members`（R7/R8，{id,nickname,phone,balance} 最小字段集）；`PosPayDTO` +`memberId`；posPay 储值分支 | Pos.vue 储值渠道解禁 + 会员远程搜索选择（余额展示） | F-ORD-04（8.4） |
| 原料消耗趋势 | `GET /api/v1/reports/material-consumption`（TOP-N 子查询+按日聚合，Service 嵌套化并按合计回排） | Inventory.vue 新「消耗趋势」tab（门店/天数/TOP 工具栏+宽度条+逐日动态列） | F-RPT-03（12.3） |

**要点**：
- **券口径（行为变更）**：`calcDiscount(couponId, memberId, orderAmount, items)` 签名扩参（编译器兜底全链路改造）；无 scope 行=全场（存量兼容，threshold/优惠对象不变）；指定商品券 base=命中商品 amount 之和，base≤0 抛"订单不含券适用商品"，满减 `min(amount, base)`、折扣 `base−floor(base×discount)`；订单侧 preview L242/appCreate 两处传 `toScopeItems(assembled)`
- **对账红线（B2 最高危已守住）**：posPay 储值分支**绝不调 reuseOrCreate/markPaymentSuccess、不写 pay_payment**——镜像 C 端 appCreate 储值分支（payForOrder 条件扣款 → 订单 1→2 条件更新 set payChannel=2+balanceAmount → earnOnPaid bizNo=orderNo 幂等）；防双击双扣靠两道条件更新同在 posPay 事务内（钱包 `balance>=amount` + 订单 `.eq(status,PENDING)`）
- **会员绑定**：posPay 渠道分支前，memberId 非空且订单未绑 → 条件更新 set member_id（`.eq(status,PENDING)` 防并发）；已绑不同会员 → PARAM_ERROR；现金/微信绑定后 markPaymentSuccess 的 earnOnPaid 自动生效
- **消耗趋势权限**：镜像 storeTrend 先例——R4‖R2 → storeId 可空=全部门店；R6 → requireBoundStore 名下；其余 403（admin-web auth store 角色刷新不持久化，靠后端拦而非前端隐藏）
- **净消耗口径**：`biz_type IN (1,2,3)` 的 `SUM(-change_qty)`=下单扣−超时回补−退款回补（采购入库4/盘点5 排除）；XML 不显式写 tenant_id（租户插件对子查询同样改写，对齐 ReportMapper.xml 既有风格）
- **前端防坑**：Coupons `toggleStatus` 改为先拉详情再回传——直接用列表行数据 PUT 会因缺 scopeType 把指定商品券的 scope 清空（saveScope 先全删后插）；Inventory tabs 补 name 属性（原 `name==='warning'` 判断因 pane 无 name 永不命中）
- 接口需求文档升 v1.4（8.4/11.1/12.3 三节）

## 2.15 M12 —— ✅ N1 质量收尾（2026-09-30，commit 247a32f）

**Review §4 次要四项全部落地 + 集成测试暴露两处并发真缺陷（当场修复）**：

| 项 | 落点 |
|---|---|
| genNo 撞号重试 | PayPaymentServiceImpl 两处 try-once 改循环（PAY/CB 前缀，上限 3 次后 BizException） |
| setSql `{0}` 占位 | MemWalletServiceImpl 三处 + MemPointsServiceImpl.accumulateTotal 对齐 InvStock 风格 |
| balance_after 回读 | addBalance 更新后 `selectById` 回读真实值（读改写近似值并发下会漂移） |
| tea.job.enabled 开关 | JobConfig `@ConditionalOnProperty`（测试默认关闭防调度干扰） |
| **真缺陷 A** | getOrCreateWallet DuplicateKey 兜底 `selectOne` 沿用 RR 旧快照看不到并发方刚提交的行→NPE 静默失败；改 `MemWalletMapper.lockByMemberId`（FOR UPDATE 当前读）+ null 守卫 |
| **真缺陷 B** | useForOrder 悲观锁在 `alreadyLogged` 快照读**之后**，锁等待后 sumPoints 仍用旧读视图→并发双单可双双通过刷负 SUM；时序修正为锁先于任何一致性读 |

**资金链路并发集成测试（tea-server/src/test，19 例全绿）**：`IntegrationTestBase`（独立测试库 `tea_saas_test` 每轮从 01_schema.sql 重建；CyclicBarrier 并发工具；死锁感知断言口径"落地量=成功笔数×单笔"）+ 库存/积分/钱包/券/回调 5 类用例。运行：`mvn -pl tea-server -am test`（需本机 Docker tea-mysql/tea-redis）。

**防坑**：
- pre-commit 红线5 门禁**含测试文件**——测试类用 `@TestConstructor(autowireMode=ALL)`（标在基类，@Inherited）+ 构造器注入，禁 @Autowired
- 并发同 uk 插入的 InnoDB 死锁牺牲者=整单回滚可重试，**非数据损坏**——断言按成功笔数口径，勿按线程数
- biz_no/order_no 列 VARCHAR(32)：随机串取 12 位（全量 UUID 会超长截断报 Data too long）

**回归冒烟（2026-09-30 全过）**：R4/R6/R7/R8 按钮种子库内矩阵 + R6/R7 `/auth/menus` 端到端 + R7 接单收银/R6 订单查询 UI 按钮渲染均对齐 PRD 口径；M9 取消（待支付→6+库存 3 笔精确回补）/退款（订单 7+钱包 ±1800 精确回冲）；M10/M11 券编辑回显适用商品、启停双向不清 scope、POS 储值收款（钱包 6900→5100）、会员下拉标签单 ¥、原料消耗趋势页数据正常。

## 2.16 M13 —— ✅ C 端接口补缺（2026-10-09，commit 2a6794a）

**接口需求 §13（F-APP-01~03）契约全量对齐**——补齐此前缺失的三个 C 端接口：

| 接口 | 编号 | 落点 |
|---|---|---|
| `GET /api/app/v1/stores/{id}/categories` 门店商品分类 | F-APP-02 | `appStoreCategories`（公开白名单，X-Tenant-Id 注入同 products 惯例）；`selectAppCategories` 仅返回该店有上架+已发布商品的分类（JOIN 去重，按 c.sort）；`AppProductVO` 顺带补 `categoryName`（LEFT JOIN prd_category，插件改写 join 隔离） |
| `GET /api/app/v1/orders/{id}` 订单详情 | F-APP-03 | `appDetail`：`requireOwner`（仅本人，多租户归属校验同 appCancel 口径）；B 端 detail 抽 `buildDetail` 共用组装 |
| `POST /api/app/v1/orders/{id}/reorder` 再来一单 | F-APP-03 | `appReorder`：按 ord_order_item 明细重建购物车项；上架/发布状态批量校验 + `selectShortages` 缺货批量校验，不可购项拆入 `skipped`（skipReason=已下架/缺货），可购项带当前本店售价 |

**要点**：
- **ReorderVO 设计**：ord_order_item 仅存 specDesc 快照（specOptionIds 不持久化），可加购项由小程序跳规格弹层重选规格后入车——VO javadoc 已注明
- reorder 只读不扣库存（入车才走既有 appCreate 幂等内核）；selectBatchIds/插件改写均走会话租户
- SaTokenConfig AppMemberInterceptor 白名单补 `/api/app/v1/stores/*/categories`
- 飞书无匹配任务未同步；`mvn -q clean compile` 通过（增量编译残留会假报 tea-supply Lombok 符号缺失，clean 即消）

## 2.17 M14 —— ✅ mini-program C 端小程序初始化（2026-10-09，commits 44d6465 + 74293b9）

**PRD §6.2 全 7 页两批次落库**——uni-app CLI（vite 5.2.8 锁定 + TS + pinia 2.3.1）复刻 `prototype/customer-app.html` 暖茶调原型，零组件库：

| 批次 | 内容 | 编号 |
|---|---|---|
| 1（44d6465） | 骨架（request 层 Bearer+X-Tenant-Id 注入、Result 解包、401 单飞重登 wx.login）+ 选店（nearby/营业判定含跨零点/切店清车确认）+ 点单（左分类右商品流/规格弹层加价/同规格合并/CartBar+Drawer）+ 结算（preview 驱动四行金额/券门槛过滤/积分开关/payChannel 本页选定）+ 收银台（wx-jsapi→mock 回调直连→轮询取餐码） | F-APP-01 F-APP-02 F-ORD-01 F-PAY-01 |
| 2（74293b9） | 订单列表（状态 tab 本地过滤+触底翻页，裸 OrdOrder 卡片降级）/ 详情（steps/取餐码/继续支付/取消回补）/ 再来一单（skipped 提示+pendingReorder 队列逐项弹规格重选）/ 会员中心（渐变头+资产三格/券包三态/积分明细/固定档储值 mock 即时到账/绑手机号/模板ID领券调试入口）；tabBar 三 tab（纯文字） | F-APP-03 F-MEM-01~03 F-MKT-01 |

**要点**：
- 分批提交策略：批次1 pages.json 裁至 4 页无 tabBar（store-select 为入口，reLaunch 回首页），批次2 恢复 7 页+tabBar 并切回 switchTab——两 commit 树均可独立构建
- 契约口径已核对落码：PageResult={list,total,page,size}；雪花 ID 序列化为 string（types/api.ts 全 string）；订单列表无 status 入参返裸实体；payChannel 是下单 DTO 字段；RechargeDTO 仅 amount(分)
- 构建：`npm run build:mp-weixin` 全绿（仅 sass legacy-js-api 弃用噪音）；devtools 导入 `dist/build/mp-weixin`，须勾「详情→本地设置→不校验合法域名」
- 遗留：真实支付（config.useMockPay=false）后端 paySign 未实现，真机联调前需补；tabBar 无图标资源（一期纯文字）
- 飞书无匹配任务未同步（23 个已同步任务均 M2~M5 期）

## 3. 关键实现事实（接手者容易踩的坑）

1. **@MapperScan**：`MybatisPlusConfig` 上 `@MapperScan(basePackages="com.teasaas", annotationClass=Mapper.class)`——新增模块 mapper 接口记得标 `@Mapper`，否则不注册
2. **登录查用户的租户绕过**：`SysUserMapper.selectByUsernameForLogin` 带 `@InterceptorIgnore` + 注释声明——业务查询禁止模仿（规范 6.1）
3. **C 端公开接口租户注入**：X-Tenant-Id 请求头 + `TenantContext` 手动 set（参考 franchise 附近门店实现）；SaTokenConfig 白名单放行 `/api/app/**`；会员身份 M5 R3 起已会话化（wx-login 签发 Bearer token，AppMemberInterceptor 解析；双头兼容期一个迭代后硬切）
4. **实体 id**：`IdType.ASSIGN_ID`；平台表与关联/流水表不继承 BaseEntity（字段与表严格对齐）；生成器按 DDL 自动路由模块/判断继承
5. **飞书同步**：`E:\DeepSeekHarness\tools\feishu-task.ps1`（create/complete/list，PS5.1 兼容）；本地映射 `tools/feishu-tasks.json`；凭据 `tools/feishu.config.json`（**禁止提交**）
6. **pre-commit hook**：`tea-saas/.git/hooks/pre-commit`（克隆需重装；内容可从 `.harness/skills/code-review-commit/SKILL.md` 恢复）
7. **MySQL/Redis/MinIO 均已就绪（Docker）**：`tea-mysql`（8.4.11，宿主 **3307**）+ `tea-redis`（宿主 **6379** 无密码）+ MinIO（本机自有 `minio` 容器，宿主 **9000** API / **9001** 控制台，minioadmin/minioadmin，数据 bind mount `C:\Users\lenovo\minio\data`，桶 tea-saas 由后端启动自建）；`tea_saas` **40 表**（批次 4 新增 mem_member_config + pay_refund.payment_id 放开 NULL）+种子已初始化；application.yml 本地默认直连，后端免环境变量启动。3306 是 phpStudy 的 MySQL 5.7（跑不了 02_data.sql 窗口函数，禁止用于本项目）
8. **前端 npm 陷阱**：`npm install --prefix` 会解析到父目录，必须 `cd admin-web` 后执行；Node v24 / npm 11 / npmmirror
9. **代码生成器**：`java -cp tea-generator/target/classes com.teasaas.generator.CodeGenerator --ddl=sql/init/01_schema.sql --out=. --tables=prd_*,inv_*`（先 `mvn -pl tea-generator -am package -DskipTests` 或复用已编译 classes；跳过已存在文件）
10. **SaInterceptor 自定义 auth 函数会替换默认登录校验**（bdf1bdd 教训，曾致 B 端未登录裸奔）：必须显式 `StpUtil.checkLogin()`；C 端 `/api/app/**` 走 AppMemberInterceptor 自行校验（Bearer 为主，双头 X-Tenant-Id/X-Member-Id 兼容期内 WARN 放行）
11. **mini-program 工程陷阱**：`computed/ref/watch` 必须从 `'vue'` 导入（`@dcloudio/uni-app` 只导出 onLoad/onShow 等生命周期，导错 build 直接报 not exported）；组件平铺在 `components/X.vue` **不匹配 easycom 默认规则**（要求 `components/x/x.vue` 目录式），页面必须显式 `import X from '@/components/X.vue'`，否则 **build 不报错但运行时不渲染**（M14 EmptyState 踩坑，用 dist 里 page.json 的 usingComponents 核对）；pinia 安装需 `--legacy-peer-deps`；vite 锁 5 勿升 6（vite-plugin-uni 不兼容 6）；小程序端不认 `<small>` 标签选择器

## 4. 接手后的建议顺序

1. **二期 backlog 后端补缺**（推荐先做，前端页面已标注缺口）：~~后台会员余额/流水查询接口~~、~~券模板 GET /{id}~~、~~角色-菜单授权回显接口~~（M10 批次A 收口）、~~mkt_coupon_scope 指定商品券~~、~~POS 储值收款会员身份入参~~、~~F-RPT-03 原料消耗趋势~~（M11 批次B 收口；注：M3 批次4 遗留项"F-RPT-03 导出"系当时错记——接口需求 12.3 的 F-RPT-03 实为原料消耗趋势，**报表导出无契约定义仍留 backlog**）、报表导出（无契约定义，做前需 Owner 拍板格式与范围）、WebSocket /ws/orders（store-terminal 未初始化）、微信 V3 验签；另 MinIO 生产部署需把 `OSS_ENDPOINT` 指到公网/内网可达地址（**DB 中已存 URL 不会自动迁移**，存量 localhost URL 需刷数据）
2. ~~**Review 方案 §4 次要项**~~（M12 247a32f 收口：genNo 重试循环、setSql `{0}` 占位风格、balance_after 回读、资金链路集成测试 19 例——测试暴露的钱包 RR 快照与积分锁时序两缺陷已当场修复）
3. ~~mini-program 初始化~~（M14 两批次收口，devtools 联调口径见 §2.17；后端 mock 支付/公开接口免鉴权配置均已打通）；store-terminal 仍未初始化（依赖 WebSocket /ws/orders，无契约定义）
4. ~~端到端冒烟（回归用）~~（M12 已回归全过：R4/R6/R7/R8 按钮可见性 + M9~M11 端到端；回归口径见 §2.15）
5. 提醒 Owner：t100010（种子访谈）/ t100011（微信支付资质）仍在待办——**t100011 下发后 `tea.pay.mock` 置 false**；另 warning 提案色 `#C9982F` 待确认（M6）

> 已完成：~~admin-web 视觉收敛~~（M8 d9c881a 收官，23/23 页）、~~端到端冒烟首验~~（M5）、~~管理端订单取消/退款闭环~~（M9 81385ef）、~~二期补缺批次A：后台会员资产/流水+券详情+授权回显~~（M10 3a6be01）、~~二期补缺批次B：指定商品券+POS储值收款+原料消耗趋势~~（M11 9e9e81a）、~~N1 质量收尾：Review§4 四项+并发集成测试19例+全量回归冒烟~~（M12 247a32f）、~~C 端接口补缺：订单详情/再来一单/门店分类，§13 契约全量对齐~~（M13 2a6794a）、~~mini-program C 端小程序初始化：PRD §6.2 全 7 页两批次+mock 支付闭环~~（M14 44d6465/74293b9）

## 5. 权威文档索引（冲突时以此为准）

`docs/`（人读权威）> `.harness/`（AI 投影）> 本文件（状态快照，会过时）
