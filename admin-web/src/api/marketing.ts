import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { CouponTemplateDTO, MktCouponTemplate } from '@/types/marketing'

/** 优惠券接口（后端 CouponTemplateController /api/v1/coupon-templates，F-MKT-01） */
export const couponApi = {
  /** 券模板分页（status: 1启用/2停用） */
  page: (params: { page: number; size: number; status?: number }) =>
    http.get<PageResult<MktCouponTemplate>>('/coupon-templates', params),
  /** 创建券模板 */
  create: (data: CouponTemplateDTO) => http.post<MktCouponTemplate>('/coupon-templates', data),
  /** 编辑/停用（已领券不受影响） */
  update: (id: number, data: CouponTemplateDTO) => http.put<void>(`/coupon-templates/${id}`, data),
}
