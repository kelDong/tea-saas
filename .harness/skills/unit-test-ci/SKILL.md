# SKILL: CI 与质量门禁

> 目的：确保每次提交/PR 达标，不让有质量问题或红线违反的代码合入。

## 门禁（必跑，任一失败即阻断）

1. `mvn clean test` 全绿（单元 + 集成）
2. Checkstyle（阿里 P3C 规则）无 error 级违规
3. 多租户隔离用例通过（A 访问 B → 403/空）
4. 编译通过（JDK21 特有：`mvn -q compile`）

## CI 流水线（GitHub Actions / GitLab CI）

```
触发：push 到 feature/dev / PR
1. checkout → 2. setup JDK21 → 3. mvn verify（含单测+checkstyle）
4. 前端构建 + lint → 5. 构建 Docker 镜像 → 6. 部署 test 环境 → 7. 冒烟测试
```

## 质量工具

| 工具 | 用途 | 位置 |
|---|---|---|
| Maven Enforcer | JDK 版本、依赖冲突、禁循环依赖 | 父 POM |
| Checkstyle | 代码风格（P3C） | CI 阻断 |
| Spotless | 格式化 | `mvn spotless:apply` |
| Surefire | 单测 | 失败阻断 |

## 冒烟测试（部署后）

- [ ] 顾客端：选门店 → 点单 → 支付 → 取餐码
- [ ] 门店端：接单 → 制作 → 核销
- [ ] 总部：报表数据正常、库存统计一致
- [ ] 多租户：A/B 品牌互不可见

## 参考

`rules/开发流程规范.md`、`rules/编码规范.md`
