# 茶饮 SaaS · AI Harness 索引

> 本项目级 AI 上下文规范。任何 AI 编码工具（vibe coding）在接手本项目前，请先阅读本索引并按目录读取所需上下文。

## 目录结构

| 目录 | 用途 |
|---|---|
| `agents/` | Agent 角色/边界定义（Owner） |
| `rules/` | 硬性规范：工程结构、编码红线、开发流程 |
| `skills/` | 可复用流程技能：需求分析、编码、评审、单测、部署 |
| `wiki/` | 领域知识：业务模型、接口协议、数据模型 |
| `changes/` | 变更追踪（每次实质变更记录一份） |

## 推荐阅读顺序

1. **接手项目**：`agents/owner.md`（对齐角色与底线）→ `rules/编码规范.md`（红线）
2. **开始开发某功能**：`rules/工程结构.md` + `wiki/业务模型.md` → 定位模块与复用
3. **写代码**：`rules/编码规范.md`（红线+规范）→ `skills/coding-skill/SKILL.md`
4. **验收**：`skills/unit-test-ci/SKILL.md`（质量门禁）+ `rules/开发流程规范.md`

## 红线速览（详见 rules/编码规范.md）

1. 金额必须用 `Integer`（分）表示，禁止 Float/Double/BigDecimal
2. Redis Key 前缀必须 `teasaas:`（本项目前缀）
3. 消费/回调处理必须幂等（重复消息安全）
4. 异常必须用 `BizException` 体系，禁止原始异常泄露到 API
5. Controller 必须构造器注入（禁止 `@Autowired` 字段注入）
6. Service `@Transactional` 必须声明 `rollbackFor`（禁止异常时静默提交）
7. Vue3 必须用 `<script setup>`（禁止 Options API）
8. API 响应必须统一 `{code, message, data}`

## 全文文档

权威文档在 `docs/`（人读），本 `.harness/`（AI 读）是其精炼投影，二者冲突时以 `docs/` 为准。
