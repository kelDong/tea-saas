/** 供应链模块类型（F-SUP-01~04，后端 Sup* Controller） */

/** 供应商（SupSupplier 实体直出） */
export interface SupSupplier {
  id: number
  supplierCode: string
  supplierName: string
  contactName: string | null
  contactPhone: string | null
  paymentTerms: string | null
  /** 1启用/0停用 */
  status: number
  createTime: string
}

/** 供应商新增/编辑入参 */
export interface SupplierSaveDTO {
  supplierCode: string
  supplierName: string
  contactName?: string
  contactPhone?: string
  paymentTerms?: string
}

/** 供应商供货原料行（SupSupplierMaterial 实体直出） */
export interface SupSupplierMaterial {
  id: number
  supplierId: number
  materialId: number
  /** 参考供货价（分） */
  supplyPrice: number
  createTime: string
}

/** 供货原料绑 定入参（全量重绑） */
export interface SupplierMaterialBindDTO {
  items: Array<{ materialId: number; supplyPrice: number }>
}

/** 分级价格行（SupTierPrice 实体直出；levelCode A/B/C） */
export interface SupTierPrice {
  id: number
  materialId: number
  /** A/B/C */
  levelCode: string
  /** 单价（分） */
  price: number
  updateTime: string
}

/** 分级价格批量保存入参 */
export interface TierPriceBatchDTO {
  items: Array<{ materialId: number; levelCode: string; price: number }>
}

/** 采购单行（SupPurchaseOrder 实体直出）
 *  状态：1待确认/2已确认/3已发货/4已入库/5已取消 */
export interface SupPurchaseOrder {
  id: number
  orderNo: string
  storeId: number
  franchiseeId: number | null
  /** 计价等级快照（A/B/C） */
  levelCode: string
  status: number
  /** 总额（分） */
  totalAmount: number
  confirmTime: string | null
  shipTime: string | null
  receiveTime: string | null
  remark: string | null
  createTime: string
}

/** 采购单明细行（SupPurchaseOrderItem 实体直出） */
export interface SupPurchaseOrderItem {
  id: number
  orderId: number
  materialId: number
  qty: number
  /** 成交单价快照（分） */
  unitPrice: number
  /** 小计（分） */
  amount: number
}

/** 采购单详情（PurchaseOrderDetailVO） */
export interface PurchaseOrderDetailVO {
  order: SupPurchaseOrder
  items: SupPurchaseOrderItem[]
}

/** 采购单创建入参（F-SUP-03；等级计价快照由后端按门店加盟商等级取） */
export interface PurchaseOrderCreateDTO {
  storeId: number
  remark?: string
  items: Array<{ materialId: number; qty: number }>
}
