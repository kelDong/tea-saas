/** 订单模块类型（F-ORD-02，后端 OrdOrderController；金额一律分） */

/** 订单行（OrdOrder 实体直出） */
export interface OrdOrder {
  id: number
  orderNo: string
  storeId: number
  memberId: number | null
  /** 1小程序/2门店收银 */
  orderSource: number
  /** 1待支付/2已支付/3制作中/4待取餐/5已完成/6已取消/7已退款 */
  status: number
  totalAmount: number
  discountAmount: number | null
  payAmount: number
  couponId: number | null
  couponAmount: number | null
  pointsUsed: number | null
  pointsAmount: number | null
  balanceAmount: number | null
  /** 1微信/2储值/3现金 */
  payChannel: number | null
  takeCode: string | null
  payTime: string | null
  finishTime: string | null
  cancelReason: string | null
  remark: string | null
  createTime: string
}

/** 订单明细项（OrderDetailVO.OrderItemNode） */
export interface OrderItemNode {
  productId: number
  listingId: number
  productName: string
  specDesc: string | null
  price: number
  qty: number
  amount: number
}

/** 订单详情（OrderDetailVO，B/C 端共用） */
export interface OrderDetailVO {
  id: number
  orderNo: string
  storeId: number
  memberId: number | null
  orderSource: number
  status: number
  totalAmount: number
  discountAmount: number | null
  payAmount: number
  payChannel: number | null
  takeCode: string | null
  payTime: string | null
  finishTime: string | null
  cancelReason: string | null
  remark: string | null
  createTime: string
  items: OrderItemNode[]
}

/** POS 手工下单入参（F-ORD-03，order_source=2，顾客免登录，下单即扣库存） */
export interface PosOrderCreateDTO {
  storeId: number
  items: Array<{ listingId: number; qty: number; specOptionIds?: number[] }>
}

/** POS 下单返回 */
export interface PosOrderCreateVO {
  id: number
  orderNo: string
  takeCode: string
  /** 实付（分） */
  payAmount: number
}

/** POS 收款确认入参（注意：POS 端渠道编号 1现金/2微信Native/3储值，与订单表 payChannel 不同） */
export interface PosPayDTO {
  payChannel: number
  /** 实收（分），现金必传且≥应收 */
  receiveAmount?: number
  /** 会员ID：储值必填；现金/微信可选（传则绑会员并发放积分） */
  memberId?: number
}

/** POS 会员搜索结果（储值收款选会员，最小字段集） */
export interface PosMemberVO {
  id: number
  nickname: string | null
  phone: string | null
  /** 储值余额（分） */
  balance: number
}

/** POS 收款返回 */
export interface PosPayVO {
  orderId: number
  orderNo: string
  /** 应收（分） */
  payAmount: number
  /** 实收（分） */
  receiveAmount: number
  /** 找零（分），非现金为 0 */
  changeAmount: number
  paymentNo: string
}

/** POS 收款渠道（后端 PosPayDTO 编号） */
export const POS_PAY_CHANNEL_MAP: Record<number, string> = {
  1: '现金',
  2: '微信收款码',
  3: '储值',
}

/** 退款流水（PayRefund 实体直出，F-PAY-02 一期仅整单退） */
export interface PayRefund {
  id: number
  refundNo: string
  orderId: number
  /** 退款金额（分），整单退=实付 */
  amount: number
  /** 1微信原路/2退储值 */
  refundType: number
  /** 券不退还=0 */
  couponReturned: number
  reason: string | null
  /** 1处理中/2成功/3失败 */
  status: number
  refundTime: string | null
  createTime: string
}

/** 申请退款出参（后端 RefundVO，F-PAY-02） */
export interface RefundVO {
  refundNo: string
  orderId: number
  orderNo: string
  /** 原支付流水ID（储值单为空） */
  paymentId: number | null
  /** 退款金额（分），整单退=实付 */
  amount: number
  /** 1微信原路/2退储值 */
  refundType: number
  /** 券不退还=0 */
  couponReturned: number
  reason: string | null
  /** 1处理中/2成功/3失败 */
  status: number
  refundTime: string | null
  createTime: string
}

/**
 * 订单 WS 推送消息（/ws/orders，接口需求 8.3；仅 R7/R8 订阅名下门店）
 * 字段口径遵循 SafeLongSerializer：雪花 ID（orderId/storeId，19 位）超
 * JS 安全整数 → 字符串；tenantId（9001 等）/status/time 为小数值 → number
 */
export interface WsOrderMessage {
  type: 'NEW_ORDER' | 'STATUS_CHANGED'
  tenantId: number
  /** 雪花 ID，前端按不透明字符串透传（与 REST /stores 返回的 id 同口径） */
  storeId: string
  /** 雪花 ID，前端按不透明字符串透传 */
  orderId: string
  orderNo: string
  /** 1待支付/2已支付/3制作中/4待取餐/5已完成/6已取消/7已退款 */
  status: number
  takeCode: string | null
  /** 服务端毫秒时间戳 */
  time: number
}
