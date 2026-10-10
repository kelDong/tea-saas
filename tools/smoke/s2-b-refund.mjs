/**
 * S2 · B 端退款闭环（测试计划 §4.2）
 * 现金单退款 + 微信Native单退款（mock 即时落账）→ 退款列表可见
 */
import { api, loginB, check, finish, pickStore, pickListing, posCashOrder, bOrderDetail, N } from './lib/common.mjs'

const store01 = await loginB('store01')
check('登录 store01(R7)', !!store01.token)
const brand = await loginB('brand01')
check('登录 brand01(R2)', !!brand.token)

const store = await pickStore(store01)
const listing = await pickListing(store01, store.id)
check('门店/可售商品就绪', !!store && !!listing, `store=${store?.id} listing=${listing?.id}`)
if (!store || !listing) finish('S2')

// ---------- 现金单退款 ----------
const { order: cashOrder, pay: cashPay } = await posCashOrder(store01, store.id, listing.id, 1)
check('现金单下单+收款', !!cashPay?.orderNo, `orderNo=${cashPay?.orderNo}`)

const refund1 = await api('POST', `/api/v1/orders/${cashOrder.id}/refund`, store01, { reason: '冒烟退款-现金' })
check('现金单退款受理', refund1.json?.code === 0, `refundNo=${refund1.data?.refundNo}`)
let d = await bOrderDetail(store01, cashOrder.id)
check('现金单订单 →7 已退款', N(d?.status) === 7, `status=${d?.status}`)

// ---------- 微信 Native 单退款（mock 即时落账）----------
const wxCreate = await api('POST', '/api/v1/pos/orders', store01, {
  storeId: store.id, items: [{ listingId: listing.id, qty: 1 }],
})
const wxOrder = wxCreate.data
const wxPay = await api('POST', `/api/v1/pos/orders/${wxOrder.id}/pay`, store01, { payChannel: 2 })
check('微信Native收款受理', wxPay.json?.code === 0, `paymentNo=${wxPay.data?.paymentNo}`)
d = await bOrderDetail(store01, wxOrder.id)
check('微信单 mock 即时 status=2', N(d?.status) === 2, `status=${d?.status}`)

const refund2 = await api('POST', `/api/v1/orders/${wxOrder.id}/refund`, store01, { reason: '冒烟退款-微信' })
check('微信单退款受理', refund2.json?.code === 0)
d = await bOrderDetail(store01, wxOrder.id)
check('微信单订单 →7', N(d?.status) === 7, `status=${d?.status}`)

// ---------- 退款列表（brand01 R2 全租户可见；PayRefund 出参为 orderId 雪花字符串）----------
const refunds = await api('GET', '/api/v1/refunds?page=1&size=50', brand)
const ids = (refunds.data?.list || []).map((x) => String(x.orderId))
check('退款列表含两笔', ids.includes(String(cashOrder.id)) && ids.includes(String(wxOrder.id)),
  `列表 ${ids.length} 条`)

finish('S2')
