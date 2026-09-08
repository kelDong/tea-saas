# SKILL: 编码实现

> 目的：按红线与规范实现一个已评审的需求/功能。产出可编译、可测试的代码。

## 前置条件

- 需求已评审（PRD 有功能编号）
- 数据库/接口设计已确认
- 已定位到所属模块（见 `rules/工程结构.md`）

## 编码步骤

1. **定位模块与依赖**：确认功能属于哪个模块，能否复用下层已有能力
2. **实体映射**：按 `wiki/数据模型.md` 建 entity（含通用字段、@TableLogic、mybatis-plus 注解）
3. **DTO/VO**：定义入参出参，字段校验 + 金额用 Integer 分
4. **Service 接口 + impl**：业务逻辑 + 事务（`@Transactional(rollbackFor=Exception.class)`）
5. **Controller**：构造器注入、参数校验、返回 `Result<T>`
6. **错误码**：在 ErrorCode 登记新增错误码（模块+场景+序号）
7. **接口注解**：OpenAPI `@Operation/@Parameter/@Schema`
8. **单元测试**：核心逻辑必须测（状态机、金额、库存扣减）
9. **日志**：关键状态流转记日志，含 traceId

## 红线自查（写码时）

- [ ] 金额用 Integer 分？Redis key 用了 `teasaas:` 前缀？
- [ ] 幂等处理做了吗（回调/扣库存）？
- [ ] 异常用 BizException？构造器注入？@Transactional 声明了 rollbackFor？
- [ ] 返回值统一 Result<T>？
- [ ] 多租户：Service 有没有显式追加数据权限？

## 输出

可编译的模块代码 + 单元测试 + 接口注解完整。

## 参考

`rules/编码规范.md`、`rules/工程结构.md`、`wiki/数据模型.md`
