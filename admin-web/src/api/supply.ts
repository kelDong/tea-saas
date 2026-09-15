import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  PurchaseOrderCreateDTO,
  PurchaseOrderDetailVO,
  SupPurchaseOrder,
  SupSupplier,
  SupSupplierMaterial,
  SupplierMaterialBindDTO,
  SupplierSaveDTO,
  SupTierPrice,
  TierPriceBatchDTO,
} from '@/types/supply'

/** 供应商接口（后端 SupSupplierController /api/v1/suppliers，F-SUP-01） */
export const supplierApi = {
  page: (params: { page: number; size: number; keyword?: string }) =>
    http.get<PageResult<SupSupplier>>('/suppliers', params),
  /** 供应商供货原料清单 */
  materials: (id: number) => http.get<SupSupplierMaterial[]>(`/suppliers/${id}/materials`),
  create: (data: SupplierSaveDTO) => http.post<number>('/suppliers', data),
  update: (id: number, data: SupplierSaveDTO) => http.put<void>(`/suppliers/${id}`, data),
  /** 供货原料绑定（全量重绑） */
  bindMaterials: (id: number, data: SupplierMaterialBindDTO) => http.put<void>(`/suppliers/${id}/materials`, data),
  updateStatus: (id: number, status: number) => http.patch<void>(`/suppliers/${id}/status`, { status }),
}

/** 分级价格接口（后端 SupTierPriceController /api/v1/tier-prices，F-SUP-02） */
export const tierPriceApi = {
  page: (params: { page: number; size: number; materialId?: number; level?: string }) =>
    http.get<PageResult<SupTierPrice>>('/tier-prices', params),
  /** 批量保存 */
  batchSave: (data: TierPriceBatchDTO) => http.put<void>('/tier-prices/batch', data),
}

/** 采购单接口（后端 SupPurchaseOrderController /api/v1/purchase-orders，F-SUP-03/04） */
export const purchaseApi = {
  page: (params: { page: number; size: number; status?: number; storeId?: number }) =>
    http.get<PageResult<SupPurchaseOrder>>('/purchase-orders', params),
  detail: (id: number) => http.get<PurchaseOrderDetailVO>(`/purchase-orders/${id}`),
  create: (data: PurchaseOrderCreateDTO) => http.post<number>('/purchase-orders', data),
  /** 确认计价（1→2） */
  confirm: (id: number) => http.post<void>(`/purchase-orders/${id}/confirm`),
  /** 发货（2→3） */
  ship: (id: number) => http.post<void>(`/purchase-orders/${id}/ship`),
  /** 确认入库（3→4，联动库存 inbound） */
  receive: (id: number) => http.post<void>(`/purchase-orders/${id}/receive`),
  /** 取消（1/2→5） */
  cancel: (id: number) => http.post<void>(`/purchase-orders/${id}/cancel`),
}
