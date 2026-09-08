# SKILL: 任务完成自动审查与提交

> 目的：每次项目任务完成时，自动执行代码审查，**审查通过后才允许 git 提交**。把"审查→提交"串联成强制工作流，配合 git pre-commit 硬门禁（双层保障）。

## 触发时机

当且仅当：一个项目任务（功能开发/修复/重构）已实现完毕，即将向 git 仓库提交时。

## 工作流（严格按序执行，禁止跳步）

### 第 1 步：编译验证

```bash
# backend-api 工程（工作目录 tea-saas/backend-api）
mvn -q compile
```

编译失败 => 修复后重试，**禁止带病进入审查**。

### 第 2 步：红线审查（对照 .harness/rules/编码规范.md 逐条检查变更文件）

对 `git diff --name-only` 的每个文件逐项检查：

| # | 红线 | 检查方法 |
|---|---|---|
| 1 | 金额必须 Integer 存"分" | 检查金额字段类型（amount/price/fee/cost 命名的字段） |
| 2 | Redis Key 前缀 `teasaas:` | 检查所有 Redis 操作的 key 是否引用 RedisConstants 常量 |
| 3 | 回调/扣减必须幂等 | 支付回调、库存扣减、退款逻辑查重复处理保护 |
| 4 | 异常用 BizException 体系 | 无原始异常抛到 Controller；catch 不吞异常 |
| 5 | Controller 构造器注入 | 无 @Autowired 字段注入 |
| 6 | @Transactional 带 rollbackFor | 所有事务注解必须 rollbackFor = Exception.class |
| 7 | Vue3 `<script setup>` | 前端文件无 Options API |
| 8 | 统一 Result<T> 响应 | Controller 返回值全部为 Result<T>/PageResult |

**多租户专项**（一票否决）：
- 新表 SQL 是否含 tenant_id
- 是否存在绕过租户插件的 SQL（@InterceptorIgnore 需注释原因）
- 按id操作是否校验归属（tenant_id + store_id）

### 第 3 步：提交前 Checklist（docs/茶饮SaaS-工程开发规范.md 第 15 章）

- [ ] 无魔法值（状态/类型用枚举）
- [ ] 写操作有事务 + 操作日志/库存流水
- [ ] 接口有 OpenAPI 注解、错误码已在 ErrorCode 登记
- [ ] 核心逻辑有单测（状态机/金额/库存扣减）

### 第 4 步：提交（触发 pre-commit 硬门禁）

```bash
git add <变更文件>
git commit -m "<type>(<scope>): <subject>"
```

提交信息规范：`feat(order): 实现订单状态机 F-ORD-02`（Conventional Commits + 功能编号，见规范 10.2）。

### 第 5 步：处理 hook 拒绝（如发生）

pre-commit 检查失败 => 输出 ❌ 原因 => **回到第 2 步修复** => 重新 add+commit。
**禁止使用 `--no-verify` 绕过**（绕过即违反本流程，需在 PR 说明中显式标注并获 Owner 同意）。

## 审查结论记录

每次提交后输出一行审查摘要：

```
📋 审查通过 | 红线 8/8 ✓ 多租户 ✓ | commit: <hash> <message>
```

## 第 6 步：同步飞书任务状态（提交成功后）

若本项目任务已同步到飞书（凭据 `tools/feishu.config.json` 已配置），提交成功后更新对应任务状态：

```powershell
# 任务完成 → 标记飞书任务完成（按任务标题，需先 create 过）
E:\DeepSeekHarness\tools\feishu-task.ps1 complete -Title "<任务标题>"
```

- 首次出现的新任务：先 `create -Title "<标题>" -Desc "<说明>"`
- 凭据未配置（config 缺失或占位符）：**跳过本步并在摘要中注明「飞书未同步」**，不阻塞提交流程
- 批量收尾时逐任务调用 complete

完整摘要格式（含飞书状态）：

```
📋 审查通过 | 红线 8/8 ✓ 多租户 ✓ | commit: <hash> <message> | 飞书: <已同步/未同步>
```

## 边界说明

- 本技能负责**提交前**门禁；跨模块大变更（新模块/改表结构）还需走 `.harness/skills/expert-reviewer/SKILL.md` 深度评审
- pre-commit hook 只做轻量静态检查（grep 级）；语义级审查（幂等/越权/业务一致性）由本技能第 2 步人工/AI 完成
- Hook 脚本位置：`tea-saas/.git/hooks/pre-commit`（克隆仓库后需重新安装，见项目 README）
