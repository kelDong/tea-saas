# HANDOFF.md — 交接状态快照

> 写于：M3 批次 2 收官（商品/库存已提交，批次 3 订单+支付开工前）。接手者：Claude Code（或其他 AI 工具）。
> 配套入口：`CLAUDE.md`（必读）。

## 1. Git 位置（tea-saas 仓库）

```
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

## 3. 关键实现事实（接手者容易踩的坑）

1. **@MapperScan**：`MybatisPlusConfig` 上 `@MapperScan(basePackages="com.teasaas", annotationClass=Mapper.class)`——新增模块 mapper 接口记得标 `@Mapper`，否则不注册
2. **登录查用户的租户绕过**：`SysUserMapper.selectByUsernameForLogin` 带 `@InterceptorIgnore` + 注释声明——业务查询禁止模仿（规范 6.1）
3. **C 端公开接口租户注入**：X-Tenant-Id 请求头 + `TenantContext` 手动 set（参考 franchise 附近门店实现）；SaTokenConfig 白名单放行 `/api/app/**`
4. **实体 id**：`IdType.ASSIGN_ID`；平台表与关联/流水表不继承 BaseEntity（字段与表严格对齐）；生成器按 DDL 自动路由模块/判断继承
5. **飞书同步**：`E:\DeepSeekHarness\tools\feishu-task.ps1`（create/complete/list，PS5.1 兼容）；本地映射 `tools/feishu-tasks.json`；凭据 `tools/feishu.config.json`（**禁止提交**）
6. **pre-commit hook**：`tea-saas/.git/hooks/pre-commit`（克隆需重装；内容可从 `.harness/skills/code-review-commit/SKILL.md` 恢复）
7. **MySQL/Redis 均已就绪（Docker）**：`tea-mysql`（8.4.11，宿主 **3307**）+ `tea-redis`（宿主 **6379** 无密码）；`tea_saas` 39 表+种子已初始化；application.yml 本地默认直连，后端免环境变量启动。3306 是 phpStudy 的 MySQL 5.7（跑不了 02_data.sql 窗口函数，禁止用于本项目）
8. **前端 npm 陷阱**：`npm install --prefix` 会解析到父目录，必须 `cd admin-web` 后执行；Node v24 / npm 11 / npmmirror
9. **代码生成器**：`java -cp tea-generator/target/classes com.teasaas.generator.CodeGenerator --ddl=sql/init/01_schema.sql --out=. --tables=prd_*,inv_*`（先 `mvn -pl tea-generator -am package -DskipTests` 或复用已编译 classes；跳过已存在文件）

## 4. 接手后的建议顺序

1. 批次 3：订单+支付（F-ORD/F-PAY）——下单扣减直接注入 `InvStockService.deduct`（幂等已内建）；超时/退款走 restore；支付回调本地表+定时重试，不引 MQ
2. 批次 4：会员/营销/报表（F-MEM/F-MKT/F-RPT）
3. 前端逐模块替换 `views/error/Placeholder.vue`（`component` 字段如 `product/Materials` → `src/views/product/Materials.vue`，动态路由自动匹配）
4. 提醒 Owner：t100010（种子访谈）/ t100011（微信支付资质）仍在待办

## 5. 权威文档索引（冲突时以此为准）

`docs/`（人读权威）> `.harness/`（AI 投影）> 本文件（状态快照，会过时）
