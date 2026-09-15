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
