/**
 * S7 · /ws/orders 实时推送（测试计划 §4.7，M17 ws-smoke.mjs 收编增强）
 * 握手/心跳 → POS 收款 NEW_ORDER → 接单/制作 STATUS_CHANGED → 退款(7) →
 * 负例：坏 token / 非门店角色(R6)
 */
import { api, loginB, check, finish, pickStore, pickListing, posCashOrder, WS_BASE, N } from './lib/common.mjs'

const auth = await loginB('store01')
check('登录 store01(R7)', !!auth.token)
const store = await pickStore(auth)
const listing = await pickListing(auth, store.id)
check('门店/可售商品就绪', !!store && !!listing)
if (!store || !listing) finish('S7')

// ---------- 握手 + 心跳 ----------
const messages = []
const ws = new WebSocket(`${WS_BASE}/ws/orders?token=${encodeURIComponent(auth.token)}`)
let opened = false
await new Promise((resolve) => {
  const t = setTimeout(resolve, 5000)
  ws.onopen = () => { opened = true; clearTimeout(t); resolve() }
  ws.onerror = () => { clearTimeout(t); resolve() }
  ws.onclose = () => { clearTimeout(t); resolve() }
})
check('WS 握手（query token）', opened)
if (!opened) finish('S7')

let pongAt = null
ws.onmessage = (ev) => {
  if (ev.data === 'PONG') { pongAt = Date.now(); return }
  messages.push(JSON.parse(ev.data))
  console.log('  WS <-', ev.data)
}
ws.onclose = () => {}
ws.send('PING')
await new Promise((r) => setTimeout(r, 800))
check('心跳 PING/PONG', pongAt !== null)

const waitMsg = async (type, orderId, status) => {
  return await (async () => {
    const deadline = Date.now() + 3000
    for (;;) {
      const hit = messages.find((m) => m.type === type && String(m.orderId) === String(orderId) &&
        (status === undefined || N(m.status) === status))
      if (hit) return hit
      if (Date.now() > deadline) return null
      await new Promise((r) => setTimeout(r, 200))
    }
  })()
}

// ---------- 业务触发 → 推送断言 ----------
const { order } = await posCashOrder(auth, store.id, listing.id, 1)
check('POS 现金下单+收款', !!order?.id, `orderId=${order.id}`)
const m1 = await waitMsg('NEW_ORDER', order.id, 2)
check('WS 推送 NEW_ORDER(status=2)', !!m1, m1 ? `storeId=${m1.storeId} takeCode=${m1.takeCode}` : '未收到')

await api('POST', `/api/v1/orders/${order.id}/accept`, auth, {})
const m2 = await waitMsg('STATUS_CHANGED', order.id, 3)
check('接单 → STATUS_CHANGED(3)', !!m2)

await api('POST', `/api/v1/orders/${order.id}/make`, auth, {})
const m3 = await waitMsg('STATUS_CHANGED', order.id, 4)
check('制作 → STATUS_CHANGED(4)', !!m3)

await api('POST', `/api/v1/orders/${order.id}/refund`, auth, { reason: '冒烟退款-WS' })
const m4 = await waitMsg('STATUS_CHANGED', order.id, 7)
check('退款 → STATUS_CHANGED(7)', !!m4)

ws.close()

// ---------- 负例：坏 token ----------
const bad = new WebSocket(`${WS_BASE}/ws/orders?token=bad-token-smoke`)
let badOpened = false
await new Promise((resolve) => {
  const t = setTimeout(resolve, 3000)
  bad.onopen = () => { badOpened = true; clearTimeout(t); resolve() }
  bad.onerror = () => { clearTimeout(t); resolve() }
  bad.onclose = () => { clearTimeout(t); resolve() }
})
check('负例：坏 token 握手拒绝', !badOpened)
try { bad.close() } catch {}

// ---------- 负例：非门店角色（fra01 R6 仅 403）----------
const fra = await loginB('fra01')
const r6 = new WebSocket(`${WS_BASE}/ws/orders?token=${encodeURIComponent(fra.token)}`)
let r6Opened = false
await new Promise((resolve) => {
  const t = setTimeout(resolve, 3000)
  r6.onopen = () => { r6Opened = true; clearTimeout(t); resolve() }
  r6.onerror = () => { clearTimeout(t); resolve() }
  r6.onclose = () => { clearTimeout(t); resolve() }
})
check('负例：R6 握手拒绝（仅 R7/R8）', !r6Opened)
try { r6.close() } catch {}

finish('S7')
