# 茶饮 SaaS 系统 · 工程开发规范

> 版本：v1.1 ｜ 关联文档：`茶饮SaaS项目规划书.md`(v1.1)、`茶饮SaaS-PRD.md`(v1.1)、`茶饮SaaS-数据库设计.md`(v1.1)、`茶饮SaaS-需求评审报告.md`(v1.1)｜ 状态：已评审待开发
> 本文档是**项目宪法**：所有代码必须遵循本规范。M2 地基搭建前必须评审通过；违反本规范的 PR 不得合入。
> 适用范围：后端（Java/Spring Boot 多模块）、管理后台（Vue3）、小程序（uni-app）、数据库、Git。
>
> **v1.1 变更说明**：版本要求更新为 JDK 21 + Spring Boot 3.5.5；新增第 16 章《项目 AI 协作规范（.harness）》。

---

## 目录

1. [总则](#1-总则)
2. [工程结构规范](#2-工程结构规范)
3. [命名规范](#3-命名规范)
4. [后端代码规范](#4-后端代码规范)
5. [数据库开发规范](#5-数据库开发规范)
6. [多租户开发规范](#6-多租户开发规范)
7. [接口规范](#7-接口规范)
8. [安全规范](#8-安全规范)
9. [日志与异常规范](#9-日志与异常规范)
10. [Git 协作规范](#10-git-协作规范)
11. [前后端协作规范](#11-前后端协作规范)
12. [测试规范](#12-测试规范)
13. [工具链与质量门禁](#13-工具链与质量门禁)
14. [代码生成器产物规范](#14-代码生成器产物规范)
15. [Checklist（提交前自检）](#15-checklist提交前自检)
16. [项目 AI 协作规范（.harness）](#16-项目-ai-协作规范harness)

---

## 1. 总则

### 1.1 三条铁律

1. **租户安全优先**：任何业务 SQL 不得绕过租户插件；数据权限缺失 = 阻断发布（见第 6 章）
2. **金额必须精确**：一律以"分"为单位的整数运算，禁止 float/double 参与金额计算
3. **可追溯**：关键操作（下单/采购/改价/改配方/退款）必须写操作日志与库存流水

### 1.2 规范冲突处理

- 本规范与框架/库的约定冲突时，以本规范为准并记录到决策记录
- 本规范未覆盖的，遵循业界通行最佳实践（后端参考阿里《Java开发手册》P3C，前端参考 Vue 官方风格指南）

### 1.3 版本要求

| 项 | 版本 | 说明 |
|---|---|---|
| JDK | 21（LTS） | 固定 21，本项目不使用更高版本新特性 |
| Spring Boot | 3.5.5 | 3.x 主线，与 JDK 21 兼容 |
| Maven | 3.9+ | 统一 settings 配置私服/镜像 |
| MySQL | 8.0+ | InnoDB，utf8mb4 |
| Node | 20 LTS+ | 前端/小程序构建（本机 24 可用） |

---

## 2. 工程结构规范

### 2.1 顶层目录结构

```
tea-saas/                    # Git 仓库根（含父 POM）
├── pom.xml                  # 父 POM：统一依赖版本/dependencyManagement
├── .gitignore
├── .editorconfig
├── .github/ 或 .gitlab/     # CI/CD 流水线
├── docs/                    # 项目文档（规划/PRD/数据库设计/规范）
├── sql/                     # 数据库脚本
│   ├── init/                # 初始化脚本（建库、建表、种子数据）
│   ├── upgrade/             # 升级脚本（V1.0.1__xxx.sql）
│   └── test/                # 测试数据
├── tea-common/
├── tea-framework/
├── tea-system/
├── tea-franchise/
├── tea-supply/
├── tea-bom/
├── tea-product/
├── tea-inventory/
├── tea-order/
├── tea-payment/
├── tea-member/
├── tea-marketing/
├── tea-report/
├── tea-job/
└── tea-server/              # 启动模块（聚合依赖，不写业务代码）
```

### 2.2 模块职责红线

- **tea-server**：只含 `Application` 启动类与 `application*.yml`，禁止业务代码
- **tea-common**：只放无三方框架依赖的工具（常量、枚举、结果类），禁止引入 Spring
- **tea-framework**：放框架级配置（安全/Redis/MyBatis-Plus/租户插件），禁止业务逻辑
- **业务模块**（system/franchise/supply/...）：只依赖 `common`、`framework` 及下游业务模块，**禁止反向依赖**

### 2.3 依赖方向（防循环依赖）

```
common → framework → system → { bom, franchise, supply } → { product, inventory } → { order, payment, member, marketing, report } → server
```

- 业务模块之间只允许"上层依赖下层"，如 `order` 可依赖 `product`、`inventory`
- 禁止 A 依赖 B 且 B 依赖 A；出现循环依赖即重构
- 用 Maven Enforcer 插件强制校验（见 13 章）

### 2.4 包结构（以 tea-order 为例）

```
com.teasaas.order
├── controller/        # 只做参数接收/校验/调用 service，禁止业务逻辑
├── service/           # 业务逻辑；接口 + impl 分开
├── mapper/            # MyBatis-Plus Mapper；复杂 SQL 用 XML
├── entity/            # 数据库实体（DO），与表一一对应
├── dto/               # 入参对象（req/query）
├── vo/                # 出参对象
├── convert/           # 对象转换（MapStruct 或手写，禁止散落转换代码）
├── enums/             # 模块内枚举（订单状态、业务类型）
└── constants/         # 模块内常量
```

- 实体层命名单词：`entity`（DO）、`dto`、`vo` 三层不混用
- 跨模块引用：只能引下层的 `dto/vo/enums`，禁止引下层的 `entity`

---

## 3. 命名规范

### 3.1 Java 命名

| 类型 | 规则 | 示例 |
|---|---|---|
| 类 | 大驼峰，名词 | `OrderService`、`PurchaseOrderController` |
| 接口 | 大驼峰 | `IStockService` 或 `StockService`（接口+Impl 后缀统一二选一，本项目用 Impl） |
| 方法 | 小驼峰，动词开头 | `createOrder()`、`cancelOrder()`、`listByStore()` |
| 布尔变量 | 用 `is/has/can` 前缀 | `isDeleted`、`canSell` |
| 常量 | 全大写下划线 | `ORDER_STATUS_PAID` |
| 枚举 | 类型大驼峰，值全大写 | `OrderStatus.PAID` |
| 包名 | 全小写 | `com.teasaas.order.service` |
| Mapper 方法 | 与 SQL 意图对应 | `selectByStoreIdAndStatus` |

### 3.2 数据库命名

- 表：`模块前缀_表名` 小写下划线（`ord_order`、`inv_store_material_stock`）
- 字段：小写下划线，不用缩写（`stock_qty` 而非 `stkqty`）
- 索引：`idx_表名_字段`、唯一索引 `uk_表名_字段`
- 通用字段固定：`id/tenant_id/create_by/create_time/update_by/update_time/del_flag`

### 3.3 接口/URL 命名

- RESTful：名词复数，小写，连字符分隔：`/api/v1/orders/{id}`
- 动作优先用 HTTP 方法表达；必须用动词时（如"接单"）用 `POST /orders/{id}/accept`
- 前端接口函数：`api/order/acceptOrder`（小驼峰 + 领域前缀）

### 3.4 前端/小程序命名

| 类型 | 规则 | 示例 |
|---|---|---|
| Vue 组件文件 | 大驼峰 | `OrderList.vue`、`SpecSheet.vue` |
| 页面路由路径 | 小写连字符 | `/orders/accept` |
| JS 变量/函数 | 小驼峰 | `cartCount`、`fetchOrderList()` |
| CSS 类 | BEM 或 kebab-case | `.order-card`、`.cart-foot` |
| 常量（Vue） | 全大写下划线 | `ORDER_STATUS_MAP` |
| uni-app 页面目录 | 小驼峰或 kebab-case | `pages/order/confirm` |

---

## 4. 后端代码规范

### 4.1 分层职责

| 层 | 允许 | 禁止 |
|---|---|---|
| Controller | 参数绑定、基础校验（@Validated）、调用 service、返回 Result | 业务逻辑、直接操作 mapper |
| Service | 业务逻辑、事务边界、跨模块编排 | 直接返回 entity（转 VO）、写 controller 逻辑 |
| Mapper | SQL 查询/写入 | 业务判断 |
| Entity | 与表映射、MyBatis-Plus 注解 | 业务方法、@TableField(exist=false) 的非查询字段尽量少用 |

### 4.2 事务规范

- 写操作（涉及多表/跨模块）必须标注 `@Transactional(rollbackFor = Exception.class)`
- 事务只放 Service 层，禁止 Controller 标注事务
- 大事务拆分：一个方法只做一件事；禁止事务内远程调用（如微信支付回调处理要先落库再发请求）
- 库存扣减必须与订单创建同一事务（见数据库设计第 13 章）

### 4.3 通用代码约定

- 禁止 `System.out.println`、`printStackTrace`；统一用 SLF4J
- 禁止硬编码魔法值：状态、类型必须用枚举（`OrderStatus.PAID`）
- 字符串拼接用占位符：`log.info("order {} created", orderNo)`，禁止 `"order " + orderNo`
- 集合判空用 `CollectionUtils.isEmpty` / `StringUtils.hasText`（Spring 工具类）
- Optional 用于可空返回；禁止滥用 `Optional.get()`

### 4.4 对象转换

- 简单字段映射：MapStruct（编译期生成）
- 复杂聚合：在 service 显式组装，禁止在 entity 里写 toVO()

### 4.5 统一返回与异常

- 返回结构：`Result<T> { code, message, data, traceId }`（tea-common 定义）
- 业务异常：`BizException(ErrorCode, message)`，ErrorCode 见接口规范第 7.4
- Controller 不 try-catch 业务异常，由全局异常处理器统一处理（tea-framework）
- 参数校验：Bean Validation 注解（@NotNull/@Size/@Pattern）优先，禁止手写 if 校验散落

---

## 5. 数据库开发规范

### 5.1 表结构

- 所有业务表必须含通用字段（见数据库设计 1.1）：`tenant_id`、`del_flag`、审计字段
- 金额列：`INT`/`BIGINT` 存"分"，禁止 DECIMAL 金额
- 数量列：原料库存 `DECIMAL(10,3)`；件数 `INT`
- 逻辑删除：`del_flag TINYINT DEFAULT 0`；唯一索引必须含 `del_flag`
- 禁止物理外键，禁止触发器，禁止存储过程（业务放应用层）

### 5.2 SQL 规范

- 简单 CRUD 用 MyBatis-Plus 内置方法；多表/复杂统计用 XML
- 禁止 `SELECT *`；必须显式列名
- 分页必须用分页插件（PageHelper 或 MyBatis-Plus PaginationInnerInterceptor），禁止内存分页
- 批量插入用 `foreach` 或 MP 的 `saveBatch`，限制单批 ≤ 500 条
- `LIKE` 查询：禁止前置 `%`（`LIKE '%xx'` 无法走索引）
- 更新必须带条件（`WHERE id = ?`），禁止无条件 UPDATE/DELETE

### 5.3 索引规范

- 单表索引 ≤ 5 个，联合索引 ≤ 4 列
- 高流量查询必须覆盖索引（见数据库设计 12.2）
- 新建/修改索引需在 `sql/upgrade/` 留脚本并注明原因
- 上线前必须过慢查询检查（explain 执行计划）

### 5.4 脚本规范

- `sql/init/` 全量可重复执行（幂等）；`sql/upgrade/` 增量不可修改历史文件
- 脚本命名：`V<版本>__<描述>.sql`（如 `V1.0.1__add_order_index.sql`）
- 种子数据与结构分离：`init/` 下 `schema.sql` + `data.sql`

---

## 6. 多租户开发规范

### 6.1 硬性规则

1. 新表必须含 `tenant_id`，且加入租户插件白名单（默认全量）
2. **禁止**绕过租户插件的 SQL（如 `@InterceptorIgnore(tenantLine = "true")`）——确需绕过的（如平台侧统计）必须 code review + 注释说明原因
3. 数据权限：Service 层必须显式追加数据范围（`store_id IN (...)`），禁止"先查全量再内存过滤"
4. 顾客端接口：只能按"当前 openid → 租户 + 门店"取数，禁止跨租户查询参数

### 6.2 隔离测试要求

- 每个新功能必须带隔离用例：A 租户 token 访问 B 租户数据 → 期望 403/空结果
- 单元测试中必须验证 SQL 自动带上了 `tenant_id`（MyBatis-Plus 租户插件拦截结果）

### 6.3 平台侧 vs 业务侧

- 平台管理员（tenant_id=0）访问：走平台专用接口，禁止复用业务接口
- `sys_menu`、`tenant` 等平台表不加租户列；业务表必须加

---

## 7. 接口规范

### 7.1 URL 与版本

- 统一前缀：`/api/v1/...`（业务）；平台管理：`/api/admin/v1/...`；小程序：`/api/app/v1/...`
- 一期 v1；破坏性变更升 v2，禁止修改 v1 语义

### 7.2 HTTP 方法与语义

| 方法 | 语义 | 示例 |
|---|---|---|
| GET | 查询（幂等） | `GET /api/v1/orders?storeId=&status=` |
| POST | 创建/动作 | `POST /api/v1/orders`、`POST /api/v1/orders/{id}/accept` |
| PUT | 全量更新 | `PUT /api/v1/products/{id}` |
| PATCH | 部分更新 | `PATCH /api/v1/listings/{id}/price` |
| DELETE | 删除（逻辑删除） | `DELETE /api/v1/materials/{id}` |

### 7.3 分页规范

- 入参：`page`（从 1 开始）、`size`（默认 10，上限 100）
- 出参：`PageResult<T> { list, total, page, size }`
- 所有列表接口必须分页，禁止全量返回

### 7.4 错误码规范（ErrorCode 枚举）

```
格式：模块(2位) + 场景(2位) + 序号(2位)，如 040201
模块：00 通用 / 01 系统 / 02 产品 / 03 订单 / 04 支付 / 05 会员 / 06 供应链 / 07 库存 / 08 加盟 / 09 营销 / 10 报表
```

| 示例 | 含义 |
|---|---|
| 000001 | 参数错误（由校验框架生成） |
| 000002 | 未登录 / 会话过期 |
| 000003 | 无权限 |
| 030101 | 订单不存在 |
| 030102 | 订单状态不允许该操作 |
| 040201 | 支付回调签名校验失败 |
| 070101 | 库存不足 |

- HTTP 状态码：业务错误统一 200 + code（前端按 code 处理）；鉴权失败 401；无权限 403；未找到 404
- 前端错误提示：展示 `message`，禁止展示裸异常堆栈

### 7.5 接口文档

- 全量接入 OpenAPI（springdoc）；`tea-server` 聚合扫描，文档随代码生成
- 接口必须写 `@Operation` 摘要与 `@Parameter` 说明；实体字段写 `@Schema(description=...)`
- 提交前保证 `GET /api/v3/api-docs` 能正常生成（CI 检查）

---

## 8. 安全规范

| 项 | 规范 |
|---|---|
| 认证 | Sa-Token；B 端账号密码 + 验证码；C 端微信登录（code2session） |
| 密码 | BCrypt 加密存储，禁止明文/可逆加密 |
| 密钥 | 支付密钥/数据库密码只放环境变量或配置中心，禁止进 Git |
| 传输 | 生产环境全站 HTTPS；`SameSite` 与 CORS 白名单配置 |
| 注入 | 全部参数化 SQL（MyBatis-Plus/预编译），禁止字符串拼接 SQL |
| XSS | 前端输出转义；富文本输入白名单过滤 |
| 越权 | 所有"按 id 操作"的接口必须校验归属（`tenant_id + store_id` 匹配当前上下文） |
| 限流 | 下单/支付/登录接口必须限流（Redis 令牌桶） |
| 敏感数据 | 手机号等脱敏输出（`138****1234`） |
| 审计 | 下单/采购/改价/改配方/退款写操作日志（F-SYS-04） |

---

## 9. 日志与异常规范

### 9.1 日志规范

- 框架：SLF4J + Logback（tea-framework 统一配置，日志级别可配置）
- 日志级别：
  - `INFO`：接口调用摘要（traceId、耗时）、关键状态流转（订单/采购单状态变化）
  - `WARN`：可恢复异常、重试场景、库存低于阈值
  - `ERROR`：业务异常落库失败、支付回调异常、未捕获异常
- 日志必须含 `traceId`（请求入口生成，MDC 传递），用于全链路串联
- 禁止日志打印敏感信息（密码、token、支付密钥）
- 错误日志必须带上下文（单号/ID），禁止只打 message 无对象信息

### 9.2 异常规范

- 业务可预期异常 → `BizException(ErrorCode)`，全局处理器统一返回
- 系统异常 → 全局处理器兜底，记 ERROR 日志，返回"系统繁忙，请稍后重试"
- 禁止吞异常（空 catch）；确需忽略的必须注释原因 + 记 WARN
- 异步任务异常必须捕获记录，禁止静默失败

---

## 10. Git 协作规范

### 10.1 分支模型

```
main          # 生产，只允许从 release 合入，打 tag v1.0.0
├── dev       # 集成分支，feature 合入后联调
├── release/  # 发布分支（从 dev 拉出，只修 bug，合回 main + dev）
└── feature/xxx  # 功能分支：从 dev 拉出，命名 feature/F-ORD-02-接单状态机
```

- 命名：`feature/<PRD功能编号>-<简述>`、`fix/<问题简述>`、`chore/<简述>`
- 禁止直接向 main 提交；禁止把未评审代码合入 dev

### 10.2 提交信息规范（Conventional Commits）

```
<type>(<scope>): <subject>

feat(order): 实现订单接单状态机 F-ORD-03
fix(payment): 修复回调重复处理 F-PAY-01
refactor(framework): 抽取租户插件配置
docs: 更新数据库设计 v1.0
test(order): 补充库存不足用例
```

- type：`feat / fix / refactor / docs / test / chore / perf / style`
- scope：模块名（order/payment/bom/framework/...）
- 一条提交只做一件事；写清楚为什么（引用功能编号/问题单）

### 10.3 PR 规范

- PR 标题同提交规范；描述含：变更内容、测试情况、关联文档/功能编号
- PR 检查项（合入门禁）：编译通过、单测通过、Checkstyle 通过、无规范红线违规
- 个人全栈也走 PR：先自审，再合入 dev（保持"可回滚"）

---

## 11. 前后端协作规范

| 项 | 规范 |
|---|---|
| 接口定义 | 后端先行（OpenAPI 文档），前端按文档开发，禁止前端猜字段 |
| 字段命名 | 统一 camelCase；时间用 ISO8601 字符串或时间戳，前端统一格式化 |
| 金额传输 | 后端传"分"整数，前端展示时格式化（工具函数 `formatFen(1234) => ¥12.34`） |
| 分页 | 统一 `PageResult<T>` 结构，前端封装 usePagination |
| 枚举传输 | 后端传 code（数字/字符串），前端维护枚举映射（`ORDER_STATUS_MAP`），禁止传中文 |
| 权限 | 后端返回当前用户菜单树+按钮权限码集合；前端动态路由 + `v-permission` 指令 |
| 错误处理 | 前端统一 axios 拦截器处理 `code!=0`，弹出 message |
| 空态/加载 | 列表必须有 loading、空态、错误态三态 |

---

## 12. 测试规范

### 12.1 测试分层与命名

| 层 | 框架 | 位置 | 命名 |
|---|---|---|---|
| 单元测试 | JUnit5 + Mockito | 各模块 `src/test/java` | `OrderServiceTest` |
| 集成测试 | SpringBootTest + H2/Testcontainers | tea-server `src/test` | `OrderFlowIntegrationTest` |
| 接口测试 | MockMvc / Apifox 集合 | tea-server | `OrderApiTest` |
| 并发测试 | JMeter | `test/`（非 Java） | 下单并发、库存扣减 |

### 12.2 覆盖要求

- 核心业务（订单状态机、优惠券计算、金额计算、库存扣减）单测覆盖 ≥ 90%
- 新增业务代码必须带测试；无测试的 feat PR 不合入
- 支付回调、退款必须有过账测试（幂等、重复回调、验签失败）

### 12.3 测试数据

- 单元测试用 Mock，不连真实库
- 集成测试用独立测试库（test profile），禁止污染 dev 数据
- 测试数据不写死时间；用相对时间（`now()`）

### 12.4 必跑用例（上线门禁）

- [ ] 跨租户隔离用例（A 访问 B → 403/空）
- [ ] 库存并发扣减（200 并发无超卖、无负库存）
- [ ] 支付回调幂等（重复回调只处理一次）
- [ ] 金额精确性（优惠券+积分+储值组合支付分毫不差）

---

## 13. 工具链与质量门禁

| 工具 | 用途 | 门禁 |
|---|---|---|
| Maven Enforcer | 校验 Java 版本、依赖冲突、禁止循环模块依赖 | CI 阻断 |
| Checkstyle（阿里 P3C 规则） | 代码风格 | CI 阻断（warn 级别可过，error 阻断） |
| Spotless 或 google-java-format | 格式化（提交前 `mvn spotless:apply`） | CI 校验 |
| SonarQube（可选二期） | 质量门禁 | 二期接入 |
| Maven Surefire | 单测执行 | 失败阻断 |
| OpenAPI diff（可选） | 接口变更检查 | 二期接入 |
| git hooks（husky 前端） | 提交前 lint + 格式化 | 本地强制 |

### CI 流水线（GitHub Actions / GitLab CI）

```
触发：push 到 feature/dev / PR
1. checkout → 2. setup JDK21 → 3. mvn verify（含单测+checkstyle）
4. 前端构建 + lint → 5. 构建 Docker 镜像 → 6. 部署 test 环境 → 7. 冒烟测试
```

---

## 14. 代码生成器产物规范

> M2 将实现代码生成器（见规划书 5.5），生成产物必须符合本规范：

- 生成范围：`entity / mapper(+xml) / service(接口+impl) / controller` + 基础单测骨架
- 生成实体：自动带通用字段注解（@TableLogic、审计字段填充 handler）
- 生成的 Controller 默认走 `Result<T>` + 分页 + `@Validated`
- 生成代码禁止包含业务注释占位符（TODO 由开发者手写，生成器不产出 TODO）
- 生成后必须人工 review，禁止"生成即合入"

---

## 15. Checklist（提交前自检）

**后端提交前**：
- [ ] 无绕过租户插件的 SQL（有则已注释原因并过评审）
- [ ] 写操作有事务、有操作日志/流水
- [ ] 金额全为"分"整数运算
- [ ] 无魔法值（已用枚举）、无 System.out、无吞异常
- [ ] 接口有 OpenAPI 注解、错误码已登记
- [ ] 单测通过、核心逻辑有覆盖
- [ ] 越权校验（归属检查）已做

**前端提交前**：
- [ ] 接口字段与 OpenAPI 一致
- [ ] 金额已格式化、枚举已映射
- [ ] loading/空态/错误态三态齐全
- [ ] lint 通过、无 console.log 残留

**数据库变更提交前**：
- [ ] 脚本幂等、在 `sql/upgrade/` 留增量
- [ ] 新表含 tenant_id + 通用字段
- [ ] 索引已评估（高流量查询有覆盖）

---

## 16. 项目 AI 协作规范（.harness）

> 适用于所有 AI 编码工具（vibe coding）：Cursor、Claude Code、Windsurf、Copilot 等。本规范定义项目如何向 AI 工具暴露上下文，确保 AI 产出自动遵守本宪法。

### 16.1 定位与关系

| 目录 | 使用者 | 作用 |
|---|---|---|
| `docs/` | 人 | 权威文档（详细、可评审） |
| `.harness/` | AI 工具 | 项目 AI 上下文规范（精炼、结构化），是 docs/ 的 AI 可读投影 |

**冲突处理**：`.harness/` 与 `docs/` 不一致时，**以 `docs/` 为准**（.harness 是投影，修改需求/规范应先改 docs 再同步）。

### 16.2 目录结构

```
.harness/
├── README.md                  # 索引入口 + 红线速览 + 推荐阅读顺序
├── agents/
│   └── owner.md               # Owner 角色、工作原则、架构决策基线（一期不微服务/不引MQ/整单退款）
├── rules/
│   ├── 工程结构.md             # 目录结构 + 模块边界 + 依赖方向（对应第 2 章）
│   ├── 编码规范.md             # 8 条红线 + 多租户红线 + API 响应（对应第 3/4/6/7 章）
│   └── 开发流程规范.md         # 7 阶段流水线 + Git 流程 + Checklist（对应第 10/12 章）
├── skills/                    # 5 个可复用流程技能
│   ├── request-analysis/SKILL.md
│   ├── coding-skill/SKILL.md
│   ├── expert-reviewer/SKILL.md
│   ├── unit-test-write/SKILL.md
│   ├── unit-test-ci/SKILL.md
│   └── deploy-verify/SKILL.md
├── wiki/
│   ├── 业务模型.md             # 业务主线、租户模型、实体关系、状态机
│   ├── 接口协议.md             # URL/方法/响应/错误码/关键接口（对应第 7 章）
│   └── 数据模型.md             # 33+ 张表清单 + 库存一致性 SQL（对应第 5 章 / 数据库设计）
└── changes/                   # 变更追踪（每次实质变更记录一份）
```

### 16.3 红线速览（.harness/rules/编码规范.md 核心，AI 必须遵守）

1. **金额字段必须使用 `Integer`（分）**——禁止 Float/Double/BigDecimal 表示金额
2. **Redis Key 前缀必须 `teasaas:`**——禁止无名/通用前缀
3. **回调/消费处理必须幂等**——支付回调、库存扣减、退款异步都必须安全处理重复消息
4. **异常必须用 `BizException` 体系**——禁止原始异常泄露到 API
5. **Controller 构造器注入**——禁止 `@Autowired` 字段注入
6. **Service `@Transactional` 必须声明 `rollbackFor`**——禁止异常时静默提交
7. **Vue3 必须 `<script setup>`**——禁止 Options API
8. **API 响应统一 `{code, message, data}`**——使用 `Result<T>`

多租户红线补充：新表必含 `tenant_id`；禁止绕过租户插件；Service 层显式追加数据权限。

### 16.4 使用约定

- 任何 AI 工具接手项目，先读 `.harness/README.md`（索引）+ `agents/owner.md`（对齐角色与底线）+ `rules/编码规范.md`（红线）
- 开发某功能时，读 `wiki/业务模型.md` 定位领域，`rules/编码规范.md` 约束实现
- `.harness/skills/` 把流程固化为可复用 SKILL，AI 可据此一步步执行（需求→编码→评审→单测→部署）
- `changes/` 记录变更，与 Git 提交规范（10.2）配合，保证可追溯

### 16.5 维护规范

- 改动 .harness 前，先改 `docs/` 对应章节，再同步（保持"权威在 docs/"）
- `.harness/` 不承载 docs 已有的全部细节，只保留当前阶段生效的红线/模型/流程
- 每季度或大版本评审一次 `.harness/` 是否与 docs 一致、是否冗余

---

*本文档随项目演进更新；重大变更需评审并在"修订记录"标注版本。*
