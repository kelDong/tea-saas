import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { OrderDetailVO, OrdOrder } from '@/types/order'

/** B 端订单接口（后端 OrdOrderController /api/v1/orders，F-ORD-02/03） */
export const orderApi = {
  page: (params: { page: number; size: number; storeId?: number; status?: number }) =>
    http.get<PageResult<OrdOrder>>('/orders', params),
  detail: (id: number) => http.get<OrderDetailVO>(`/orders/${id}`),
  /** 接单（已支付→制作中） */
  accept: (id: number) => http.post<void>(`/orders/${id}/accept`),
  /** 标记制作完成（制作中→待取餐） */
  make: (id: number) => http.post<void>(`/orders/${id}/make`),
  /** 核销取餐（待取餐→已完成） */
  finish: (id: number) => http.post<void>(`/orders/${id}/finish`),
}
