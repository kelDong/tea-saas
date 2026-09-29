/** 营销模块类型（F-MKT-01，后端 CouponTemplateController）
 *  金额字段单位「分」；折扣率 0.01~0.99（0.90=9折） */

/** 券模板（MktCouponTemplate 实体直出） */
export interface MktCouponTemplate {
  id: number
  name: string
  /** 1满减/2折扣 */
  type: number
  /** 门槛(分)，null/0=无门槛 */
  threshold: number | null
  /** 满减额(分)，type=1 */
  amount: number | null
  /** 折扣率，type=2 */
  discount: number | null
  /** 发放总量，-1不限 */
  totalQty: number
  /** 每人限领 */
  perLimit: number
  /** 1固定时段/2领取后N天 */
  validType: number
  validStart: string | null
  validEnd: string | null
  validDays: number | null
  /** 1启用/2停用 */
  status: number
  createTime: string
}

/** 券模板创建/编辑入参 */
export interface CouponTemplateDTO {
  name: string
  type: number
  threshold?: number
  amount?: number
  discount?: number
  totalQty: number
  perLimit: number
  validType: number
  validStart?: string
  validEnd?: string
  validDays?: number
  status: number
  /** 适用范围：1全场(默认)/2指定商品 */
  scopeType?: number
  /** 指定商品ID列表，scopeType=2 必填非空 */
  productIds?: number[]
}

/** 券详情范围节点 */
export interface ScopeNode {
  productId: number
  productName: string
}

/** 券模板详情（模板字段 + 适用范围，后端 CouponTemplateDetailVO） */
export interface CouponTemplateDetailVO extends MktCouponTemplate {
  scopeType: number
  scope: ScopeNode[]
}
