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

未提交内容：`tea-generator/pom.xml`（新建，见 §3 进行中）、`CLAUDE.md`、本文件。

## 2. M2 进度（对照飞书任务清单）

| 任务 | 状态 | 飞书 task_id |
|---|---|---|
| Maven 15 模块骨架 | ✅ 已提交 | （未单独建任务） |
| SQL 初始化（39 表） | ✅ 已提交 | t100007 ✅ |
| tea-system（登录/RBAC/用户/角色/日志/密码Runner） | ✅ 已提交 | t100001 ✅ |
| **代码生成器** | 🔨 **进行到一半（见 §3）** | t100008 ⬜ |
| admin-web 前端骨架 | ⬜ 未开始 | t100009 ⬜ |
| 并行：种子访谈 + 微信资质 | ⬜ Owner 侧（提醒） | t100010/t100011 |

## 3. 进行到一半：代码生成器（tea-generator）

**已完成**：
- `tea-generator/pom.xml` 已创建（依赖 tea-framework + lombok）
- 设计已定：解析 `sql/init/01_schema.sql` 的 DDL → 离线生成四件套（Entity/Mapper/Service/Controller）
  - 表前缀路由模块：`sys_/tenant→tea-system`、`fra_→tea-franchise`、`sup_→tea-supply`、`bom_→tea-bom`、`prd_→tea-product`、`inv_→tea-inventory`、`ord_→tea-order`、`pay_→tea-payment`、`mem_→tea-member`、`mkt_→tea-marketing`
  - 类型映射：bigint→Long、varchar→String、int→Integer、tinyint→Integer、decimal→BigDecimal、datetime→LocalDateTime、date→LocalDate、time→LocalTime、json→String
  - 通用字段全含的表（tenant_id+create_by+create_time+update_by+update_time+del_flag）→ 继承 `BaseEntity` 并只生成业务字段
  - **已存在的输出文件跳过**（不覆盖手写代码，如 SysUser 系列）
  - Controller 模板：Result<T> + 分页 + @Validated + OpenAPI 注解 + @OpLog

**未完成**：
- 父 POM `modules` 列表**尚未加入** `<module>tea-generator</module>`（pom 已建但未挂载——先补这个）
- 生成器代码未写（建议结构：`CodeGenerator`（main）+ `TableMeta`/`ColumnMeta` + 模板渲染；输入参数：DDL 路径/输出根/表名过滤）
- 未跑验证（计划：先对 BOM 模块 3 张表 bom_material/bom_formula/bom_formula_item 生成并编译验证）
- 工程规范第 14 章 = 生成器产物要求（禁 TODO 占位、生成后人工 review）

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
