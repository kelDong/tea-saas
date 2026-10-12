---
name: tea-ui-patrol
description: tea-saas UI 页面巡视/调试。用 wmux browser 逐页验证管理后台功能时使用：含可靠 eval 模式、Element Plus 组件操作坑、每类页面验证清单。
---

# UI 页面巡视（wmux browser 驱动）

## 基本工作流

```bash
wmux browser open <url>     # 打开页面（刷新已开页面也用它）
sleep 3
wmux browser snapshot       # 拿 eN ref（页面变化后 ref 全部失效，必须重新 snapshot）
wmux browser click eN       # 点元素
wmux browser get-text       # 纯文本读整页（验证结果最方便）
wmux browser eval "<js>"    # DOM 操作（下述模式）
```

## eval 可靠模式（重要）

**可靠**：`var` 声明 + for 循环 + 末尾字符串拼接表达式：

```js
var btns=document.querySelectorAll('button'); var r='nf';
for(var i=0;i<btns.length;i++){ if(btns[i].textContent.indexOf('确认收款')>=0){btns[i].click();r='ok';break} }
r
```

**不可靠**（会报 Uncaught）：箭头函数+find、JSON.stringify、裸标识符结尾、复杂选择器一次性写完、`new Event()`/`new KeyboardEvent()`。

## Element Plus 组件操作坑（实战踩过）

**1. el-input-number 内部包含 `.el-input`** —— 按 `.el-input input` 选择会误伤数字框。区分：

```js
var inputs=document.querySelectorAll('.el-dialog .el-input input');
var num=null, txts=[];
for(var i=0;i<inputs.length;i++){
  if(inputs[i].closest('.el-input-number')){ num=inputs[i] } else { txts.push(inputs[i]) }
}
```

**2. 程序化设值**（v-model 生效）：

```js
num.focus(); num.value='20';
var ev=document.createEvent('HTMLEvents'); ev.initEvent('input',true,true);
num.dispatchEvent(ev);   // 数字框再加一个 change 事件
```

**3. el-date-picker**：设 input.value 无效。要点日历单元格：

```js
document.querySelector('.el-date-editor input').click()          // 弹日历
// 然后：td 内的 .el-date-table-cell 才是点击目标（点 td 本身可能只关不选）
// 选完后手动点「查询」按钮触发加载（@change 不一定走）
```

**4. 关闭弹层**：`.el-drawer__close-btn` 或 `.el-dialog__headerbtn`。

## 每类页面验证清单

| 页面类型 | 验证点 |
|---|---|
| 列表页 | 表格渲染、总条数、筛选器联动、分页、状态-操作按钮矩阵（按状态只出现合法按钮） |
| 表单弹窗 | 打开回填、校验拦截（留空提交看 toast）、提交成功 toast + 列表刷新 |
| 统计卡片 | **数字自洽**（如 营业额 = 各渠道之和；不闭合=对账 bug，见下） |
| 流转操作 | 点按钮后状态迁移 + 按钮组切换 + toast（如 已支付→制作中→待取餐→已完成） |
| 实时页 | ws 状态 tag（实时推送中/不可用） |

## 资金数字对不上账的标准排查链

UI 数字矛盾（如 营业额 ≠ 微信+储值+现金）→ 读组件源码找 API → 查 VO 字段 → 查 Mapper SQL 的 CASE WHEN 渠道分支 → 对 DTO 枚举注释（**注意 POS 渠道编号 1现金/2微信/3储值 与订单表 pay_channel 1微信/2储值/3现金 不同**）→ SQL 真值核对：

```bash
docker exec tea-mysql mysql -uroot -proot tea_saas -N -e \
"SELECT pay_channel, COUNT(*), SUM(pay_amount) FROM ord_order \
 WHERE status IN (2,3,4,5) AND pay_time>='YYYY-MM-DD 00:00:00' GROUP BY pay_channel"
```

## 其他注意

- 登录态失效（跳 /login）→ 用 `tea-prog-login` 重登，别手输验证码
- 菜单切换卡死（面包屑变主区不变）历史上是 rAF 遮挡节流问题，已修复（AdminLayout setTimeout 兜底）；若再现，先抓现场：`getComputedStyle(el)` + class 快照，别急着改 CSS
- 巡视顺序建议：每端 1 个列表页练手 → 统计页核数字 → 流转页做完整状态链
