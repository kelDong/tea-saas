# 茶饮 SaaS 系统 · 测试计划（端到端冒烟）

> 版本：v1.0 ｜ 关联文档：`茶饮SaaS-接口需求.md`（v1.4）、`茶饮SaaS-PRD.md`（v1.1）、`茶饮SaaS-工程开发规范.md`（v1.1）｜ 状态：已评审
> 本文档是**端到端冒烟测试的权威事实源**：定义测试场景清单（S1~S9）、每场景的前置/步骤/断言口径/负例与清理策略。脚本实现（`tools/smoke/`、`admin-web/e2e/`）以本文档为准；两者冲突时以本文档为准。
>
> 范围声明：本文档只管**端到端冒烟**。单元/集成测试资产（tea-server `src/test` 资金链路并发 19 例、tea-payment `NotifyControllerTest` 5 例）与 pre-commit 红线门禁不在本文档重复定义，见 §6 索引。

---

## 目录

1. [目的与范围](#1-目的与范围)
2. [环境与前置](#2-环境与前置)
3. [场景总表](#3-场景总表)
4. [场景详述](#4-场景详述)
5. [验收口径](#5-验收口径)
6. [既有自动化资产索引](#6-既有自动化资产索引)
7. [维护规则](#7-维护规则)

---

## 1. 目的与范围

- **目的**：以可重复执行的脚本覆盖四条业务主干（B 端订单、C 端下单支付、C 端资产、WS 实时）+ 安全负例，替代散落在 Temp 目录的一次性冒烟脚本（M15 Python 系列 / M17 ws-smoke.mjs），作为每次发版/回归的统一入口。
- **冒烟定位**：验证"链路通断 + 关键状态/金额正确"，不追求分支覆盖（分支由单元/集成测试承担）。
- **两种形态**：
  - **API 级**（`tools/smoke/`，Node ≥22 原生 fetch/WebSocket，零 npm 依赖）：S1~S8，直打后端 `http://localhost:8080`；
  - **UI 级**（`admin-web/e2e/`，Playwright + Chromium）：S9，走 admin-web dev server（5173）的页面真实交互。

## 2. 环境与前置

| 项 | 要求 |
|---|---|
| Docker | `tea-mysql`（宿主 3307）+ `tea-redis`（6379）运行中；MinIO 可选（S1~S9 不依赖图片上传） |
| 后端 | `cd backend-api && java -jar tea-server/target/tea-server-1.0.0-SNAPSHOT.jar`（8080；修改过 Java 代码先 `mvn -q -DskipTests install`；旧进程占 jar 先 taskkill） |
| 支付开关 | `tea.pay.mock=true`（默认；**真实网关模式下 mock 回调场景不适用**，切真实支付后本套件 S5/S6 支付段需另行适配） |
| Node | ≥22（全局 fetch + WebSocket） |
| 演示数据假设 | 租户 9001 至少一家门店有 `status=1 && sellable=1` 的上架商品（如"回测门店"珍珠奶茶）；无则 S1~S3/S7 FAIL 并提示环境不满足 |
| UI 冒烟附加 | `cd admin-web && npm i -D @playwright/test`；`PLAYWRIGHT_DOWNLOAD_HOST=https://cdn.npmmirror.com/binaries/playwright npx playwright install chromium` |

**演示库账号**（本地演示凭据，可入库）：

| 账号 | 角色 | 密码 | 用途 |
|---|---|---|---|
| brand01 | R2 品牌管理员 | Smoke2026x | 对账/建券模板/建 R8 账号 |
| store01 | R7 店长 | Smoke2026x | POS/接单/退款/日结 |
| fra01 | R6 加盟商 | Smoke2026x | WS 非门店角色负例 |
| smoke-r8 | R8 店员 | Smoke2026x | S8 自建（首次运行自动创建，跑完停用） |

C 端会员不依赖存量：wx-login mock 模式 `code` 即 openid，脚本用随机 openid 每次新建会员。

## 3. 场景总表

| 编号 | 场景 | 覆盖功能 | 形态 | 脚本入口 |
|---|---|---|---|---|
| S1 | B 端 POS 现金全流程（下单→收款→接单→制作→核销） | F-ORD-02/03/04 | API | `tools/smoke/s1-b-pos-cash.mjs` |
| S2 | B 端退款闭环（现金单 + 微信单） | F-PAY-02 | API | `tools/smoke/s2-b-refund.mjs` |
| S3 | B 端待支付取消 + 库存精确回补 | F-ORD-02 | API | `tools/smoke/s3-b-cancel.mjs` |
| S4 | 对账与日结回流 | F-PAY-03 F-RPT-02 | API | `tools/smoke/s4-b-settle-reconcile.mjs` |
| S5 | C 端下单支付全链路（新会员） | F-APP-01~03 F-ORD-01 F-PAY-01 | API | `tools/smoke/s5-c-order-flow.mjs` |
| S6 | C 端资产组合（充值/积分/券/储值组合支付） | F-MEM-01~03 F-MKT-01 | API | `tools/smoke/s6-c-assets.mjs` |
| S7 | /ws/orders 实时推送（含负例） | F-ORD-03（§8.3） | API | `tools/smoke/s7-ws.mjs` |
| S8 | 权限与租户负例矩阵 | 多模块 | API | `tools/smoke/s8-authz-negative.mjs` |
| S9 | admin-web UI 资金主干流（登录→收银→接单流转） | F-ORD-02/04 | UI | `admin-web/e2e/pos-flow.spec.ts` |

统一入口：`node tools/smoke/run-all.mjs`（S1~S8 顺序执行，支持 `--only=s1,s7`）；`cd admin-web && npm run test:e2e`（S9）。

## 4. 场景详述

### 4.1 S1 · B 端 POS 现金全流程

**前置**：store01 登录；门店列表（`GET /api/v1/stores`）取第一家；`GET /api/v1/listings?storeId=` 取 `status=1 && sellable=1` 第一项；库存台账 `GET /api/v1/inventories?storeId=` 全量 SUM 记为 stockBefore。

**步骤与断言**：

1. `POST /api/v1/pos/orders`（items 2 件）→ 订单 status=1，payAmount = preview 口径（单价×2）
2. `POST /api/v1/pos/orders/{id}/pay`（payChannel=1，receiveAmount=payAmount+10000）→ 订单 1→2，`changeAmount=10000`（找零）
3. `accept` 2→3 → `make` 3→4 → `finish` 4→5（核销），每步后 `GET /api/v1/orders/{id}` 断言终态
4. 库存台账再查（stockAfter）：**总扣减 > 0**（SUM 差值为负；精确配方用量不断言，避免配方耦合）

### 4.2 S2 · B 端退款闭环

**前置**：同 S1 店铺/商品。

**步骤与断言**：

1. 现金单：POS 下单 → 现金收款（实收=应收）→ status=2 → `POST /api/v1/orders/{id}/refund`（reason="冒烟退款"）→ 订单 7；`GET /api/v1/refunds`（brand01 查）含该 orderNo
2. 微信单：POS 下单 → `posPay`（payChannel=2 微信Native，mock 即时落账）→ status=2 且流水 MOCKN 前缀 → refund → 订单 7

### 4.3 S3 · B 端取消 + 库存回补

**步骤与断言**：stockBefore → POS 下单（**不支付**）→ `POST /api/v1/orders/{id}/cancel`（body 空）→ status=6、cancelReason 默认"门店取消" → 库存 SUM after == before（**回补精确归零**）。

### 4.4 S4 · 对账与日结回流

**步骤与断言**：

1. brand01 `GET /api/v1/reconcile?date=今天` 记 before（orderCount/paymentCount/diffCount）
2. 本场景自建 1 单微信支付（POS payChannel=2，mock 即时）
3. reconcile after：orderCount 与 paymentCount 差值各 ≥1，**diffCount 差值 = 0**（资金一致性）
4. store01 `GET /api/v1/reports/store/{storeId}/daily?date=今天`：orderCount/salesAmount 差值 ≥1（日结口径 status∈(2,3,4,5) 按 pay_time，退款单不计——本场景不退款故差值即新增）

### 4.5 S5 · C 端下单支付全链路

**前置**：随机 openid `smoke-{timestamp}` wx-login（头 `X-Tenant-Id: 9001`）→ isNew=true 新会员。

**步骤与断言**：

1. `GET /api/app/v1/stores/nearby?lng&lat` 返回含门店；`categories`/`products` **无 token 也 200**（公开接口负例验证白名单）
2. 带 token `POST /orders/preview` → `POST /orders`（payChannel=1 微信）→ status=1
3. `POST /api/app/v1/payments/wx-jsapi` → paymentNo/payParams（mock 六键：appId/timeStamp/nonceStr/package/signType/paySign）
4. `POST /api/notify/wx-pay/callback`（明文 JSON：paymentNo/transactionId/resultCode=SUCCESS/amount=payAmount）→ **响应体为纯文本 `SUCCESS`**（非 Result 包装，M15 教训）
5. 订单 1→2 → `GET /orders/{id}` 详情金额一致
6. `POST /orders/{id}/reorder` → 重建项 qty/现价正确、skipped 空；将重建项 `POST /orders` 下单（不支付）→ `cancel` → status=6（取消负路径）

### 4.6 S6 · C 端资产组合（自建可回收数据）

**步骤与断言**：

1. 新会员钱包 baseline=0 → `POST /me/wallet/recharge`（5000 分，mock 即时到账）→ balance=5000（差值断言）、`/me/wallet/logs` 含此笔
2. 微信单支付（同 S5 流程 1 单）→ `/me` 可用积分差值 > 0（earnOnPaid 攒分）
3. 积分抵扣单：`preview`（pointsUsed=50）→ 下单（pointsUsed=50 + payChannel=2 储值）→ payAmount=小计-50、即时已支付；balance 差值=payAmount；points 差值=攒分-50；`/me/points` 明细含抵扣记录（changePoints<0）
4. **券链路（自建）**：brand01 `POST /api/v1/coupon-templates`（满 2000 减 100、validType=2 validDays=7、totalQty=100、status=1；幂等：重跑先查同名模板复用并置 1）→ C 端 `POST /api/app/v1/coupons/{templateId}/claim` → 券包 status=1
5. 券门槛负例：1 件商品 preview 带 couponId（小计 < 2000）→ code≠0（"未满足券使用门槛"）
6. 券+储值组合下单 2 件：payAmount = 无券 preview 小计 - 100 → 储值支付成功 → 券 status=2（已核销）
7. **清理**：brand01 `PUT /api/v1/coupon-templates/{id}`（全量 DTO + status=2 停用，含 scopeType=1 防 M11 scope 清空坑）

### 4.7 S7 · /ws/orders 实时推送

**步骤与断言**：

1. store01 登录 → `ws://localhost:8080/ws/orders?token=` 握手成功 → 发 `PING` 收 `PONG`
2. POS 现金收款一单 → 收到 `NEW_ORDER`（status=2、orderId/storeId 为字符串——雪花口径）
3. `accept` → `STATUS_CHANGED`(3)；`make` → (4)；`refund` → (7)
4. 负例①：坏 token 握手被拒（onopen 不触发/1006）；负例②：fra01(R6) 握手被拒（仅 R7/R8）

### 4.8 S8 · 权限与租户负例矩阵

**前置**：brand01 建/复能 smoke-r8（`POST /api/v1/users` username=smoke-r8 password=Smoke2026x roleIds=[8] storeIds=[门店]；重跑时先查列表复用并 PUT status=1）。

**步骤与断言**：

| # | 操作 | 预期 |
|---|---|---|
| 1 | smoke-r8 对 store01 已付单 `POST /orders/{id}/refund` | code=3「仅总部/加盟商/店长可操作退款」 |
| 2 | C 端无 token `GET /api/app/v1/orders/{id}` | HTTP 401（code=2） |
| 3 | C 端伪 token 同上 | HTTP 401 |
| 4 | 会员 B（另一随机 openid）查会员 A 订单详情 | code=3「非本人订单」 |
| 5 | store01 查不存在订单 id（超大雪花） | code=30101「订单不存在」（跨租户等价口径：租户插件改写后查无此行；真跨租户需第二租户数据，一期不建） |

**清理**：brand01 `PUT /api/v1/users/{id}`（status=0 停用）。

### 4.9 S9 · admin-web UI 资金主干流（Playwright）

**步骤与断言**：

1. `/login` 填 store01/Smoke2026x → 点「登 录」（全角空格；验证码框若显示留空——后端一期不校验；登录失败 fallback：API 登录后 `localStorage` 注入 `teasaas:token`/`teasaas:token-name` 直连）→ 离开 /login
2. `/store-term/st-pos`：默认门店 → 点第一个可售商品 + → 「下单收款（¥…）」→ 收款弹窗（title 含 orderNo，捕获）选现金、实收=应收+100 → 「确认收款」→ 出现「收款完成」
3. `/store-term/st-orders`：「查询」→ 按 orderNo 定位行 → 「接单」→「制作完成」→「核销取餐」→ 行状态到已完成
4. 断言 WS 连接状态 tag 为已连接态文案（R7 握手成功）

## 5. 验收口径

- **API 套件**：`node tools/smoke/run-all.mjs` 退出码 0；每场景输出逐条 PASS/FAIL 与汇总；任何 FAIL 即整轮失败。
- **SKIP 规则**：环境性缺数据（如无可售商品、建用户接口异常）允许 SKIP 并输出原因，**不计失败**；业务断言失败不允许 SKIP。
- **UI 套件**：`npm run test:e2e` 全部 spec 通过。
- **演示库副作用声明**（跑套件的预期代价，均已声明可接受）：新增订单/退款/充值/积分流水、随机 openid 会员、停用态券模板「冒烟临时券」、停用账号 smoke-r8。**不修改任何存量业务数据**（差值断言原则）。
- **禁止事项**：脚本不得硬编码演示库状态值（余额/订单号/模板 id）；断言一律用运行时查询的前后差值或响应回显。

## 6. 既有自动化资产索引

| 资产 | 位置 | 覆盖 | 运行 |
|---|---|---|---|
| 资金链路并发集成测试 19 例 | `backend-api/tea-server/src/test/java/com/teasaas/it/` | 库存/积分/钱包/券核销/回调的幂等与并发安全 | `mvn -pl tea-server -am test`（需 Docker） |
| 回调控制器单测 5 例 | `backend-api/tea-payment/src/test/.../NotifyControllerTest.java` | 双协议分派/V3 映射/fail-fast | 同上 |
| pre-commit 红线门禁 | `.git/hooks/pre-commit` | @Autowired/System.out/事务/金额类型/SELECT * 静态检查 | git commit 时 |

## 7. 维护规则

1. **新功能上线必须同步补场景行**：涉及资金/状态机/鉴权的接口变更，先改本文档 §3/§4 再改脚本；纯配置类变更可不加。
2. **真实支付切换时**（t100011 资质下发、`tea.pay.mock=false`）：S5/S6 的 mock 回调段与 S2 微信段不适用，须按切换当日的专项检查清单另行验证（盯 pay_callback_task 重试与对账），并更新本文档标注。
3. **脚本与文档冲突以本文档为准**；发现脚本口径更真实（文档写错）时，修文档并升版本号。
4. 差值断言原则（§5）适用于所有新增场景。
