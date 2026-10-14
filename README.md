# tea-saas · 茶饮甜品连锁加盟 SaaS 管理系统

多租户 SaaS（模块化单体），覆盖**品牌总部 → 加盟商 → 门店**的加盟全链路：商品/BOM 配方、采购供应、门店库存（BOM 反算扣减）、收银下单、微信支付/储值、会员积分、优惠券、门店报表与品牌看板。

**四端一仓**（monorepo）：

| 目录 | 端 | 技术栈 | 状态 |
|---|---|---|---|
| `backend-api/` | 后端服务 | JDK 21 · Spring Boot 3.5.5 · Maven 多模块（15+1）· MyBatis-Plus · Sa-Token · MySQL 8 · Redis 7 · MinIO · WebSocket | ✅ M1~M18 完成 |
| `admin-web/` | 管理后台（品牌/加盟商/门店） | Vue 3.5 · TypeScript · Vite 6 · Element Plus · Pinia · Vue Router | ✅ M3~M18 完成 |
| `mini-program/` | 微信小程序（C 端顾客） | uni-app · Vue 3 · Pinia | ✅ M7 起迭代中 |
| `store-terminal/` | 门店收银平板 | 复用 admin-web + WebSocket 接单 + 平板适配（M11 拍板） | 📐 方案就绪，未单独建仓 |

后端四条接入通道：**管理端 REST / 门店 WS 接单 / 微信支付回调 / C 端 REST**。接口约 130 个按 `F-XXX` 编号（见接口需求文档），数据库 39 张表（含 `tenant_id` 多租户隔离）。

## 快速开始（本地开发）

### 环境要求

- JDK 21、Maven 3.9+
- Node.js ≥ 22（smoke / e2e 需要）、npm
- MySQL 8、Redis 7、MinIO（连接参数可用环境变量覆盖，默认值见 `backend-api/tea-server/src/main/resources/application.yml`）
- 微信开发者工具（仅调试小程序需要）

### 1. 初始化数据库

```bash
mysql -u root -p tea_saas < backend-api/sql/init/01_schema.sql
mysql -u root -p tea_saas < backend-api/sql/init/02_data.sql
```

种子账号（品牌/加盟商/门店演示账号）见 `02_data.sql`，首次登录后请改密。

### 2. 启动后端（默认 localhost:8080）

```bash
cd backend-api
mvn -q -DskipTests install
java -jar tea-server/target/tea-server-1.0.0-SNAPSHOT.jar
```

> 多模块下请勿用 `mvn -pl tea-server spring-boot:run`（会在根 POM 错位执行）。模块结构与依赖方向见 [`backend-api/README.md`](backend-api/README.md)。

### 3. 启动管理后台（Vite dev，`/api`、`/ws` 已代理到 8080）

```bash
cd admin-web
npm install
npm run dev        # http://localhost:5173
```

### 4. 启动微信小程序（C 端）

```bash
cd mini-program
npm install
npm run dev:mp-weixin     # 产物 dist/dev/mp-weixin，导入微信开发者工具（关闭域名校验）
```

## 测试

```bash
# 后端集成测试（Testcontainers，需本机 Docker）
cd backend-api && mvn -pl tea-server -am test

# 四角色冒烟：收银/退款/取消/日结对账/小程序下单/资产/WS 接单/越权负例（Node ≥22 零依赖，后端需已启动）
node tools/smoke/run-all.mjs

# 管理后台 E2E（Playwright）
cd admin-web && npm run test:e2e
```

## 生产部署（deploy/）

本机演练与云服务器同一套流程：构建产物 → 填 `.env` → Compose 五服务（nginx + backend + mysql + redis + minio）。

```powershell
cd deploy
powershell -ExecutionPolicy Bypass -File build.ps1   # 产物落 deploy/build/{backend,web}/ + deploy/sql/，可选 -SkipBackend/-SkipWeb/-SkipSql
copy .env.example .env                               # 逐项填写密码（建议纯字母数字）；对外端口 NGINX_PORT（演练默认 8090）
docker compose up -d --build
```

- 首次启动：`.env` 中 `TEA_ADMIN_PASSWORD` 用于初始化 `admin` 账号，登录一次后即可移除。
- OSS 对外地址 `OSS_PUBLIC_ENDPOINT` 上云时**必须**改为域名/公网 IP（容器内 `localhost` 指向自身）。
- 微信支付 V3 资质下发后，在 `.env` 启用 `WX_PAY_*` 各项（当前 `tea.pay.mock=true` 走 Mock 支付）。
- SQL 唯一事实源是 `backend-api/sql/init/`，`deploy/sql/` 由脚本复制生成，勿手改。

## 文档索引（`docs/`）

| 文档 | 内容 |
|---|---|
| `茶饮SaaS-项目规划书.md` | 项目背景与总体规划 |
| `茶饮SaaS-PRD.md`（v1.1） | 需求与权限矩阵（11 项已拍板决策） |
| `茶饮SaaS-工程开发规范.md`（v1.1） | 工程宪法：结构/命名/接口/Git/测试 |
| `茶饮SaaS-接口需求.md`（v1.0） | 前后端契约，约 130 接口按 `F-XXX` 编号 |
| `茶饮SaaS-数据库设计.md`（v1.1） | 39 张表 + 库存一致性 SQL |
| `茶饮SaaS-视觉规范.md` / `茶饮SaaS-测试计划.md` | UI 规范 / 测试策略 |
| `茶饮SaaS-项目复盘-M2至M18.md` | 阶段复盘（真缺陷规律/待改进缺口） |
| `docs/diagrams/`、`docs/marketing/` | 架构图 / 营销模块资料 |

## 工程红线（违反即返工）

1. 金额一律 `Integer` 存"分"，禁止 Float/Double/BigDecimal
2. Redis Key 前缀必须 `teasaas:`（用 `RedisConstants`）
3. 回调/库存扣减必须幂等
4. 异常必须走 `BizException` 体系（tea-common）
5. Controller 构造器注入（`@RequiredArgsConstructor`），禁止 `@Autowired`
6. `@Transactional` 必须声明 `rollbackFor = Exception.class`
7. Vue3 一律 `<script setup>`
8. 响应统一 `Result<T>`，分页 `PageResult<T>`

**多租户红线**：新表必含 `tenant_id`；禁止绕过租户插件；按 id 操作必须校验归属。细则见 `.harness/rules/编码规范.md`。

## AI 协作

本仓库由 AI 结对开发，协作约定见 [`CLAUDE.md`](CLAUDE.md) 与 `.harness/README.md`（含红线全文、会话记忆、提交门禁：Conventional Commits + 功能编号 `F-XXX`）。

## 当前进度

M18（品牌报表/看板）已完成；进行中的工作、剩余任务与环境细节见 [`HANDOFF.md`](HANDOFF.md)。
