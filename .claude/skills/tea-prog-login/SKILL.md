---
name: tea-prog-login
description: tea-saas 程序化登录（绕验证码）。UI 调试/接口测试需要登录态时使用；验证码识别不稳或 token 因重启失效时首选。
---

# 程序化登录（跳过验证码）

## 原理

后端 `AuthServiceImpl` **仅当 captchaKey 非空才校验验证码**。登录请求不带 captchaKey 即跳过——这是留给自动化测试的通路。

## 浏览器内登录（UI 调试用）

```js
// wmux browser eval 执行
fetch('/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'store01', password: 'Smoke2026x' })
}).then(function (r) { return r.json() }).then(function (j) {
  var d = j.data
  if (d && d.token) {
    localStorage.setItem('teasaas:token', d.token)
    localStorage.setItem('teasaas:token-name', 'Authorization')
    localStorage.setItem('teasaas:nickname', d.nickname || '')
    return 'LOGIN_OK'
  }
  return 'FAIL ' + j.message
})
```

账号任选：`brand01`(R2 品牌) / `fra01`(R6 加盟商) / `store01`(R7 店长)。写完直接 `wmux browser open <目标路由>` 即可，路由守卫会拉菜单注册动态路由。

## curl 登录（接口测试用）

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"store01","password":"Smoke2026x"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")

curl -s "http://localhost:8080/api/v1/..." -H "Authorization: $TOKEN"
```

## 关键细节

- **请求头 `Authorization: <token>`，不带 `Bearer` 前缀**（带了报"未登录或会话过期"）
- localStorage 键名前缀 `teasaas:`，共 3 个：token / token-name / nickname
- 后端重启不清 token（在 Redis）；**Redis 重启才清**
- 登录返回 data 字段：token/tokenName/userId/tenantId/nickname/userType/roles

## 各角色页面路由前缀（菜单驱动，来自 sys_menu 种子）

| 角色 | 前缀 | 页面 |
|---|---|---|
| R2 品牌端 | `/biz/` `/product/` `/supply/` `/franchise/` `/system/` | workspace/orders/members/coupons/... |
| R7 门店端 | `/store-term/` | st-dashboard/st-orders/st-pos/st-inventory/st-settle |

注意门店端是 `/store-term/st-*`，**不是** `/store/*`（直接开 /store/* 会 404）。
