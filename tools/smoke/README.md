# 端到端冒烟套件（API 级 S1~S8）

> 权威口径见 `docs/茶饮SaaS-测试计划.md`（冲突以该文档为准）。UI 级冒烟（S9）在 `admin-web/e2e/`。

## 运行

```bash
# 前置：Docker tea-mysql/tea-redis 在跑；后端 8080 已启动
node tools/smoke/run-all.mjs                 # S1~S8 全量
node tools/smoke/run-all.mjs --only=s1,s7    # 指定场景
```

环境变量：`SMOKE_BASE`（默认 `http://localhost:8080`）、`SMOKE_TENANT`（默认 9001）、`SMOKE_PASSWORD`（默认 Smoke2026x）。

## 前置

| 项 | 说明 |
|---|---|
| 后端 | `cd backend-api && java -jar tea-server/target/tea-server-1.0.0-SNAPSHOT.jar` |
| 支付 | 必须为 `tea.pay.mock=true`（默认）；真实网关模式下 S5/S6 支付段不适用 |
| Node | ≥22（全局 fetch/WebSocket，零 npm 依赖） |
| 演示数据 | 租户 9001 需有 `status=1 && sellable=1` 上架商品；账号 brand01/store01/fra01（密码 Smoke2026x） |
| smoke-r8 | S8 首次运行自动创建（R8 店员，密码同上），跑完自动停用，重跑自动复用 |

## 场景索引

| 脚本 | 场景 |
|---|---|
| s1-b-pos-cash.mjs | POS 现金：下单→收款找零→接单→制作→核销→库存扣减 |
| s2-b-refund.mjs | 现金单/微信单退款闭环 + 退款列表 |
| s3-b-cancel.mjs | 待支付取消 + 库存精确回补归零 |
| s4-b-settle-reconcile.mjs | 自建微信单 → 对账差值（diff 不增）+ 门店日结差值 |
| s5-c-order-flow.mjs | 新会员 wx-login→公开接口→preview→下单→wx-jsapi→mock 回调→reorder→取消 |
| s6-c-assets.mjs | 充值/攒积分/积分抵扣/自建券模板领券核销/组合支付（跑完停用模板） |
| s7-ws.mjs | /ws/orders 握手心跳/NEW_ORDER/STATUS_CHANGED(3/4/7)/坏 token/R6 拒绝 |
| s8-authz-negative.mjs | R8 退款 403、无/伪 token 401、越权他人订单、不存在订单 30101 |

## 断言原则（务必遵守，改脚本前先读测试计划 §5）

- **动态查找 + 前后差值**：门店/商品/单价运行时查询；余额/积分/库存/对账计数一律断言差值，禁止硬编码演示库状态值（M15 旧脚本教训）
- **自建可回收**：券模板「冒烟临时券-勿用手动停用」与账号 smoke-r8 由脚本自建并在收尾停用
- **SKIP≠FAIL**：环境性缺数据允许 SKIP 并输出原因

## 演示库副作用（已声明，见测试计划 §5）

新增订单/退款/充值/积分流水、随机 openid 会员（`smoke-*`）、停用态券模板、停用账号 smoke-r8。不修改任何存量业务数据。
