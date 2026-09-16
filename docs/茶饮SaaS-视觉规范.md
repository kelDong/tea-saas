# 茶饮SaaS — 视觉规范(Design Spec)

> 版本:v1.0(2026-09-16)
> 来源:`E:\DeepSeekHarness\prototype\admin-panel.html`(总部后台原型,唯一视觉事实源)
> 落地:`admin-web/src/styles/`(tokens.css → theme.css → patterns.css,经 index.css 全局加载)
> 适用:admin-web 全部页面;后续 mini-program / store-terminal 的色彩语言可参照 §2,布局不适用

## 1. 设计原则

**暖茶调(茶褐 + 奶油 + 琥珀)**:管理后台不必是冷蓝色工具。深茶棕侧栏如"茶叶在暗处",琥珀主色如"温茶点灯"——签名元素是**深棕侧栏 + 琥珀激活轨**(激活菜单项 3px 琥珀左条 + 琥珀微光底)。

三条纪律:

1. **色彩只从 token 取**:页面禁止裸写 hex,一律 `var(--ts-*)` 或 EP 变量;
2. **数字用 tabular-nums**:所有金额/数量/编码列加 `.mono`,表格数值右对齐;
3. **克制的暖**:暖色只出现在底色/边框/表头/标签等"容器"层,正文墨色保持高对比,信息密度优先。

## 2. Design Tokens(tokens.css)

### 2.1 色彩

| Token | 值 | 用途 |
|---|---|---|
| `--ts-canvas` | `#FAF6EE` | 页面底色(奶油) |
| `--ts-paper` | `#FFFFFF` | 卡片/面板 |
| `--ts-ink` | `#3A2B1E` | 主文本 |
| `--ts-ink-2` | `#8A7A66` | 次级文本/表头字 |
| `--ts-ink-3` | `#B7A992` | 占位/弱化 |
| `--ts-primary` | `#D97B29` | 品牌琥珀(主按钮/链接/激活) |
| `--ts-primary-deep` | `#B85F1C` | hover/强调/链接 |
| `--ts-primary-soft` | `#F8EDDD` | 琥珀浅底(标签/头像底) |
| `--ts-primary-bright` | `#F5B573` | 深底上的琥珀亮字(侧栏激活) |
| `--ts-success` / `--ts-success-soft` | `#5E7C4C` / `#E9F0E3` | 成功(茶绿) |
| `--ts-danger` / `--ts-danger-soft` | `#C2542F` / `#F8E8E1` | 危险(赭红) |
| `--ts-warning` / `--ts-warning-soft` | `#C9982F` / `#F8EDD8` | 警告(金黄,**提案色**,见 §6) |
| `--ts-border` | `#EFE6D6` | 边框/分隔线 |
| `--ts-gray-tag` | `#F0EBDF` | 灰标签/进度条底 |
| `--ts-table-header` | `#FBF7EE` | 表头暖底 |
| `--ts-stripe` / `--ts-fill-hover` | `#FCF9F1` / `#FDFAF3` | 斑马纹 / 行 hover |
| 侧栏组 | `#2E251B` 底,`#D9CCB4` 字,激活 `rgba(217,123,41,.16)` 底 + `#F5B573` 字 | |

### 2.2 形状/字体/遮罩

| Token | 值 | 用途 |
|---|---|---|
| `--ts-radius-panel` | `14px` | 卡片/面板 |
| `--ts-radius-btn` | `8px` | 按钮/输入框 |
| `--ts-radius-dialog` | `16px` | 弹窗/登录卡 |
| `--ts-radius-pill` | `99px` | 胶囊标签/进度条 |
| `--ts-font` | PingFang SC → Microsoft YaHei 栈 | 全局 |
| `--ts-mono` | ui-monospace/Consolas | 数字/编码列 |
| `--ts-mask` | `rgba(42,32,22,.45)` | 弹窗遮罩(暖黑) |

字号阶:页标题 17/700(面包屑末级 16/700)、面板标题 14/700、正文 13、次级 12、表头 12/600、统计值 26/800、胶囊标签 11/600。

