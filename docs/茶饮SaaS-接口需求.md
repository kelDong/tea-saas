# 茶饮 SaaS 系统 · 接口需求文档

> 版本：v1.2 ｜ 关联文档：`茶饮SaaS-PRD.md`（v1.1）、`茶饮SaaS-数据库设计.md`（v1.1）、`茶饮SaaS-工程开发规范.md`（v1.1）｜ 状态：已评审
> 本文档是**前后端契约**：每个接口对应 PRD 功能编号（F-XXX），标注权限角色、入参出参、MVP 优先级。后端 Controller 与前端 API 层以此为准开发。
>
> **v1.2 变更**：补 B 端取消订单接口（8.2，`POST /api/v1/orders/{id}/cancel`，对齐 PRD 5.3 R6/R7 取消按钮）。
> **v1.1 变更**：C 端接口会话化（Review §2）——wx-login 签发会员 token，`/api/app/v1/**` 除公开接口外统一 `Authorization: Bearer` 鉴权，废弃裸 `X-Member-Id` 头传递（兼容期一个迭代，详见 1.1）。

---

## 目录

1. [接口设计总则](#1-接口设计总则)
2. [系统与权限（tea-system）](#2-系统与权限tea-system)
3. [产品研发 BOM（tea-bom）](#3-产品研发-bomtea-bom)
4. [供应链（tea-supply）](#4-供应链tea-supply)
5. [加盟商与门店（tea-franchise）](#5-加盟商与门店tea-franchise)
6. [商品与上架（tea-product）](#6-商品与上架tea-product)
7. [库存（tea-inventory）](#7-库存tea-inventory)
8. [订单（tea-order）](#8-订单tea-order)
9. [支付（tea-payment）](#9-支付tea-payment)
10. [会员（tea-member）](#10-会员tea-member)
11. [营销（tea-marketing）](#11-营销tea-marketing)
12. [报表（tea-report）](#12-报表tea-report)
13. [顾客端小程序（C 端）](#13-顾客端小程序c-端)
14. [接口优先级总览](#14-接口优先级总览)
15. [与 PRD 功能编号对照](#15-与-prd-功能编号对照)

---

## 1. 接口设计总则

### 1.1 URL 与认证

| 前缀 | 使用者 | 认证 |
|---|---|---|
| `/api/admin/v1/**` | 平台管理员 | Sa-Token（R1） |
| `/api/v1/**` | 总部/加盟商/店长/店员 | Sa-Token（R2–R8） |
| `/api/app/v1/**` | 顾客小程序 | 会员会话 token（v1.1 会话化，见下） |
| `/api/notify/**` | 微信支付回调 | 验签，无会话 |

- B 端认证方式：请求头 `Authorization: Bearer <token>`（Sa-Token）
- **C 端会话（v1.1，Review §2）**：
  - 登录：`POST /api/app/v1/auth/wx-login`（入参 `{code}`，公开）返回 `{memberId, token, isNew, nickname, avatar}`，其中 `token` 为会员会话凭证，有效期 7 天（`tea.app.member-token-ttl-days` 可配）
  - 鉴权：C 端请求统一携带 `Authorization: Bearer <token>`；后端拦截器注入会员/租户上下文，Controller 不再读 `X-Member-Id` 头
  - 兼容期（一个迭代，到期硬切）：无 token 但携带 `X-Tenant-Id`/`X-Member-Id` 双头仍放行，后端打 WARN 日志督促前端切换
  - 公开接口（无需会话，仍读 `X-Tenant-Id` 头）：`wx-login`、`GET /api/app/v1/stores/nearby`、`GET /api/app/v1/stores/{id}/products`（含 categories）
  - 未登录（无 token 且无兼容头）：HTTP 401，`code=2` 未登录或会话过期（对齐 B 端口径）
- 数据权限：后端按登录用户角色 + 绑定门店自动过滤（见工程规范第 6 章）
- 版本：一期 v1，破坏性变更升 v2

### 1.2 统一响应（规范红线）

```json
{ "code": 0, "message": "success", "data": { }, "traceId": "xxx" }
```

- `code=0` 成功；非 0 失败（错误码见 1.4）
- 分页 data：`{ "list": [], "total": 0, "page": 1, "size": 10 }`

### 1.3 请求/响应字段约定

- **金额一律"分"（Integer）**，前端展示时格式化
- 枚举传 code（数字），不传中文
- 时间：`yyyy-MM-dd HH:mm:ss` 字符串；日期：`yyyy-MM-dd`
- 分页参数：`page`（从 1 起）、`size`（默认 10，上限 100）
- 通用入参省略：分页 `page/size`、排序 `sort`（`-createTime` 降序）

### 1.4 错误码（模块(2)+场景(2)+序号(2)）

> 注：编码表中前导零仅用于格式表述；**实际传输为整数**（Java 侧去除前导零，如 `000001` → `1`、`080101` → `80101`，避免八进制字面量陷阱）。

| 编码 | 含义 | 编码 | 含义 |
|---|---|---|---|
| 000001 | 参数错误 | 030102 | 订单状态不允许该操作 |
| 000002 | 未登录/会话过期 | 040201 | 支付回调签名校验失败 |
| 000003 | 无权限 | 040202 | 退款金额超限 |
| 010001 | 品牌不存在/已停用 | 060101 | 采购单不存在 |
| 020101 | 原料不存在 | 060102 | 采购单状态不允许该操作 |
| 020201 | 配方不存在 | 070101 | 库存不足 |
| 030101 | 订单不存在 | 070102 | 库存操作并发冲突 |

---

## 2. 系统与权限（tea-system）

### 2.1 登录认证（F-SYS-03）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 登录（账号+验证码） | POST | `/api/v1/auth/login` | 公开 | MVP |
| 获取验证码 | GET | `/api/v1/auth/captcha` | 公开 | MVP |
| 退出登录 | POST | `/api/v1/auth/logout` | 已登录 | MVP |
| 获取当前用户信息 | GET | `/api/v1/auth/me` | 已登录 | MVP |
| 获取菜单树+按钮权限 | GET | `/api/v1/auth/menus` | 已登录 | MVP |
| 微信登录（C 端） | POST | `/api/app/v1/auth/wx-login` | 公开 | MVP |

**登录入参**：`{username, password, captchaKey, captchaCode}` → **出参**：`{token, tokenName, userInfo}`，随后拉取 `/auth/menus` 渲染动态路由。

### 2.2 品牌与套餐（F-SYS-01）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 品牌列表（分页） | GET | `/api/admin/v1/tenants` | R1 | MVP |
| 创建品牌 | POST | `/api/admin/v1/tenants` | R1 | MVP |
| 编辑品牌 | PUT | `/api/admin/v1/tenants/{id}` | R1 | MVP |
| 启停品牌 | PATCH | `/api/admin/v1/tenants/{id}/status` | R1 | MVP |
| 套餐列表 | GET | `/api/admin/v1/packages` | R1 | MVP |
| 套餐 CRUD | POST/PUT/DELETE | `/api/admin/v1/packages` | R1 | 二期细化 |

### 2.3 用户与角色（F-SYS-02）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 用户列表 | GET | `/api/v1/users` | R2/R6(本店)/R7(本店) | MVP |
| 创建用户 | POST | `/api/v1/users` | R2/R6/R7 | MVP |
| 编辑用户 | PUT | `/api/v1/users/{id}` | 同上 | MVP |
| 重置密码 | PATCH | `/api/v1/users/{id}/password` | 同上 | MVP |
| 绑定门店 | PUT | `/api/v1/users/{id}/stores` | R2 | MVP |
| 角色列表 | GET | `/api/v1/roles` | R2 | MVP |
| 角色 CRUD + 菜单授权 | POST/PUT | `/api/v1/roles` | R2 | MVP |

**用户入参**：`{username, nickname, phone, roleIds[], storeIds[], status}`；出参含 `dataScope`（数据范围）。

### 2.4 操作日志（F-SYS-04）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 日志列表 | GET | `/api/v1/operation-logs` | R2(全) / 各角色(本人) | MVP |
| 日志详情 | GET | `/api/v1/operation-logs/{id}` | 同上 | MVP |

---

## 3. 产品研发 BOM（tea-bom）

### 3.1 原料档案（F-BOM-01）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 原料列表 | GET | `/api/v1/materials` | R2/R3/R4 | MVP |
| 原料详情 | GET | `/api/v1/materials/{id}` | 同上 | MVP |
| 新增原料 | POST | `/api/v1/materials` | R2/R3 | MVP |
| 编辑原料 | PUT | `/api/v1/materials/{id}` | R2/R3 | MVP |
| 停用/启用 | PATCH | `/api/v1/materials/{id}/status` | R2/R3 | MVP |

**入参**：`{materialCode, materialName, category, unit, warnThreshold, costPrice(分)}`；编码唯一。

### 3.2 配方 BOM（F-BOM-02）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 配方列表 | GET | `/api/v1/formulas` | R2/R3 | MVP |
| 配方详情（含明细） | GET | `/api/v1/formulas/{id}` | R2/R3 | MVP |
| 新增配方 | POST | `/api/v1/formulas` | R2/R3 | MVP |
| 编辑配方（+1 版本） | PUT | `/api/v1/formulas/{id}` | R2/R3 | MVP |
| 复制配方 | POST | `/api/v1/formulas/{id}/copy` | R2/R3 | MVP |
| 停用/启用 | PATCH | `/api/v1/formulas/{id}/status` | R2/R3 | MVP |
| 发布（进入产品库候选） | POST | `/api/v1/formulas/{id}/publish` | R2/R3 | MVP |

**入参**：`{formulaCode, formulaName, items:[{materialId, qty, sort}]}`；编辑后 `version+1`，改配方留版本记录。

### 3.3 毛利试算（F-BOM-03）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 试算毛利 | POST | `/api/v1/formulas/calc-profit` | R2/R3 | MVP |

**入参**：`{formulaId, suggestPrice(分)}` → **出参**：`{costTotal(分), suggestPrice, grossProfit(分), grossMargin(%)}`。

---

## 4. 供应链（tea-supply）

### 4.1 供应商（F-SUP-01）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 供应商列表 | GET | `/api/v1/suppliers` | R2/R4 | MVP |
| 新增/编辑 | POST/PUT | `/api/v1/suppliers` | R2/R4 | MVP |
| 关联可供应原料 | PUT | `/api/v1/suppliers/{id}/materials` | R2/R4 | MVP |
| 停用 | PATCH | `/api/v1/suppliers/{id}/status` | R2/R4 | MVP |

### 4.2 分级价格表（F-SUP-02）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 价格表查询 | GET | `/api/v1/tier-prices?materialId=&level=` | R2/R4/R6(读) | MVP |
| 批量设置价格 | PUT | `/api/v1/tier-prices/batch` | R2/R4 | MVP |

**入参**：`{items:[{materialId, levelCode(A/B/C), price(分)}]}`；唯一约束 `materialId+levelCode`。

### 4.3 采购单（F-SUP-03/04）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 采购单列表 | GET | `/api/v1/purchase-orders` | R4(全部) / R6(名下门店) | MVP |
| 采购单详情 | GET | `/api/v1/purchase-orders/{id}` | 同上 | MVP |
| 发起采购单 | POST | `/api/v1/purchase-orders` | **R6 仅**（P1-1） | MVP |
| 确认计价 | POST | `/api/v1/purchase-orders/{id}/confirm` | R4 | MVP |
| 发货 | POST | `/api/v1/purchase-orders/{id}/ship` | R4 | MVP |
| 确认入库 | POST | `/api/v1/purchase-orders/{id}/receive` | R6/R7(名下门店) | MVP |
| 取消采购单 | POST | `/api/v1/purchase-orders/{id}/cancel` | R4 / R6(未确认前) | MVP |

**发起入参**：`{storeId, items:[{materialId, qty}]}`；服务端按加盟商等级自动取 `tier_price` 计价（快照写入明细）。**状态机**：待确认→已确认→已发货→已入库；取消。**入库联动（F-SUP-04）**：`receive` 时更新 `inv_store_material_stock + qty` 并写流水（bizType=4 采购入库）。

---

## 5. 加盟商与门店（tea-franchise）

### 5.1 加盟商（F-FRA-01）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 加盟商列表 | GET | `/api/v1/franchisees` | R2/R5 | MVP |
| 新增加盟商 | POST | `/api/v1/franchisees` | R2/R5 | MVP |
| 编辑加盟商 | PUT | `/api/v1/franchisees/{id}` | R2/R5 | MVP |
| 等级评定 | PUT | `/api/v1/franchisees/{id}/level` | R2/R5 | MVP |
| 开通账号 | POST | `/api/v1/franchisees/{id}/account` | R2/R5 | MVP |
| 启停合作 | PATCH | `/api/v1/franchisees/{id}/status` | R2/R5 | MVP |

**等级评定入参**：`{levelCode(A/B/C), remark}`；等级决定分级价格取价（F-SUP-02）。

### 5.2 门店（F-FRA-02）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 门店列表 | GET | `/api/v1/stores` | R2/R5(全) / R6(名下) | MVP |
| 门店详情 | GET | `/api/v1/stores/{id}` | 同上 | MVP |
| 新增门店 | POST | `/api/v1/stores` | R2/R5 | MVP |
| 编辑门店 | PUT | `/api/v1/stores/{id}` | R2/R5 | MVP |
| 营业状态切换 | PATCH | `/api/v1/stores/{id}/business-status` | R2/R7(本店) | MVP |
| 附近门店（C 端） | GET | `/api/app/v1/stores/nearby?lng=&lat=` | 公开 | MVP |

**门店入参**：`{storeCode, storeName, franchiseeId(可空=直营), address, longitude, latitude}`；营业状态影响顾客端可下单。

---

## 6. 商品与上架（tea-product）

### 6.1 产品库与商品（F-PRD-01）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 商品列表 | GET | `/api/v1/products` | R2/R3 | MVP |
| 商品详情（含配方/规格） | GET | `/api/v1/products/{id}` | R2/R3 | MVP |
| 发布商品 | POST | `/api/v1/products` | R2/R3 | MVP |
| 编辑商品 | PUT | `/api/v1/products/{id}` | R2/R3 | MVP |
| 调整建议售价 | PATCH | `/api/v1/products/{id}/price` | R2/R3 | MVP |
| 下架 | PATCH | `/api/v1/products/{id}/off` | R2/R3 | MVP |
| 分类管理 | GET/POST/PUT | `/api/v1/categories` | R2/R3 | MVP |

**发布入参**：`{categoryId, productCode, productName, formulaId, suggestPrice(分), specs:[{specName, isRequired, options:[{optionName, extraPrice}]}]}`。

### 6.2 加盟商上架（F-PRD-02）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 可上架商品（产品库） | GET | `/api/v1/listings/available-products` | R6 | MVP |
| 本店上架列表 | GET | `/api/v1/listings?storeId=` | R6/R7 | MVP |
| 上架商品 | POST | `/api/v1/listings` | R6 | MVP |
| 调整本店售价 | PATCH | `/api/v1/listings/{id}/price` | R6 | MVP |
| 下架 | PATCH | `/api/v1/listings/{id}/off` | R6 | MVP |

**上架入参**：`{storeId, productId, salePrice(分, 上限受总部配置约束)}`；配方自动关联（`prd_listing → prd_product → formula`）。

### 6.3 可售状态联动（F-PRD-03）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 门店可售商品列表 | GET | `/api/app/v1/stores/{id}/products?category=` | 公开 | MVP |
| 商品可售校验 | POST | `/api/v1/listings/check-sellable` | R6/R7 | MVP |

**可售判定（F-PRD-03 / F-INV-02 联动）**：该门店所有配方原料 `stock_qty >= 用量`（服务端实时计算，见数据库设计 13.2）；顾客端仅返回可售商品。**下单扣减（F-INV-02）**：订单提交时按订单明细的 `formula_id` 快照反算原料用量，Redis 预扣 + DB 条件更新防超卖，扣减写流水（bizType=1）。

---

## 7. 库存（tea-inventory）

### 7.1 库存台账（F-INV-01）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 门店库存列表 | GET | `/api/v1/inventories?storeId=&materialId=` | R6/R7(门店) / R2(读) | MVP |
| 库存变动流水 | GET | `/api/v1/inventories/{storeId}/{materialId}/logs` | R6/R7 | MVP |
| 盘点修正 | POST | `/api/v1/inventories/adjust` | **R6/R7 仅** | MVP |

**盘点入参**：`{storeId, materialId, actualQty, remark}`；差异写流水（bizType=5 盘点修正），留痕。

### 7.2 库存预警（F-INV-03）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 低库存列表 | GET | `/api/v1/inventories/warnings?storeId=` | R6/R7 | MVP |
| 总部库存总览 | GET | `/api/v1/inventories/summary` | R2/R5(读) | MVP |

---

## 8. 订单（tea-order）

### 8.1 顾客下单（F-ORD-01）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 下单（C 端） | POST | `/api/app/v1/orders` | C 端会员 | MVP |
| 订单预览（试算） | POST | `/api/app/v1/orders/preview` | C 端会员 | MVP |
| 购物车快照 | POST | `/api/app/v1/carts/snapshot` | C 端会员 | 二期 |

**下单入参**：`{storeId, items:[{listingId, qty, specOptionIds[]}], couponId?, pointsUsed?, payChannel(1微信/2储值)}` → **出参**：`{orderNo, takeCode, payAmount(分)}`。
**关键规则**：服务端二次校验库存（F-ORD-01 AC）；**15 分钟未支付自动取消并回补库存**（P1-5）；下单即扣库存（Redis 预扣 + DB 条件更新）。

### 8.2 订单状态机（F-ORD-02）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 订单列表 | GET | `/api/v1/orders?storeId=&status=` | R6/R7(门店)/R2(全) | MVP |
| 订单详情 | GET | `/api/v1/orders/{id}` | 同上 | MVP |
| 顾客取消（待支付） | POST | `/api/app/v1/orders/{id}/cancel` | C 端 | MVP |
| 门店取消（待支付，v1.2） | POST | `/api/v1/orders/{id}/cancel` | R2(全)/R6/R7(名下门店) | MVP |
| 超时取消（定时） | — | tea-job 定时任务 | 系统 | MVP |
| 订单列表（C 端） | GET | `/api/app/v1/orders` | C 端 | MVP |

**状态机**：`待支付 → 已支付 → 制作中 → 待取餐 → 已完成`；`已取消/已退款` 为终态（不允许从已完成流转）。

### 8.3 门店接单（F-ORD-03）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 接单 | POST | `/api/v1/orders/{id}/accept` | R6/R7/R8 | MVP |
| 标记制作完成 | POST | `/api/v1/orders/{id}/make` | R6/R7/R8 | MVP |
| 核销取餐 | POST | `/api/v1/orders/{id}/finish` | R6/R7/R8 | MVP |
| 新订单推送 | WS | `/ws/orders`（WebSocket） | R7/R8 | MVP |

**WebSocket**（P1-2 决策）：门店端实时接收新订单/状态变更；连接鉴权用 Sa-Token token。

### 8.4 门店手工收银（F-ORD-04，P0-1 决策）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 手工下单 | POST | `/api/v1/pos/orders` | R7/R8 | MVP |
| 收款确认 | POST | `/api/v1/pos/orders/{id}/pay` | R7/R8 | MVP |

**手工下单入参**：`{storeId, items:[{listingId, qty, specOptionIds[]}]}` → 生成 `order_source=2` 订单，顾客无需登录。
**收款入参**：`{payChannel(1现金/2微信收款码Native/3储值), receiveAmount(分,现金找零用)}`；三选一不支持混合（P0-4）；现金录入实收找零；计入门店日结。

---

## 9. 支付（tea-payment）

### 9.1 微信支付（F-PAY-01）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 统一下单（JSAPI） | POST | `/api/app/v1/payments/wx-jsapi` | C 端会员 | MVP |
| 统一下单（Native 收款码） | POST | `/api/v1/payments/wx-native` | R7/R8 | MVP |
| 支付回调 | POST | `/api/notify/wx-pay/callback` | 验签公开 | MVP |
| 查询支付状态 | GET | `/api/v1/payments/{paymentNo}` | 相关角色 | MVP |

**回调处理**（P1-3）：验签 → 幂等（同 transactionId 只处理一次）→ 落 `pay_callback_task` → 定时重试（失败指数退避，不引 MQ）。

### 9.2 退款（F-PAY-02，P0-5 决策）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 申请退款 | POST | `/api/v1/orders/{id}/refund` | R2/R6/R7 | MVP |
| 退款列表 | GET | `/api/v1/refunds` | R2(全)/R6/R7(本店) | MVP |

**退款规则**：仅**整单退**；储值部分→退储值账户，微信部分→原路退回；**已核销券不退还**；退款成功回补库存（`inv_stock_log bizType=3`）。

### 9.3 对账（F-PAY-03）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 日终对账（触发） | POST | `/api/v1/reconcile/run` | R2 | MVP |
| 对账结果 | GET | `/api/v1/reconcile?date=` | R2 | MVP |
| 每日对账（定时） | — | tea-job 定时任务 | 系统 | MVP |

---

## 10. 会员（tea-member）

### 10.1 会员档案（F-MEM-01）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 微信登录建档 | POST | `/api/app/v1/auth/wx-login` | 公开 | MVP |
| 会员信息 | GET | `/api/app/v1/members/me` | C 端会员 | MVP |
| 绑定手机号 | PUT | `/api/app/v1/members/me/phone` | C 端会员 | MVP |
| 会员列表（后台） | GET | `/api/v1/members` | R2/R5(全)/R6(名下) | MVP |

### 10.2 积分（F-MEM-02）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 积分明细 | GET | `/api/app/v1/members/me/points` | C 端会员 | MVP |
| 积分规则配置 | PUT | `/api/v1/member-configs/points` | R2/R5 | MVP |

### 10.3 储值（F-MEM-03，P0-4 决策）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 充值下单 | POST | `/api/app/v1/members/me/wallet/recharge` | C 端会员 | MVP |
| 储值账户 | GET | `/api/app/v1/members/me/wallet` | C 端会员 | MVP |
| 储值流水 | GET | `/api/app/v1/members/me/wallet/logs` | C 端会员 | MVP |

**储值支付**：独立通道（`pay_channel=2`），**不支持与微信混合**；余额不足整单时不可用储值（P0-4）。

---

## 11. 营销（tea-marketing）

### 11.1 优惠券（F-MKT-01）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 券模板列表 | GET | `/api/v1/coupon-templates` | R2/R5/R6(本店) | MVP |
| 创建券模板 | POST | `/api/v1/coupon-templates` | R2/R5 | MVP |
| 编辑/停用 | PUT/PATCH | `/api/v1/coupon-templates/{id}` | R2/R5 | MVP |
| 领券（C 端） | POST | `/api/app/v1/coupons/{templateId}/claim` | C 端会员 | MVP |
| 我的券包 | GET | `/api/app/v1/coupons` | C 端会员 | MVP |
| 券核销联动 | — | 订单结算时自动 | 系统 | MVP |

**券模板入参**：`{name, type(1满减/2折扣), threshold(分), amount(分)/discount, totalQty, perLimit, validType, validStart, validEnd/validDays}`；核销联动订单金额（F-MKT-01 AC）。

---

## 12. 报表（tea-report）

### 12.1 门店报表（F-RPT-01）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 门店日报/周报 | GET | `/api/v1/reports/store?storeId=&granularity=day|week|month` | R6/R7(本店)/R2 | MVP |
| 门店日结数据 | GET | `/api/v1/reports/store/{storeId}/daily?date=` | R7(本店)/R2 | MVP |

### 12.2 品牌看板（F-RPT-02）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 汇总看板 | GET | `/api/v1/reports/dashboard` | R2/R5 | MVP |
| 热销商品 TOP | GET | `/api/v1/reports/hot-products?days=` | R2/R5 | MVP |
| 门店排行 | GET | `/api/v1/reports/store-ranking?date=` | R2/R5 | MVP |
| 库存总览 | GET | `/api/v1/reports/inventory-summary` | R2/R5(读) | MVP |

### 12.3 原料消耗（F-RPT-03）

| 接口 | 方法 | URL | 角色 | 优先级 |
|---|---|---|---|---|
| 原料消耗趋势 | GET | `/api/v1/reports/material-consumption` | R4 | **二期** |

---

## 13. 顾客端小程序（C 端）

| 接口 | 方法 | URL | 功能 | 优先级 |
|---|---|---|---|---|
| 附近门店 | GET | `/api/app/v1/stores/nearby?lng=&lat=` | F-APP-01 | MVP |
| 门店可售商品 | GET | `/api/app/v1/stores/{id}/products?category=` | F-APP-02 | MVP |
| 商品分类 | GET | `/api/app/v1/stores/{id}/categories` | F-APP-02 | MVP |
| 下单预览 | POST | `/api/app/v1/orders/preview` | F-APP-02 | MVP |
| 下单 | POST | `/api/app/v1/orders` | F-APP-02 | MVP |
| 微信登录 | POST | `/api/app/v1/auth/wx-login` | F-MEM-01 | MVP |
| 历史订单 | GET | `/api/app/v1/orders?status=` | F-APP-03 | MVP |
| 订单详情 | GET | `/api/app/v1/orders/{id}` | F-APP-03 | MVP |
| 再来一单 | POST | `/api/app/v1/orders/{id}/reorder` | F-APP-03 | MVP |

**再来一单**：按历史订单明细重新生成购物车（商品已下架/缺货项提示跳过）。

---

## 14. 接口优先级总览

### 14.1 MVP 必做（约 60 个接口）

- 系统：登录/验证码/菜单/用户/角色/品牌/套餐/日志（14）
- BOM：原料/配方/毛利（9）
- 供应链：供应商/分级价格/采购单（11）
- 加盟：加盟商/门店（8）
- 商品：产品库/上架/可售（10）
- 库存：台账/盘点/预警（4）
- 订单：下单/状态机/接单/收银（12）
- 支付：微信/储值/退款/对账（9）
- 会员：登录/档案/积分/储值（8）
- 营销：券模板/领券（6）
- 报表：日报/看板/排行（6）
- 小程序端：与上述 C 端接口复用（约 9 个）

### 14.2 二期（暂缓）

- 原料消耗分析（F-RPT-03）、购物车快照、套餐 CRUD 细化、会员等级自动化、拼团/秒杀、供应商自助平台

---

## 15. 与 PRD 功能编号对照

| PRD 功能 | 接口章节 |
|---|---|
| F-SYS-01~04 | 2.1–2.4 |
| F-BOM-01~03 | 3.1–3.3 |
| F-SUP-01~04 | 4.1–4.3 |
| F-FRA-01~02 | 5.1–5.2 |
| F-PRD-01~03 | 6.1–6.3 |
| F-INV-01~03 | 7.1–7.2 |
| F-ORD-01~04 | 8.1–8.4 |
| F-PAY-01~03 | 9.1–9.3 |
| F-MEM-01~03 | 10.1–10.3 |
| F-MKT-01 | 11.1 |
| F-RPT-01~03 | 12.1–12.3 |
| F-APP-01~03 | 13 |

---

*本文档随实现推进更新；接口变更需同步修订本节并记录版本。*
