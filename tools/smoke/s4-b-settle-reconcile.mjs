/**
 * S4 · 对账与日结回流（测试计划 §4.4）
 * 自建 1 单微信支付 → reconcile 差值（订单/流水各 +1、diff 不增）→ 门店日结差值
 */
import { api, loginB, check, finish, pickStore, pickListing, today, bOrderDetail, N } from './lib/common.mjs'

const brand = await loginB('brand01')
check('登录 brand01(R2)', !!brand.token)
const store01 = await loginB('store01')
check('登录 store01(R7)', !!store01.token)
const store = await pickStore(store01)
const listing = await pickListing(store01, store.id)
check('门店/可售商品就绪', !!store && !!listing)
if (!store || !listing) finish('S4')

const date = today()

// ---------- before ----------
const recBefore = await api('GET', `/api/v1/reconcile?date=${date}`, brand)
check('对账查询（R2）', recBefore.json?.code === 0, `date=${date}`)
const rec0 = recBefore.data || {}
const dailyBefore = await api('GET', `/api/v1/reports/store/${store.id}/daily?date=${date}`, store01)
const daily0 = dailyBefore.data || {}

// ---------- 自建 1 单微信支付（POS Native，mock 即时落账写流水）----------
const created = await api('POST', '/api/v1/pos/orders', store01, {
  storeId: store.id, items: [{ listingId: listing.id, qty: 1 }],
})
const order = created.data
const pay = await api('POST', `/api/v1/pos/orders/${order.id}/pay`, store01, { payChannel: 2 })
check('微信单收款受理', pay.json?.code === 0, `orderNo=${pay.data?.orderNo}`)
const d = await bOrderDetail(store01, order.id)
check('微信单 status=2', N(d?.status) === 2, `status=${d?.status}`)

// ---------- after：对账差值 ----------
const recAfter = await api('GET', `/api/v1/reconcile?date=${date}`, brand)
const rec1 = recAfter.data || {}
check('对账 orderCount 差值 ≥1', N(rec1.orderCount) - N(rec0.orderCount) >= 1,
  `${rec0.orderCount} → ${rec1.orderCount}`)
check('对账 paymentCount 差值 ≥1', N(rec1.paymentCount) - N(rec0.paymentCount) >= 1,
  `${rec0.paymentCount} → ${rec1.paymentCount}`)
check('对账 diffCount 差值 =0（资金一致）', N(rec1.diffCount) - N(rec0.diffCount) === 0,
  `${rec0.diffCount} → ${rec1.diffCount}`)

// ---------- after：日结差值 ----------
const dailyAfter = await api('GET', `/api/v1/reports/store/${store.id}/daily?date=${date}`, store01)
const daily1 = dailyAfter.data || {}
check('日结 orderCount 差值 ≥1', N(daily1.orderCount) - N(daily0.orderCount) >= 1,
  `${daily0.orderCount} → ${daily1.orderCount}`)
check('日结 salesAmount 差值 ≥1', N(daily1.salesAmount) - N(daily0.salesAmount) >= 1,
  `${daily0.salesAmount} → ${daily1.salesAmount}`)

finish('S4')
