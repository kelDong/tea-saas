/**
 * S1 · B 端 POS 现金全流程（测试计划 §4.1）
 * 下单 → 现金收款(找零) → 接单 → 制作 → 核销 → 库存总扣减
 */
import { api, loginB, check, skip, finish, pickStore, pickListing, stockSum, posCashOrder, bOrderDetail, N } from './lib/common.mjs'

const auth = await loginB('store01')
check('登录 store01(R7)', !!auth.token)

const store = await pickStore(auth)
check('门店列表取第一家', !!store, store ? `storeId=${store.id} ${store.storeName}` : '无门店')
if (!store) finish('S1')

const listing = await pickListing(auth, store.id)
check('上架可售商品', !!listing, listing ? `listingId=${listing.id} ${listing.productName}` : '无可售商品（环境前置不满足）')
if (!listing) finish('S1')

const before = await stockSum(auth, store.id)

const { order, pay, paidRes } = await posCashOrder(auth, store.id, listing.id, 2, 10000)
check('POS 下单 2 件', !!order?.id, `orderId=${order?.id} payAmount=${order?.payAmount}`)
check('现金收款订单 1→2', paidRes.json?.code === 0, `orderNo=${pay?.orderNo}`)
check('找零 changeAmount=10000', N(pay?.changeAmount) === 10000, `changeAmount=${pay?.changeAmount}`)

let d = await bOrderDetail(auth, order.id)
check('支付后 status=2', N(d?.status) === 2, `status=${d?.status}`)

await api('POST', `/api/v1/orders/${order.id}/accept`, auth, {})
d = await bOrderDetail(auth, order.id)
check('接单 2→3', N(d?.status) === 3, `status=${d?.status}`)

await api('POST', `/api/v1/orders/${order.id}/make`, auth, {})
d = await bOrderDetail(auth, order.id)
check('制作 3→4', N(d?.status) === 4, `status=${d?.status}`)

await api('POST', `/api/v1/orders/${order.id}/finish`, auth, {})
d = await bOrderDetail(auth, order.id)
check('核销 4→5（finish）', N(d?.status) === 5, `status=${d?.status}`)

const after = await stockSum(auth, store.id)
check('库存总扣减 > 0', after < before, `SUM ${before} → ${after}（Δ${after - before}）`)

finish('S1')
