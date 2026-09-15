/** 订单枚举展示映射（契约 8.x；code 数字为准，中文仅展示） */

const ORDER_STATUS_LIST: Array<{ value: number; label: string; tag: 'warning' | 'primary' | 'success' | 'info' | 'danger' }> = [
  { value: 1, label: '待支付', tag: 'warning' },
  { value: 2, label: '已支付', tag: 'primary' },
  { value: 3, label: '制作中', tag: 'primary' },
  { value: 4, label: '待取餐', tag: 'warning' },
  { value: 5, label: '已完成', tag: 'success' },
  { value: 6, label: '已取消', tag: 'info' },
  { value: 7, label: '已退款', tag: 'danger' },
]

export const ORDER_STATUS_MAP = new Map(ORDER_STATUS_LIST.map((item) => [item.value, item]))

export const ORDER_STATUS_OPTIONS = ORDER_STATUS_LIST.map(({ value, label }) => ({ value, label }))

export const PAY_CHANNEL_MAP: Record<number, string> = {
  1: '微信',
  2: '储值',
  3: '现金',
}

export const ORDER_SOURCE_MAP: Record<number, string> = {
  1: '小程序',
  2: '门店收银',
}
