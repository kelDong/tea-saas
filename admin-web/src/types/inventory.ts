/** 库存模块类型（F-INV-01/03，后端 InventoryController） */

/** 库存台账行（InvStoreMaterialStock 实体直出） */
export interface InvStoreMaterialStock {
  id: number
  storeId: number
  materialId: number
  /** 库存量（非负） */
  stockQty: number
  /** 乐观锁版本 */
  version: number
  updateTime: string
}

/** 库存流水行（InvStockLog 实体直出）
 *  bizType：1下单扣/2超时回补/3退款回补/4采购入库/5盘点 */
export interface InvStockLog {
  id: number
  storeId: number
  materialId: number
  /** 入正出负 */
  changeQty: number
  beforeQty: number
  afterQty: number
  bizType: number
  bizNo: string | null
  remark: string | null
  createTime: string
}

/** 盘点修正入参（F-INV-02；实盘覆盖+version+1+流水留痕） */
export interface StockAdjustDTO {
  storeId: number
  materialId: number
  actualQty: number
  remark?: string
}

/** 库存预警行（StockWarningVO，F-INV-03） */
export interface StockWarningVO {
  storeId: number
  materialId: number
  materialCode: string
  materialName: string
  unit: string
  stockQty: number
  warnThreshold: number
  /** 缺口（阈值-库存） */
  gap: number
}

/** 库存总览行（InventorySummaryVO） */
export interface InventorySummaryVO {
  materialId: number
  materialCode: string
  materialName: string
  unit: string
  totalQty: number
  storeCount: number
}

export const STOCK_BIZ_TYPE_MAP: Record<number, string> = {
  1: '下单扣减',
  2: '超时回补',
  3: '退款回补',
  4: '采购入库',
  5: '盘点修正',
}