## 3. Element Plus 2.9.1 映射(theme.css)

### 3.1 变量覆盖(优先方式)

- **主色** `--el-color-primary` = `--ts-primary`,色阶 light-3/5/7/8 按 mix 公式推导(`#E4A369/#ECBD94/#F4D7BF/#F7E5D4`),light-9 = `--ts-primary-soft`,**dark-2 = `#B85F1C`**(原型 hover,覆盖计算值 `#AE6221`);
- success/danger(+error 双通道)/warning/info 同构全套色阶,见 theme.css;
- **文本/边框/填充/背景** 全套映射暖色系;`--el-fill-color-lighter` = `--ts-stripe`(斑马纹与下拉选项 hover 共用);
- **全局组件变量**(:root 生效):`--el-border-radius-base(8px)`、`--el-font-family`、`--el-mask-color`(暖黑遮罩);
- **组件作用域变量**(必须在组件选择器上覆盖,见陷阱 4):`.el-table/.el-table-v2` 的 `--el-table-header-bg-color/-header-text-color/-row-hover-bg-color/-border-color/-tr-bg-color` 等;`.el-card` 的 `--el-card-border-radius(14px)/--el-card-padding(18px)`;`.el-dialog` 的 `--el-dialog-border-radius(16px)`;`.el-input/.el-textarea/.el-date-editor/.el-autocomplete` 的 `--el-input-focus-border-color(琥珀)`;`.el-pagination` 的 `--el-pagination-hover-color`。

### 3.2 选择器补丁(2.9 无变量,必须全局非 scoped)

| 补丁 | 选择器 | 原因 |
|---|---|---|
| 表头字重 | `.el-table th.el-table__cell { font-weight:600 }` | 无字重变量 |
| 标签胶囊化 | `.el-tag` + 语义变体(soft 底 + deep 字,去边框) | 无逐类型配色变量 |
| message-box 圆角 | `.el-message-box { border-radius:16px }` | 无变量 |
| 弹窗标题 | `.el-dialog__title { 16px/700 }` | 无变量 |
| 键盘焦点 | `:focus-visible { outline: 2px 琥珀 }` | 可达性底线 |

**陷阱记录**(改 EP 相关样式前必读):

1. `el-menu` 的 `background-color/text-color/active-text-color` 内联 props 生成内联 style,**压过一切 CSS**——侧栏菜单只允许用 `--el-menu-*` 变量(`.admin-menu` 上),禁止恢复内联 props;
2. dialog/dropdown/message 是 teleport 节点,**scoped 样式永远匹配不到**,EP 补丁只能写 theme.css(全局);
3. 条纹行颜色 = `--el-fill-color-lighter`(与下拉 hover 共用),不要单独写 striped 选择器;
4. **EP 组件级变量定义在组件选择器上,不在 :root**——`--el-table-*` 在 `.el-table`、`--el-card-*` 在 `.el-card`、`--el-dialog-*` 在 `.el-dialog`、`--el-input-*` 在 `.el-input`、`--el-pagination-*` 在 `.el-pagination`。在 `:root` 覆盖这些变量**完全无效**(自定义属性就近解析,组件自身定义永远赢);必须在同名组件选择器上覆盖,依赖 theme.css 在 EP css 之后加载赢得同 specificity 竞争。已实测踩坑:表头暖底/卡片圆角在 :root 写法下静默失效。

## 4. 页面布局模板(patterns.css)

统一边距:内容区 `20px 22px 40px`(AdminLayout 承担),卡片间距 16px。

### 4.1 表格页(参考:product/Materials.vue)

```
┌─ el-card(14px 圆角) ──────────────────────────┐
│ .page-toolbar: [筛选输入] [查询] ←spacer→ [新增] │  gap 12 / mb 16
│ ┌─ el-table stripe ──────────────────────────┐ │
│ │ 表头 #FBF7EE 12/600 │ 数值列右对齐 .mono     │ │
│ │ 状态列 el-tag 胶囊 │ 操作列 link 按钮 fixed │ │
│ └────────────────────────────────────────────┘ │
│ .pager: 分页右下(total, prev, pager, next, sizes)│
└────────────────────────────────────────────────┘
.page-note: "对应 PRD:F-XXX|业务备注"(12px 次级色)
```

