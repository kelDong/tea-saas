# CLAUDE.md — tea-saas 项目 AI 协作入口

> 本文件是 Claude Code 的自动入口。接手本项目前**必须**按顺序读完本文 → `.harness/README.md` → 本轮任务的权威文档。

## 项目是什么

茶饮甜品连锁加盟 SaaS 管理系统（多租户，模块化单体）。四端仓库：
- `backend-api/` — Java 后端（**JDK 21 + Spring Boot 3.5.5**，Maven 多模块 15+1 模块）
- `admin-web/` — 管理后台（Vue3，**未初始化**）
- `mini-program/`、`store-terminal/` — 未初始化

## 必读文档（按序）

1. `.harness/README.md` — AI 协作索引 + 红线速览
2. `.harness/rules/编码规范.md` — **8 条红线 + 多租户红线（违反即返工）**
3. `docs/茶饮SaaS-工程开发规范.md`（v1.1）— 工程宪法（结构/命名/接口/Git/测试）
4. `docs/茶饮SaaS-接口需求.md`（v1.0）— 前后端契约，**约 130 接口按 F-XXX 编号**
5. `docs/茶饮SaaS-数据库设计.md`（v1.1）— 39 张表 + 库存一致性 SQL
6. `docs/茶饮SaaS-PRD.md`（v1.1）— 需求与权限矩阵（11 项已拍板决策）

## 红线（速览，详见 .harness/rules/编码规范.md）

1. 金额必须 `Integer` 存"分"，禁止 Float/Double/BigDecimal
2. Redis Key 前缀必须 `teasaas:`（用 `RedisConstants`）
3. 回调/库存扣减必须幂等
4. 异常必须 `BizException` 体系（tea-common）
5. Controller 构造器注入（`@RequiredArgsConstructor`），禁止 `@Autowired`
6. `@Transactional` 必须声明 `rollbackFor = Exception.class`
7. Vue3 必须 `<script setup>`
8. 响应统一 `Result<T>`，分页 `PageResult<T>`

**多租户红线**：新表必含 `tenant_id`；禁止绕过租户插件（唯一例外见 `SysUserMapper.selectByUsernameForLogin` 的注释声明）；按 id 操作必须校验归属。

## 每次任务完成的固定流程（不可跳步）

```
1. mvn -q compile（backend-api 目录）→ 失败先修
2. 红线审查（对变更文件逐条检查上面 8 条 + 多租户）
3. git add + git commit -m "<type>(<scope>): <subject> F-XXX"（Conventional Commits + 功能编号）
   → pre-commit hook 会做硬门禁（@Autowired/System.out/事务/金额类型/SELECT *），违规则修复后重试，禁止 --no-verify
4. 若对应任务在飞书同步清单中：E:\DeepSeekHarness\tools\feishu-task.ps1 complete -Title "<任务标题>"
5. 输出摘要：📋 审查通过 | 红线 8/8 ✓ 多租户 ✓ | commit: <hash> <msg> | 飞书: <已同步/未同步>
```

## 环境注意事项（本机实测）

- **Maven 本地仓库在 `E:\工具\apache-maven-3.9.9\maven-repository`**（中文路径）
- **PowerShell 是 5.1 引擎**：`.ps1` 脚本需 UTF-8 **带 BOM**；不支持 `-AsHashtable`/`utf8NoBOM`
- MyBatis-Plus 3.5.12：jsqlparser 需单独依赖 `mybatis-plus-jsqlparser`（已配好）
- 实体主键用 `IdType.ASSIGN_ID`（雪花）
- 错误码是整数（文档里的六位前导零仅表述，避免八进制陷阱）

## 当前状态与下一步

见 `HANDOFF.md`（同目录）——包含 git 位置、M2 剩余任务、进行到一半的工作。
