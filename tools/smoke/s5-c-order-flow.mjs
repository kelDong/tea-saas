/**
 * S5 · C 端下单支付全链路（测试计划 §4.5，随机 openid 新会员）
 * wx-login → 公开接口负例 → preview → 下单 → wx-jsapi → mock 回调(纯文本) →
 * 详情 → reorder → 取消
 */
import { api, loginB, loginC, randomOpenid, check, finish, pickStore, pickListing, mockWxCallback, pollUntil, N, TENANT } from './lib/common.mjs'

// 后端 8080 的门店/商品用 store01 查（与 C 端同租户同店）
const store01 = await loginB('store01')
const store = await pickStore(store01)
const listing = await pickListing(store01, store.id)
check('门店/可售商品就绪', !!store && !!listing, `store=${store?.id} listing=${listing?.id}`)
if (!store || !listing) finish('S5')

// ---------- 登录新会员 ----------
const openid = randomOpenid()
const c = await loginC(openid)
check('wx-login 新会员', c.res.json?.code === 0 && !!c.bearer && c.vo?.isNew === true,
  `memberId=${c.memberId}`)

// ---------- 公开接口（无 token，仅 X-Tenant-Id）----------
const nearby = await api('GET', '/api/app/v1/stores/nearby?lng=116.397&lat=39.909', { tenantId: TENANT })
check('nearby 公开 200', nearby.status === 200 && (nearby.data?.length ?? 0) > 0, `${nearby.data?.length} 家`)
const cats = await api('GET', `/api/app/v1/stores/${store.id}/categories`, { tenantId: TENANT })
check('categories 公开 200', cats.status === 200 && cats.json?.code === 0, `${cats.data?.length} 个分类`)
const prods = await api('GET', `/api/app/v1/stores/${store.id}/products`, { tenantId: TENANT })
check('products 公开 200', prods.status === 200 && (prods.data?.length ?? 0) > 0, `${prods.data?.length} 个商品`)

// ---------- 试算 + 下单 ----------
const item = { listingId: listing.id, qty: 1 }
const preview = await api('POST', '/api/app/v1/orders/preview', { bearer: c.bearer },
  { storeId: store.id, items: [item] })
const subtotal = N(preview.data?.payAmount)
check('preview 试算', preview.json?.code === 0 && subtotal > 0, `payAmount=${subtotal}`)

const created = await api('POST', '/api/app/v1/orders', { bearer: c.bearer },
  { storeId: store.id, items: [item], payChannel: 1 })
const order = created.data
check('下单（微信）', created.json?.code === 0 && N(order?.payAmount) === subtotal,
  `orderId=${order?.id} payAmount=${order?.payAmount}`)

// ---------- wx-jsapi + mock 回调 ----------
const pay = await api('POST', '/api/app/v1/payments/wx-jsapi', { bearer: c.bearer }, { orderId: order.id })
const paymentNo = pay.data?.paymentNo
const pp = pay.data?.payParams || {}
check('wx-jsapi 出参', !!paymentNo && ['appId', 'timeStamp', 'nonceStr', 'package', 'signType', 'paySign'].every((k) => k in pp),
  `paymentNo=${paymentNo}`)

const cb = await mockWxCallback(paymentNo, subtotal)
check('mock 回调应答纯文本 SUCCESS', cb.ok, `status=${cb.status} text=${cb.text.slice(0, 40)}`)

const paid = await pollUntil(async () => {
  const r = await api('GET', `/api/app/v1/orders/${order.id}`, { bearer: c.bearer })
  return N(r.data?.status) === 2 ? r.data : null
})
check('支付成功订单 1→2', !!paid, `status=${paid?.status}`)

const detail = await api('GET', `/api/app/v1/orders/${order.id}`, { bearer: c.bearer })
check('详情金额一致', N(detail.data?.payAmount) === subtotal && N(detail.data?.status) === 2,
  `payAmount=${detail.data?.payAmount}`)

// ---------- 再来一单 ----------
const reorder = await api('POST', `/api/app/v1/orders/${order.id}/reorder`, { bearer: c.bearer }, {})
const ro = reorder.data
const roItem = ro?.items?.[0]
check('reorder 重建购物车', reorder.json?.code === 0 && (ro?.skipped?.length ?? 1) === 0 &&
  String(roItem?.listingId) === String(listing.id) && N(roItem?.qty) === 1 && N(roItem?.salePrice) > 0,
  `items=${ro?.items?.length} skipped=${ro?.skipped?.length}`)

const reCreated = await api('POST', '/api/app/v1/orders', { bearer: c.bearer }, {
  storeId: store.id, items: [{ listingId: roItem.listingId, qty: N(roItem.qty) }], payChannel: 1,
})
check('reorder 产物可下单', reCreated.json?.code === 0, `orderId=${reCreated.data?.id}`)

// ---------- 取消（待支付）----------
const cancel = await api('POST', `/api/app/v1/orders/${reCreated.data.id}/cancel`, { bearer: c.bearer }, {})
const canceled = await api('GET', `/api/app/v1/orders/${reCreated.data.id}`, { bearer: c.bearer })
check('待支付单取消 →6', cancel.json?.code === 0 && N(canceled.data?.status) === 6,
  `status=${canceled.data?.status}`)

finish('S5')
