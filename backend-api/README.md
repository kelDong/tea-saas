# 茶饮 SaaS 后端（backend-api）

茶饮甜品连锁加盟 SaaS 管理系统 · 后端服务。**JDK 21 + Spring Boot 3.5.5**，单项目 Maven 多模块（模块化单体），多租户 SaaS。

## 模块结构

```
tea-saas-backend
├── tea-common        # 统一返回(Result)/错误码(ErrorCode)/业务异常/分页（零框架依赖）
├── tea-framework     # 多租户插件/MyBatis-Plus/Sa-Token/全局异常/BaseEntity
├── tea-system        # 租户/用户/角色/菜单/权限（地基核心）
├── tea-franchise     # 加盟商/等级评定/门店档案
├── tea-supply        # 供应商/分级价格/采购单
├── tea-bom           # 原料档案/配方 BOM/毛利试算
├── tea-product       # 商品/分类/规格/产品库/加盟商上架
├── tea-inventory     # 门店原料库存/BOM 反算扣减/预警
├── tea-order         # 订单状态机/下单扣库存/门店收银
├── tea-payment       # 微信支付/储值/退款/回调重试
├── tea-member        # 会员/积分/储值
├── tea-marketing     # 优惠券
├── tea-report        # 门店报表/品牌看板/库存统计
├── tea-job           # 定时任务（超时取消/回调重试/对账）
└── tea-server        # 启动模块（聚合入口，禁止业务代码）
```

依赖方向：`common → framework → system → {bom, franchise, supply} → {product, inventory} → {order, payment, member, marketing, report} → server`

## 快速开始

```bash
# 前置：MySQL 8（库 tea_saas）+ Redis 7
# 编译
mvn clean compile

# 运行（默认 localhost:8080；DB/Redis 用环境变量覆盖，见 application.yml）
mvn -pl tea-server spring-boot:run
```

## 关键配置

- 多租户：`MybatisPlusConfig`（TenantLineInnerInterceptor，自动拼 tenant_id；排除表：tenant/sys_package/sys_menu）
- 超时取消：`tea.order.pay-timeout-minutes=15`（P1-5 决策）
- 错误码：`ErrorCode` 枚举（文档表述六位含前导零，实际整数值去前导零——避免 Java 八进制陷阱）

## 开发规范

**必读**：`E:\DeepSeekHarness\docs\茶饮SaaS-工程开发规范.md`（v1.1）与 `E:\DeepSeekHarness\.harness\`（AI 协作）。红线：金额用分、Redis 前缀 `teasaas:`、幂等、BizException、构造器注入、@Transactional(rollbackFor)、Result<T>。
