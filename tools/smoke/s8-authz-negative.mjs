/**
 * S8 · 权限与租户负例矩阵（测试计划 §4.8）
 * 自建 smoke-r8(R8 店员) → 退款 403；C 端无/伪 token 401；越权查他人订单；
 * 不存在订单 30101；清理停用账号
 */
import { api, BASE, loginB, loginC, randomOpenid, check, finish, pickStore, pickListing, posCashOrder, N, PASSWORD } from './lib/common.mjs'

const brand = await loginB('brand01')
check('登录 brand01(R2)', !!brand.token)
const store01 = await loginB('store01')
const store = await pickStore(store01)
const listing = await pickListing(store01, store.id)
check('门店/可售商品就绪', !!store && !!listing)
if (!store || !listing) finish('S8')

// ---------- 自建/复能 smoke-r8（R8 角色种子 id=8）----------
const list = await api('GET', '/api/v1/users?page=1&size=100&keyword=smoke-r8', brand)
let r8User = (list.data?.list || []).find((u) => u.username === 'smoke-r8')
if (r8User) {
  const upd = await api('PUT', `/api/v1/users/${r8User.id}`, brand,
    { nickname: '冒烟R8店员', status: 1, roleIds: [8], storeIds: [Number(store.id)] })
  check('复用 smoke-r8（置启用）', upd.json?.code === 0, `userId=${r8User.id}`)
} else {
  const created = await api('POST', '/api/v1/users', brand, {
    username: 'smoke-r8', password: PASSWORD, nickname: '冒烟R8店员',
    roleIds: [8], storeIds: [Number(store.id)],
  })
  check('创建 smoke-r8(R8)', created.json?.code === 0 && !!created.data, `userId=${created.data}`)
  r8User = { id: created.data }
}
if (!r8User?.id) finish('S8')

// ---------- R8 退款被拒 ----------
const r8 = await loginB('smoke-r8')
check('smoke-r8 登录', !!r8.token)
const { order } = await posCashOrder(store01, store.id, listing.id, 1)
check('POS 现金单就绪', !!order?.id, `orderId=${order.id}`)
const refundTry = await api('POST', `/api/v1/orders/${order.id}/refund`, r8, { reason: 'R8尝试退款' })
check('负例：R8 退款被拒', refundTry.json?.code === 3 && /退款/.test(refundTry.json?.message || ''),
  `code=${refundTry.json?.code} msg=${refundTry.json?.message}`)

// ---------- C 端鉴权负例 ----------
const noToken = await fetch(`${BASE}/api/app/v1/orders/${order.id}`)
check('负例：无 token 401', noToken.status === 401)
const fake = await fetch(`${BASE}/api/app/v1/orders/${order.id}`, {
  headers: { Authorization: 'Bearer fake-token-smoke' },
})
check('负例：伪 token 401', fake.status === 401)

// 会员 B 查会员 A（下单人）订单 → 非本人拒绝
const memberA = await loginC(randomOpenid())
const aOrder = (await api('POST', '/api/app/v1/orders', { bearer: memberA.bearer },
  { storeId: store.id, items: [{ listingId: listing.id, qty: 1 }], payChannel: 1 })).data
const memberB = await loginC(randomOpenid())
const cross = await api('GET', `/api/app/v1/orders/${aOrder.id}`, { bearer: memberB.bearer })
check('负例：查他人订单被拒', cross.json?.code === 3, `code=${cross.json?.code} msg=${cross.json?.message}`)

// ---------- 不存在订单（租户插件改写后查无 → 30101；跨租户等价口径见测试计划 §4.8）----------
// id 取雪花形态但小于 Long.MAX（9.3e18 会溢出 → 999999 而非业务码）
const ghost = await api('GET', '/api/v1/orders/9100000000000000001', store01)
check('负例：不存在订单 30101', ghost.json?.code === 30101, `code=${ghost.json?.code} msg=${ghost.json?.message}`)

// ---------- 清理：停用 smoke-r8 ----------
const disable = await api('PUT', `/api/v1/users/${r8User.id}`, brand,
  { nickname: '冒烟R8店员', status: 0, roleIds: [8], storeIds: [Number(store.id)] })
check('清理：smoke-r8 停用', disable.json?.code === 0)

finish('S8')
