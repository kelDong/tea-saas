/**
 * S6 · C 端资产组合（测试计划 §4.6，全部自建可回收数据）
 * 充值 → 微信单攒积分 → 积分抵扣+储值支付 → 自建券模板 → 领券 →
 * 门槛负例 → 券+储值组合支付 → 停用模板清理
 */
import { api, loginB, loginC, randomOpenid, check, skip, finish, pickStore, pickListing, mockWxCallback, pollUntil, memberSnapshot, N } from './lib/common.mjs'

const COUPON_NAME = '冒烟临时券-勿用手动停用'

const brand = await loginB('brand01')
check('登录 brand01(R2)', !!brand.token)
const store01 = await loginB('store01')
const store = await pickStore(store01)
const listing = await pickListing(store01, store.id)
check('门店/可售商品就绪', !!store && !!listing)
if (!store || !listing) finish('S6')

// ---------- 1. 新会员充值 ----------
const c = await loginC(randomOpenid())
check('wx-login 新会员', c.res.json?.code === 0 && !!c.bearer)

let snap0 = await memberSnapshot(c.bearer)
const recharge = await api('POST', '/api/app/v1/members/me/wallet/recharge', { bearer: c.bearer }, { amount: 5000 })
let snap1 = await memberSnapshot(c.bearer)
check('充值 5000 即时到账（差值）', recharge.json?.code === 0 && N(snap1.balance) - N(snap0.balance) === 5000,
  `balance ${snap0.balance} → ${snap1.balance}`)
const wlogs = await api('GET', '/api/app/v1/members/me/wallet/logs?page=1&size=10', { bearer: c.bearer })
check('充值记录可查', (wlogs.data?.list || []).some((x) => N(x.amount) === 5000))

// ---------- 2. 微信单攒积分 ----------
const item = (qty) => ({ listingId: listing.id, qty })
const pv1 = await api('POST', '/api/app/v1/orders/preview', { bearer: c.bearer }, { storeId: store.id, items: [item(1)] })
const price = N(pv1.data?.payAmount)
check('preview 单价', price > 0, `1件=${price}分`)
const wxOrder = (await api('POST', '/api/app/v1/orders', { bearer: c.bearer },
  { storeId: store.id, items: [item(1)], payChannel: 1 })).data
const payment = (await api('POST', '/api/app/v1/payments/wx-jsapi', { bearer: c.bearer }, { orderId: wxOrder.id })).data
await mockWxCallback(payment.paymentNo, N(wxOrder.payAmount))
const snap2 = await memberSnapshot(c.bearer)
const earned = N(snap2.points) - N(snap1.points)
check('支付攒积分（差值>0）', earned > 0, `points ${snap1.points} → ${snap2.points}`)

// ---------- 3. 积分抵扣 + 储值即时支付 ----------
const usePts = Math.max(1, Math.min(50, Math.floor(earned / 2)))
const pvPts = await api('POST', '/api/app/v1/orders/preview', { bearer: c.bearer },
  { storeId: store.id, items: [item(1)], pointsUsed: usePts })
check('preview 积分抵扣试算', N(pvPts.data?.payAmount) === price - usePts,
  `payAmount=${pvPts.data?.payAmount}（${price}-${usePts}）`)
const ptsOrder = (await api('POST', '/api/app/v1/orders', { bearer: c.bearer },
  { storeId: store.id, items: [item(1)], pointsUsed: usePts, payChannel: 2 })).data
check('积分+储值单 payAmount', N(ptsOrder?.payAmount) === price - usePts, `payAmount=${ptsOrder?.payAmount}`)
const snap3 = await memberSnapshot(c.bearer)
check('储值精确扣款（差值=payAmount）', N(snap1.balance) - N(snap3.balance) === N(ptsOrder.payAmount),
  `balance ${snap1.balance} → ${snap3.balance}`)
