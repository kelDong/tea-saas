---
name: tea-env-recover
description: tea-saas 本地环境恢复/启动。死机重启、容器挂掉、后端或前端未起时使用。固定序列：Docker→3容器→后端 jar→vite→DB 校验→登录探活。
---

# tea-saas 环境恢复（固定序列）

按序执行，每步验证通过再做下一步。账号：brand01(R2)/fra01(R6)/store01(R7)，密码 `Smoke2026x`，租户 9001。

## 1. Docker 与容器

```bash
# Docker Desktop 启动后等 engine 就绪（约 5~15s），三容器有 restart 策略会自动拉起
docker ps --format "{{.Names}} {{.Status}}"   # 应见 tea-mysql / tea-redis / minio
```

容器名固定：`tea-mysql`（端口 **3307**，root/root）、`tea-redis`(6379)、`minio`(9000)。

## 2. DB 校验

```bash
docker exec tea-mysql mysql -uroot -proot tea_saas -N \
  -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='tea_saas'"
# 期望 40（39 业务表 + flyway_schema_history）
```

**禁用** `find /c /v ""` 之类的 cmd 语法——Git Bash 会解析成 GNU find 扫全 C 盘。

## 3. 后端（8080）

```bash
# 代码没改：直接起
cd "E:/DeepSeekHarness/tea-saas" && java -jar backend-api/tea-server/target/tea-server-1.0.0-SNAPSHOT.jar > Temp/backend.log 2>&1   # 后台运行

# 代码改过：Windows 下 jar 被运行进程锁定，必须先停旧进程再打包
# （后台 shell 用 KillShell 停，或任务管理器杀 java.exe）
cd /e/DeepSeekHarness/tea-saas/backend-api && mvn -q package -DskipTests -pl tea-server -am
```

探活（就绪很快，通常 <30s）：

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" -d '{"username":"store01","password":"Smoke2026x"}'
# 期望 200
```

## 4. 前端 vite（5173）

```bash
cd /e/DeepSeekHarness/tea-saas/admin-web && npm run dev > ../Temp/admin-web-dev.log 2>&1   # 后台运行
curl -s -o /dev/null -w "%{http_code}" http://localhost:5173/   # 期望 200
```

## 5. 已知坑

- **Redis 重启会清 token**：浏览器旧 token 失效跳 /login，用 `tea-prog-login` 技能重登，不用人工输验证码
- Git Bash 里等待用 `sleep 2`，不要 `timeout /t 2`
- 后端日志 `Temp/backend.log`，vite 日志 `Temp/admin-web-dev.log`
- Maven 本地仓库在 `E:\工具\apache-maven-3.9.9\maven-repository`（无需改，配好了）
