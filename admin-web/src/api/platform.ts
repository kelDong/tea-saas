import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { SysPackageVO, TenantCreateReq, TenantUpdateReq, TenantVO } from '@/types/platform'

/** 平台侧品牌接口（后端 TenantController /api/admin/v1/tenants，R1 专属，无租户隔离）
 *  url 以 /api/ 开头不拼接 baseURL（见 utils/request.ts） */
export const tenantApi = {
  /** 品牌列表（分页，keyword 模糊编码/名称） */
  page: (params: { page: number; size: number; keyword?: string }) =>
    http.get<PageResult<TenantVO>>('/api/admin/v1/tenants', params),
  /** 创建品牌（默认启用） */
  create: (data: TenantCreateReq) => http.post<number>('/api/admin/v1/tenants', data),
  /** 编辑品牌（tenantCode 不可改） */
  update: (id: number, data: TenantUpdateReq) => http.put<void>(`/api/admin/v1/tenants/${id}`, data),
  /** 启停（1启用/0停用；停用不强制下线该租户会话，见后端注释） */
  updateStatus: (id: number, status: number) =>
    http.patch<void>(`/api/admin/v1/tenants/${id}/status`, { status }),
}

/** 平台侧套餐接口（后端 SysPackageController /api/admin/v1/packages，R1 专属；套餐 CRUD 二期） */
export const packageApi = {
  /** 套餐列表（含停用，含功能开关 JSON） */
  list: () => http.get<SysPackageVO[]>('/api/admin/v1/packages'),
}