### 4.2 表单弹窗页(参考:biz/Coupons.vue)

- `el-dialog` 480–560px(简单表单 480,含分区块 560),标题 16/700;
- `el-form label-width="100px"`,输入单位提示用 `.tip`;
- footer:取消(默认) + 确定(primary),`@closed` 清校验。

### 4.3 仪表盘(参考:biz/Workspace.vue)

- `.stat-row`(auto-fit minmax 200px)× `.stat-card`:label 12 / value 26/800 tabular-nums / sub 11(↑绿 ↓红);
- 双栏用 `.grid2`(≤1100px 折单栏);
- 占比/水位用 `.bar`(99px 圆角,琥珀填充)。

### 4.4 通用类清单

`.page-toolbar` `.toolbar-spacer` `.toolbar-input(220px)` `.toolbar-select(140px)` `.pager` `.full` `.tip` `.mono` `.link`/`.link.danger` `.panel`+`.panel-head` `.stat-row`+`.stat-card` `.grid2` `.bar` `.page-title` `.page-note`;页面切换过渡 `page-fade`(带 prefers-reduced-motion 守卫)。

## 5. 组件使用规则

| 场景 | 规则 |
|---|---|
| 主操作 | `type="primary"`(琥珀);新增类入口用 `type="success"`,置于工具栏最右(spacer 推) |
| 行内操作 | `el-button link type="primary"`,危险动作 `link type="danger"` |
| 状态标签 | `el-tag` + 语义 type(启用 success/停用 info/待处理 warning/失败 danger),**禁止**自定义色 |
| 金额/数量/编码 | `.mono` + 右对齐;金额一律 `fen2yuan`(分→元,红线) |
| 表格 | `stripe` + `v-loading`;操作列 `fixed="right"` |
| 分页 | `layout="total, prev, pager, next, sizes"` + `.pager` 右下 |
| 弹窗表单 | label-width 100px;单位/说明用 `.tip` 而非 placeholder 堆砌 |

## 6. 待拍板项

- **warning 色 `#C9982F`(金黄)**:原型未定义警告色,本规范提案。因主色已是琥珀橙,warning 特意向黄相拉开;若实际标签中辨识度不足,备选 `#A98E3B`(橄榄金)。**当前满减券类型标签用的就是它,请 Owner 在页面上确认观感**。

## 7. 页面改造清单(29 页)

已完成(参考页,新页面照抄):

| 页面 | 模板 | 状态 |
|---|---|---|
| biz/Workspace | 仪表盘 | ✅ 范例 |
| product/Materials | 表格页 | ✅ 范例 |
| biz/Coupons | 表格+表单弹窗 | ✅ 范例 |
| login/LoginView | 品牌登录页 | ✅ |
| layout/AdminLayout | 布局壳 | ✅ |

待改造(EP 变量层已自动覆盖约 80%,仅需:工具栏类名换 `.page-toolbar`、删 scoped 重复样式、数值列加 `.mono`、按需加 `.page-note`):

- system/Users、system/Roles(如有)、system/OpLogs
- product/Formulas、product/Products、product/Listings
- supply/Suppliers、supply/TierPrices(如有)、supply/Purchases、supply/Inventory
- franchise/Franchisees、franchise/Stores
- biz/Orders、biz/Members、biz/StoreReport、biz/BrandReport
- store/Dashboard、store/Orders、store/Pos、store/Inventory、store/Settle
- platform/Dashboard、platform/Tenants、platform/Packages
- error/NotFound、error/Placeholder

**Backlog**(不在本期):侧栏菜单图标(sys_menu 无 icon 字段,需加列+种子);深色模式(本期无);移动端适配(仅 grid2/stat-row 有断点)。

## 8. 变更记录

| 版本 | 日期 | 内容 |
|---|---|---|
| v1.0 | 2026-09-16 | 首版:token/EP 映射/布局模板/组件规则 + 布局壳、登录页、3 范例页落地 |
