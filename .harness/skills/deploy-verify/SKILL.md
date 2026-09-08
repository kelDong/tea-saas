# SKILL: 部署验证

> 目的：把构建产物安全部署到环境，并验证关键链路可用。

## 环境

`dev` / `test` / `prod` 三套；本地用 Docker Compose（MySQL + Redis + tea-server + nginx）。

## 部署步骤

1. **构建**：`mvn clean package -DskipTests`（产物在 tea-server/target）
2. **镜像**：Dockerfile（多阶段：Maven build → JRE run），打版本 tag
3. **配置**：环境变量注入密钥（数据库/微信支付/Redis 密码），**禁止入镜像明文**
4. **推送/部署**：`docker compose up -d`；prod 用 CI 自动部署
5. **迁移**：执行 `sql/upgrade/` 增量脚本（先备份）
6. **启动**：`spring-boot:run`（本地）或容器；确认健康检查通过

## 验证清单（部署后必跑冒烟）

- [ ] 服务健康检查 `/actuator/health` UP
- [ ] 接口文档可访问 `/api/v3/api-docs`（OpenAPI 正常）
- [ ] 登录取 token → 调测试接口
- [ ] 关键链路：点单 → 支付(沙箱/正式) → 接单 → 核销 → 报表
- [ ] 多租户隔离用例
- [ ] 日志可采集（有 traceId）

## 上线前安全检查

- [ ] HTTPS、接口限流、密码加密
- [ ] 支付密钥不落库明文、敏感字段脱敏
- [ ] 数据备份 + 恢复演练

## 回滚

- 镜像带版本 tag；异常时 `docker compose down` + 回退上一 tag + 回滚数据库（若需）
- 每次发布在 `.harness/changes/` 记录

## 参考

`rules/开发流程规范.md`、`skills/unit-test-ci/SKILL.md`
