/**
 * C 端小程序环境配置
 *
 * - baseUrl：后端网关地址（本机联调 localhost:8080；生产改为正式域名并配置合法域名）
 * - tenantId：品牌租户标识。品牌自营小程序一个 appid 对应一个品牌，
 *   公开接口（门店/商品/登录）需以 X-Tenant-Id 头携带（后端多租户注入约定）
 * - useMockPay：微信支付资质未下发（飞书 t100011）期间后端 tea.pay.mock=true，
 *   支付成功由本端「模拟支付」直连 mock 回调闭环；资质下发后置 false 走 wx.requestPayment
 */
export const config = {
  baseUrl: 'http://localhost:8080',
  tenantId: 9001,
  useMockPay: true,
};
