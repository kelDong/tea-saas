# HANDOFF.md — 交接状态快照

> 写于：M2 中途（tea-system 完成后，代码生成器进行到一半）。接手者：Claude Code（或其他 AI 工具）。
> 配套入口：`CLAUDE.md`（必读）。

## 1. Git 位置（tea-saas 仓库）

```
34b9fb2 feat(system): 用户/角色管理+操作日志切面+admin密码初始化 F-SYS-02/04
6036d40 feat(system): tea-system认证模块——登录/RBAC菜单聚合/Sa-Token租户拦截 F-SYS-02/03
f6fe6fe feat(system): SQL初始化脚本——39张表DDL+种子数据 F-SYS-01/02
33c7c84 feat(framework): M2地基搭建——15模块骨架+多租户插件+统一返回/异常体系
```

未提交内容：`CLAUDE.md`（本地有改动，属 Owner 手笔，由 Owner 决定何时提交）。

## 2. M2 进度（对照飞书任务清单）

| 任务 | 状态 | 飞书 task_id |
|---|---|---|
| Maven 15 模块骨架 | ✅ 已提交 | （未单独建任务） |
| SQL 初始化（39 表） | ✅ 已提交 | t100007 ✅ |
| tea-system（登录/RBAC/用户/角色/日志/密码Runner） | ✅ 已提交 | t100001 ✅ |
| **代码生成器** | ✅ 已完成（见 §3） | t100008 |
| admin-web 前端骨架 | ⬜ 未开始（M2 最后一步） | t100009 ⬜ |
| 并行：种子访谈 + 微信资质 | ⬜ Owner 侧（提醒） | t100010/t100011 |

## 3. 已完成：代码生成器（tea-generator）✅

- 纯 JDK 零依赖（JUL 日志，规避 pre-commit 的 System.out.println 禁令），`java -cp tea-generator/target/classes com.teasaas.generator.CodeGenerator --ddl=sql/init/01_schema.sql --out=. --tables=bom_*` 直接运行
- 结构：`CodeGenerator`（main/写入/跳过已存在）+ `core`（SqlDdlParser/TableMeta/ColumnMeta/TypeMapping/ModuleRouter/NameUtil）+ `render/Templates`
- 表前缀路由：`sys_/tenant→tea-system`、`fra_→tea-franchise`、`sup_→tea-supply`、`bom_→tea-bom`、`prd_→tea-product`、`inv_→tea-inventory`、`ord_→tea-order`、`pay_→tea-payment`、`mem_→tea-member`、`mkt_→tea-marketing`
- 类型映射：bigint→Long、varchar→String、int/tinyint→Integer、decimal→BigDecimal、datetime→LocalDateTime、date→LocalDate、time→LocalTime、json→String
- 通用字段全含的表继承 `BaseEntity`（如 bom_material/bom_formula）；关联/流水表平铺（如 bom_formula_item，含 tenant_id/create_time 显式赋值）
- 已验证：BOM 三表 18 文件生成 + 全量 `mvn compile` 通过；`--tables=sys_user` 验证跳过逻辑（5 个已有文件被保护）
- 注意：DDL 表级注释是 `COMMENT='x'`（带等号）、列级是 `COMMENT 'x'`（带空格），解析正则已兼容两者
- 规范 14 的"基础单测骨架"暂不生成（模块无 test 依赖设施，M3 随业务逻辑补）

## 4. 关键实现事实（接手者容易踩的坑）

1. **登录查用户的租户绕过**：`SysUserMapper.selectByUsernameForLogin` 带 `@InterceptorIgnore` + 注释声明——业务查询禁止模仿（规范 6.1）
2. **实体 id**：`IdType.ASSIGN_ID`；平台表（tenant/sys_package/sys_menu）与关联/流水表不继承 BaseEntity（字段与表严格对齐）
3. **飞书同步**：`E:\DeepSeekHarness\tools\feishu-task.ps1`（create/complete/list，PS5.1 兼容版）；本地映射 `tools/feishu-tasks.json`；凭据在 `tools/feishu.config.json`（**禁止提交**）
4. **pre-commit hook**：`tea-saas/.git/hooks/pre-commit`（克隆仓库需重装；内容可从本仓库 hook 或 `.harness/skills/code-review-commit/SKILL.md` 恢复）
5. **admin 密码**：首次部署设 `TEA_ADMIN_PASSWORD` 环境变量启动，自动替换 SQL 占位哈希（AdminPasswordInitRunner）
6. **无 MySQL/Redis**：一切只到 `mvn compile` 为止；集成测试需要环境（可建议 Owner 装 Docker 或云数据库）

## 5. 接手后的建议顺序

1. 提交本交接包（CLAUDE.md/HANDOFF.md/tea-generator pom）
2. 完成 tea-generator（§3），用 BOM 模块验证 → 提交 → 飞书 complete `M2:代码生成器`
3. admin-web 骨架：`npm create vite`（Vue3+TS）+ Element Plus + Pinia + 登录页对接 `/api/v1/auth/*`（菜单树驱动动态路由，权限码做 `v-permission` 指令）→ 提交 → 飞书 complete `M2:admin-web 前端骨架` → **M2 收官**
4. M3 按 `.harness/agents/owner.md` 的"分阶段并行"策略：后端业务模块可用生成器打底 + 手工补业务逻辑（库存扣减/状态机/采购计价是重点，参考数据库设计第 13 章 SQL）

## 6. 权威文档索引（冲突时以此为准）

`docs/`（人读权威）> `.harness/`（AI 投影）> 本文件（状态快照，会过时）
