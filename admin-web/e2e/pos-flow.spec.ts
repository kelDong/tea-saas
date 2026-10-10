import { expect, test } from '@playwright/test'

/**
 * S9 · admin-web UI 资金主干流（测试计划 §4.9）
 * 登录 store01(R7) → 收银台现金下单收款 → 接单页流转(接单→制作完成→核销取餐) → WS 连接态
 *
 * 环境前置：后端 8080（tea.pay.mock=true）+ Docker tea-mysql/tea-redis；
 * 租户 9001 有可售上架商品（与 API 套件同一前置，见 tools/smoke/README.md）。
 */
const USERNAME = 'store01'
const PASSWORD = 'Smoke2026x'

/** UI 表单登录；失败（如验证码拦截）则 fallback：API 登录 + localStorage 注入 token */
async function ensureLogin(page: import('@playwright/test').Page) {
  await page.goto('/login')
  await page.getByPlaceholder('用户名').fill(USERNAME)
  await page.getByPlaceholder('密码').fill(PASSWORD)
  await page.getByRole('button', { name: '登 录' }).click()
  try {
    await expect(page).not.toHaveURL(/\/login/, { timeout: 10_000 })
    return
  } catch {
    // fallback：后端登录拿 token 注入（守卫会自动拉菜单注册动态路由）
    const res = await page.request.post('/api/v1/auth/login', {
      data: { username: USERNAME, password: PASSWORD },
    })
    const d = (await res.json()).data
    expect(d?.token, 'fallback 登录失败').toBeTruthy()
    await page.goto('/login')
    await page.evaluate(([token, tokenName]) => {
      localStorage.setItem('teasaas:token', token)
      localStorage.setItem('teasaas:token-name', tokenName)
    }, [d.token as string, (d.tokenName as string) || 'Authorization'])
  }
}

test('S9 资金主干流：登录 → POS 现金收款 → 接单流转', async ({ page }) => {
  await ensureLogin(page)

  // ---------- 收银台：下单 + 现金收款 ----------
  await page.goto('/store-term/st-pos')
  const goodsCard = page.locator('.goods-card:not(.disabled)').first()
  await expect(goodsCard).toBeVisible({ timeout: 15_000 })
  await goodsCard.click()
  await expect(page.locator('.cart-row')).toHaveCount(1)

  await page.getByRole('button', { name: /下单收款/ }).click()
  const payDialog = page.locator('.el-dialog', { hasText: '收款：' }).first()
  await expect(payDialog).toBeVisible({ timeout: 10_000 })
  const orderNo = ((await payDialog.locator('.mono').first().textContent()) || '').trim()
  expect(orderNo, '收款弹窗应含订单号').toMatch(/^SO\d+$/)
  // 实收金额默认=应收（组件预填），直接确认收款
  await payDialog.getByRole('button', { name: '确认收款' }).click()
  await expect(page.getByText('收款完成')).toBeVisible({ timeout: 10_000 })

  // ---------- 接单页：流转到已完成 ----------
  await page.goto('/store-term/st-orders')
  await expect(page.locator('.el-table__row').first()).toBeVisible({ timeout: 15_000 })

  // WS 连接态（R7 握手成功 → open）
  await expect(page.locator('.ws-status')).toHaveText('实时推送中', { timeout: 10_000 })

  const row = page.locator('.el-table__row', { hasText: orderNo })
  await expect(row).toBeVisible()

  await row.getByRole('button', { name: '接单', exact: true }).click()
  await expect(row.getByRole('button', { name: '制作完成', exact: true })).toBeVisible({ timeout: 10_000 })
  await row.getByRole('button', { name: '制作完成', exact: true }).click()
  await expect(row.getByRole('button', { name: '核销取餐', exact: true })).toBeVisible({ timeout: 10_000 })
  await row.getByRole('button', { name: '核销取餐', exact: true }).click()
  await expect(row.locator('.el-tag', { hasText: '已完成' })).toBeVisible({ timeout: 10_000 })
})
