import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  InventorySummaryVO,
  InvStockLog,
  InvStoreMaterialStock,
  StockAdjustDTO,
  StockWarningVO,
} from '@/types/inventory'

/** 库存接口（后端 InventoryController /api/v1/inventories，F-INV-01~03） */
export const inventoryApi = {
  /** 库存台账（分页） */
  page: (params: { page: number; size: number; storeId?: number; materialId?: number }) =>
    http.get<PageResult<InvStoreMaterialStock>>('/inventories', params),
  /** 库存流水（分页） */
  logs: (storeId: number, materialId: number, params: { page: number; size: number }) =>
    http.get<PageResult<InvStockLog>>(`/inventories/${storeId}/${materialId}/logs`, params),
  /** 盘点修正（实盘覆盖） */
  adjust: (data: StockAdjustDTO) => http.post<void>('/inventories/adjust', data),
  /** 库存预警 */
  warnings: (storeId?: number) => http.get<StockWarningVO[]>('/inventories/warnings', { storeId }),
  /** 库存总览（全租户按原料聚合） */
  summary: () => http.get<InventorySummaryVO[]>('/inventories/summary'),
}
