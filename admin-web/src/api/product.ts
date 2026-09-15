import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  CategorySaveDTO,
  ListingCreateDTO,
  ListingVO,
  PrdCategory,
  PrdProduct,
  ProductDetailVO,
  ProductSaveDTO,
  SellableCheckVO,
} from '@/types/product'

/** 商品分类接口（后端 PrdCategoryController /api/v1/categories） */
export const categoryApi = {
  list: () => http.get<PrdCategory[]>('/categories'),
  create: (data: CategorySaveDTO) => http.post<number>('/categories', data),
  update: (id: number, data: CategorySaveDTO) => http.put<void>(`/categories/${id}`, data),
}

/** 产品库接口（后端 PrdProductController /api/v1/products，F-PRD-01） */
export const productApi = {
  page: (params: { page: number; size: number; categoryId?: number; publishStatus?: number; keyword?: string }) =>
    http.get<PageResult<PrdProduct>>('/products', params),
  detail: (id: number) => http.get<ProductDetailVO>(`/products/${id}`),
  create: (data: ProductSaveDTO) => http.post<number>('/products', data),
  update: (id: number, data: ProductSaveDTO) => http.put<void>(`/products/${id}`, data),
  /** 调整建议售价（分） */
  adjustPrice: (id: number, price: number) => http.patch<void>(`/products/${id}/price`, { price }),
  /** 下架（发布→草稿） */
  off: (id: number) => http.patch<void>(`/products/${id}/off`),
}

/** 上架接口（后端 PrdListingController /api/v1/listings，F-PRD-02/03） */
export const listingApi = {
  /** 可上架商品（已发布且未在本店上架） */
  availableProducts: (params: { page: number; size: number; keyword?: string }) =>
    http.get<PageResult<PrdProduct>>('/listings/available-products', params),
  /** 本店上架列表 */
  listByStore: (storeId: number) => http.get<ListingVO[]>('/listings', { storeId }),
  create: (data: ListingCreateDTO) => http.post<number>('/listings', data),
  /** 调整本店售价（不得高于总部建议价） */
  adjustPrice: (id: number, price: number) => http.patch<void>(`/listings/${id}/price`, { price }),
  off: (id: number) => http.patch<void>(`/listings/${id}/off`),
  /** 批量可售校验（缺料明细） */
  checkSellable: (listingIds: number[]) => http.post<SellableCheckVO[]>('/listings/check-sellable', { listingIds }),
}
