import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { OrderDetailVO, OrdOrder, PayRefund, RefundVO } from '@/types/order'

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
  /** B 端取消（待支付→已取消，回补库存；R2/R6/R7） */
  cancel: (id: number, reason?: string) => http.post<void>(`/orders/${id}/cancel`, { reason }),
}

/** 退款接口（后端 RefundController /api/v1/orders/{id}/refund + /api/v1/refunds，F-PAY-02） */
export const refundApi = {
  /** 申请退款（整单退=实付，回补库存；R2 全部 / R6/R7 名下门店） */
  apply: (orderId: number, reason?: string) =>
    http.post<RefundVO>(`/orders/${orderId}/refund`, { reason }),
  /** 退款分页（R2 全部 / R6/R7 名下门店，按 id 倒序） */
  page: (params: { page: number; size: number }) =>
    http.get<PageResult<PayRefund>>('/refunds', params),
}
