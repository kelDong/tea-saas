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
  3: '储值（未开放）',
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
