/**
 * 端到端冒烟公共库（docs/茶饮SaaS-测试计划.md §2/§5 口径）
 *
 * - Node ≥22（全局 fetch / WebSocket），零 npm 依赖
 * - 断言原则：动态查找 + 前后差值，禁止硬编码演示库状态值
 * - 环境变量：SMOKE_BASE（默认 http://localhost:8080）、SMOKE_TENANT（默认 9001）、SMOKE_PASSWORD（默认 Smoke2026x）
 */
export const BASE = process.env.SMOKE_BASE || 'http://localhost:8080'
export const WS_BASE = BASE.replace(/^http/, 'ws')
export const TENANT = Number(process.env.SMOKE_TENANT || 9001)
export const PASSWORD = process.env.SMOKE_PASSWORD || 'Smoke2026x'

// ---------- 断言收集 ----------
const results = []
export function check(name, ok, detail = '') {
  results.push({ name, ok: !!ok })
  console.log(`${ok ? 'PASS' : 'FAIL'} ${name}${detail !== '' && detail !== undefined && detail !== null ? ' — ' + detail : ''}`)
  return !!ok
}
export function skip(name, reason) {
  results.push({ name, ok: true, skipped: true })
  console.log(`SKIP ${name} — ${reason}`)
}
export function finish(scenario) {
  const failed = results.filter((r) => !r.ok)
  const skipped = results.filter((r) => r.skipped)
  console.log(`\n===== ${scenario}：${results.length - failed.length - skipped.length}/${results.length} PASS` +
    `${skipped.length ? `（SKIP ${skipped.length}）` : ''} =====`)
  process.exit(failed.length ? 1 : 0)
}

// ---------- HTTP ----------
/**
 * 统一请求：B 端传 {tokenName, token}（头名=登录响应 tokenName）；C 端传 {bearer}；
 * 公开接口只传 {tenantId}。返回 {status, text, json(原始 Result), data(解包)}。
 */
export async function api(method, path, { tokenName, token, bearer, tenantId } = {}, body) {
  const headers = { 'Content-Type': 'application/json' }
  if (tokenName && token) headers[tokenName] = token
  if (bearer) headers['Authorization'] = 'Bearer ' + bearer
  if (tenantId) headers['X-Tenant-Id'] = String(tenantId)
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 回调接口回纯文本 */ }
  const data = json && typeof json.code === 'number' ? json.data : undefined
  return { status: res.status, text, json, data }
}

// ---------- 登录 ----------
/** B 端登录（R2~R8），返回 {tokenName, token} 可直接展开为 api() 的鉴权参数 */
export async function loginB(username, password = PASSWORD) {
  const r = await api('POST', '/api/v1/auth/login', {}, { username, password })
  const d = r.data || {}
  return { tokenName: d.tokenName || 'Authorization', token: d.token, vo: d, res: r }
}

/** C 端登录（mock：code 即 openid），返回 {bearer, memberId} */
export async function loginC(openid) {
  const r = await api('POST', '/api/app/v1/auth/wx-login', { tenantId: TENANT }, { code: openid })
  return { bearer: r.data?.token, memberId: r.data?.memberId, vo: r.data, res: r }
}

/** 随机 openid（每次冒烟新建会员，不污染存量） */
export function randomOpenid() {
  return `smoke-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

// ---------- 数据查找 ----------
/** B 端取第一家门店 */
export async function pickStore(auth) {
  const r = await api('GET', '/api/v1/stores?page=1&size=100', auth)
  return r.data?.list?.[0] || null
}

/** 取门店第一个上架可售商品（status=1 且 sellable=1） */
export async function pickListing(auth, storeId) {
  const r = await api('GET', `/api/v1/listings?storeId=${storeId}`, auth)
  const list = r.data || []
  return list.find((l) => l.status === 1 && l.sellable === 1) || list[0] || null
}

/** 门店全部材料库存量合计（差值断言用；翻页取全） */
export async function stockSum(auth, storeId) {
  let sum = 0, page = 1
  for (;;) {
    const r = await api('GET', `/api/v1/inventories?storeId=${storeId}&page=${page}&size=200`, auth)
    const list = r.data?.list || []
    sum += list.reduce((acc, x) => acc + Number(x.stockQty || 0), 0)
    const total = r.data?.total || 0
    if (page * 200 >= total || list.length === 0) break
    page++
  }
  return sum
}

/** C 端会员可用积分 / 钱包余额快照 */
export async function memberSnapshot(bearer) {
  const r = await api('GET', '/api/app/v1/members/me', { bearer })
  return { points: r.data?.availablePoints ?? 0, balance: r.data?.walletBalance ?? 0 }
}

/** 本地日期 yyyy-MM-dd（对账/日结参数） */
export function today() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

/** 轮询直到条件满足（默认 3s / 300ms 间隔），超时返回最后一次值 */
export async function pollUntil(fn, { timeout = 3000, interval = 300 } = {}) {
  const start = Date.now()
  for (;;) {
    const v = await fn()
    if (v) return v
    if (Date.now() - start > timeout) return null
    await new Promise((r) => setTimeout(r, interval))
  }
}

/** POS 下单 + 现金收款（多个场景复用），返回 {order, pay} */
export async function posCashOrder(auth, storeId, listingId, qty, extraReceive = 0) {
  const created = await api('POST', '/api/v1/pos/orders', auth, {
    storeId, items: [{ listingId, qty }],
  })
  const order = created.data
  const paid = await api('POST', `/api/v1/pos/orders/${order?.id}/pay`, auth, {
    payChannel: 1, receiveAmount: Number(order?.payAmount || 0) + extraReceive,
  })
  return { order, pay: paid.data, createdRes: created, paidRes: paid }
}

/** mock 微信支付回调（纯文本 SUCCESS 应答）；返回是否成功 */
export async function mockWxCallback(paymentNo, amount) {
  const res = await fetch(`${BASE}/api/notify/wx-pay/callback`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      paymentNo, transactionId: 'MOCK' + paymentNo, resultCode: 'SUCCESS', amount,
    }),
  })
  const text = (await res.text()).trim()
  return { ok: text === 'SUCCESS', text, status: res.status }
}

/** 订单详情（B 端） */
export async function bOrderDetail(auth, orderId) {
  const r = await api('GET', `/api/v1/orders/${orderId}`, auth)
  return r.data
}

/** 简写：数值化（雪花 id 响应为字符串时比较用） */
export const N = (v) => Number(v)
