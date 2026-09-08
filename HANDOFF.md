# HANDOFF.md — 交接状态快照

> 写于：M2 收官（admin-web 前端骨架完成并提交）。接手者：Claude Code（或其他 AI 工具）。
> 配套入口：`CLAUDE.md`（必读）。

## 1. Git 位置（tea-saas 仓库）

```
（本次）feat(admin-web): 管理后台前端骨架——登录/动态路由/v-permission F-SYS-03
（本次）fix(framework): 全局异常处理补 NotLoginException→HTTP 401 F-SYS-03
2b78e41 feat(generator): 离线DDL代码生成器——四件套生成+BOM三表落地验证 M2
4189d08 docs(diagram): 系统架构图——Excalidraw可编辑格式（三端+统一接入+15模块+存储）
6a5f5b9 docs: AI交接包——CLAUDE.md入口+HANDOFF状态快照+generator模块骨架
34b9fb2 feat(system): 用户/角色管理+操作日志切面+admin密码初始化 F-SYS-02/04
```

未提交内容：`CLAUDE.md`（本地有改动，属 Owner 手笔，由 Owner 决定何时提交）。

## 2. M2 进度（对照飞书任务清单）——**全部完成，M2 收官** 🎉

| 任务 | 状态 | 飞书 task_id |
|---|---|---|
| Maven 15 模块骨架 | ✅ 已提交 | （未单独建任务） |
| SQL 初始化（39 表） | ✅ 已提交 | t100007 ✅ |
| tea-system（登录/RBAC/用户/角色/日志/密码Runner） | ✅ 已提交 | t100001 ✅ |
| 代码生成器 | ✅ 已提交（见 §3） | t100008 ✅ |
| admin-web 前端骨架 | ✅ 已提交（见 §3.5） | t100009 ✅ |
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

## 3.5 已完成：admin-web 前端骨架 ✅

- Vite 6 + Vue 3.5 + TS 5.7（strict）+ Element Plus 2.9（全量引入）+ Pinia 2 + vue-router 4；`npm run dev`（5173，代理 `/api`→`localhost:8080`），`npm run build` = vue-tsc 类型检查 + vite 构建（已通过）
- 契约对接 `docs/茶饮SaaS-接口需求.md` F-SYS-03：登录 `/auth/login`（验证码一期可选，拉取失败不阻塞）、`/auth/captcha`、`/auth/logout`、`/auth/menus`
- `utils/request.ts`：axios 封装，解包 `Result<T>`（code=0 成功）；业务码 2 或 HTTP 401 → 清 token 硬跳 `/login`；token 持久化 localStorage `teasaas:token` / `teasaas:token-name`（tokenName 用 LoginVO 动态值，默认 satoken）
- 动态路由（`router/dynamic.ts`）：菜单树是唯一路由来源；目录 path 绝对、叶子 path 相对，`component` 字段按 `/{component}.vue` 后缀匹配 `src/views/**`；**未实现的菜单回落到 `views/error/Placeholder.vue`**（M3 逐模块替换为真页面）；守卫首次进入拉菜单 → `addRoute('layout', ...)` → 重试导航
- `v-permission` 指令（`directive/permission.ts`）：无权限码直接移除元素；演示页 `views/platform/Dashboard.vue`
- 配套后端补丁：`GlobalExceptionHandler` 增加 `NotLoginException` → HTTP 401 + 业务码 2（SaTokenConfig 注释里声明的设计意图，前端 401 跳转依赖它）
- 已知非阻塞项：Element Plus 全量引入致主 chunk ~1.1MB（M3 可做 manualChunks/按需引入）；`/auth/me` 接口前端暂未使用

## 4. 关键实现事实（接手者容易踩的坑）

1. **登录查用户的租户绕过**：`SysUserMapper.selectByUsernameForLogin` 带 `@InterceptorIgnore` + 注释声明——业务查询禁止模仿（规范 6.1）
2. **实体 id**：`IdType.ASSIGN_ID`；平台表（tenant/sys_package/sys_menu）与关联/流水表不继承 BaseEntity（字段与表严格对齐）
3. **飞书同步**：`E:\DeepSeekHarness\tools\feishu-task.ps1`（create/complete/list，PS5.1 兼容版）；本地映射 `tools/feishu-tasks.json`；凭据在 `tools/feishu.config.json`（**禁止提交**）
4. **pre-commit hook**：`tea-saas/.git/hooks/pre-commit`（克隆仓库需重装；内容可从本仓库 hook 或 `.harness/skills/code-review-commit/SKILL.md` 恢复）
5. **admin 密码**：首次部署设 `TEA_ADMIN_PASSWORD` 环境变量启动，自动替换 SQL 占位哈希（AdminPasswordInitRunner）
6. **MySQL 已就绪（2026-09-08）/ Redis 仍无**：MySQL 8.0.28 装在 `E:\mysql80`（**端口 3307**，root/root），`tea_saas` 库 39 表+种子数据已初始化验证。3306 是 phpStudy 的 MySQL 5.7（旧项目在用；5.7 跑不了 `02_data.sql` 的窗口函数，禁止用于本项目）。后端启动前设 `$env:MYSQL_PORT="3307"`；MySQL8 暂为临时进程，管理员终端跑 `E:\mysql80\bin\mysqld.exe --install MySQL8 --defaults-file=E:\mysql80\my.ini` + `net start MySQL8` 可注册成服务。验证码接口依赖 Redis，无 Redis 时登录页自动降级为纯账密登录
7. **前端 npm 陷阱**：`npm install --prefix <dir>` 在本机会解析到父目录，必须 `cd admin-web` 后执行；Node v24 / npm 11，registry 走 npmmirror

## 5. 接手后的建议顺序（M3 起）

1. M3 按 `.harness/agents/owner.md` 的"分阶段并行"策略：后端业务模块用生成器打底（§3）+ 手工补业务逻辑（库存扣减/状态机/采购计价是重点，参考数据库设计第 13 章 SQL）
2. 前端逐模块把 `views/error/Placeholder.vue` 替换为真页面：新建 `src/views/<目录>/<页面>.vue` 即被动态路由自动匹配（component 字段如 `product/Materials` → `src/views/product/Materials.vue`），无需改路由代码
3. 提醒 Owner：t100010（种子访谈）/ t100011（微信支付资质）仍在待办

## 6. 权威文档索引（冲突时以此为准）

`docs/`（人读权威）> `.harness/`（AI 投影）> 本文件（状态快照，会过时）
