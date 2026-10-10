/**
 * S3 · B 端待支付取消 + 库存精确回补（测试计划 §4.3）
 */
import { api, loginB, check, finish, pickStore, pickListing, stockSum, bOrderDetail, N } from './lib/common.mjs'

const auth = await loginB('store01')
check('登录 store01(R7)', !!auth.token)
const store = await pickStore(auth)
const listing = await pickListing(auth, store.id)
check('门店/可售商品就绪', !!store && !!listing)
if (!store || !listing) finish('S3')

const before = await stockSum(auth, store.id)

const created = await api('POST', '/api/v1/pos/orders', auth, {
  storeId: store.id, items: [{ listingId: listing.id, qty: 2 }],
})
const order = created.data
check('POS 下单（不支付）', !!order?.id, `orderId=${order.id} payAmount=${order.payAmount}`)

const cancel = await api('POST', `/api/v1/orders/${order.id}/cancel`, auth, {})
check('取消受理', cancel.json?.code === 0)

const d = await bOrderDetail(auth, order.id)
check('订单 →6 已取消', N(d?.status) === 6, `status=${d?.status}`)

const after = await stockSum(auth, store.id)
check('库存回补精确归零', after === before, `SUM ${before} → ${after}`)

finish('S3')