const plogs = await api('GET', '/api/app/v1/members/me/points?page=1&size=20', { bearer: c.bearer })
check('积分明细含抵扣记录', (plogs.data?.list || []).some((x) => N(x.changePoints) === -usePts))

// ---------- 4. 券模板自建（find-or-create，跑完停用）----------
const pv3 = await api('POST', '/api/app/v1/orders/preview', { bearer: c.bearer }, { storeId: store.id, items: [item(3)] })
const three = N(pv3.data?.payAmount)
const threshold = three - Math.floor((three - price) / 2) // 恒满足 price < threshold ≤ three
const couponAmount = Math.max(1, Math.min(50, Math.floor(threshold / 2)))
const dto = {
  name: COUPON_NAME, type: 1, threshold, amount: couponAmount, totalQty: 100, perLimit: 5,
  validType: 2, validDays: 7, status: 1, scopeType: 1,
}
const tplList = await api('GET', '/api/v1/coupon-templates?page=1&size=50', brand)
const existed = (tplList.data?.list || []).find((x) => x.name === COUPON_NAME)
let templateId
if (existed) {
  const upd = await api('PUT', `/api/v1/coupon-templates/${existed.id}`, brand, dto)
  check('复用冒烟券模板（置1）', upd.json?.code === 0, `templateId=${existed.id} threshold=${threshold}`)
  templateId = existed.id
} else {
  const created = await api('POST', '/api/v1/coupon-templates', brand, dto)
  check('自建券模板', created.json?.code === 0 && !!created.data?.id,
    `templateId=${created.data?.id} threshold=${threshold} amount=${couponAmount}`)
  templateId = created.data?.id
}
if (!templateId) finish('S6')

// ---------- 5. 领券 + 门槛负例 ----------
const claim = await api('POST', `/api/app/v1/coupons/${templateId}/claim`, { bearer: c.bearer }, {})
const coupon = claim.data
check('领券成功', claim.json?.code === 0 && !!coupon?.id, `couponId=${coupon?.id}`)
const owned = await api('GET', '/api/app/v1/coupons?status=1&page=1&size=50', { bearer: c.bearer })
check('券包可见（status=1）', (owned.data?.list || []).some((x) => String(x.id) === String(coupon.id)))

const negPv = await api('POST', '/api/app/v1/orders/preview', { bearer: c.bearer },
  { storeId: store.id, items: [item(1)], couponId: coupon.id })
check('门槛不满足拒绝', negPv.json?.code !== 0, negPv.json?.message || '')

// ---------- 6. 券+储值组合支付 ----------
if (N(snap3.balance) < three + 500) {
  await api('POST', '/api/app/v1/members/me/wallet/recharge', { bearer: c.bearer }, { amount: 5000 })
}
const balBefore = (await memberSnapshot(c.bearer)).balance
const cpOrder = (await api('POST', '/api/app/v1/orders', { bearer: c.bearer },
  { storeId: store.id, items: [item(3)], couponId: coupon.id, payChannel: 2 })).data
check('券+储值单 payAmount=小计-面值', N(cpOrder?.payAmount) === three - couponAmount,
  `payAmount=${cpOrder?.payAmount}（${three}-${couponAmount}）`)
const snap4 = await memberSnapshot(c.bearer)
check('组合单储值精确扣款', N(balBefore) - N(snap4.balance) === N(cpOrder.payAmount),
  `balance ${balBefore} → ${snap4.balance}`)
const used = await api('GET', '/api/app/v1/coupons?status=2&page=1&size=50', { bearer: c.bearer })
check('券已核销（status=2）', (used.data?.list || []).some((x) => String(x.id) === String(coupon.id)))

// ---------- 7. 清理：停用模板 ----------
const disable = await api('PUT', `/api/v1/coupon-templates/${templateId}`, brand, { ...dto, status: 2 })
check('清理：券模板停用', disable.json?.code === 0)

finish('S6')
