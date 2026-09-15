import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  FraFranchisee,
  FraStore,
  FranchiseeAccountReq,
  FranchiseeCreateReq,
  FranchiseeLevelReq,
  FranchiseeStatusReq,
  FranchiseeUpdateReq,
  StoreBusinessStatusReq,
  StoreSaveReq,
} from '@/types/franchise'

/** 门店接口（后端 FraStoreController /api/v1/stores，F-FRA-02） */
export const franchiseApi = {
  /** 门店分页 */
  storePage: (params: { page: number; size: number; keyword?: string }) =>
    http.get<PageResult<FraStore>>('/stores', params),
  /** 门店详情 */
  storeDetail: (id: number) => http.get<FraStore>(`/stores/${id}`),
  /** 新建门店 */
  storeCreate: (data: StoreSaveReq) => http.post<number>('/stores', data),
  /** 编辑门店 */
  storeUpdate: (id: number, data: StoreSaveReq) => http.put<void>(`/stores/${id}`, data),
  /** 营业状态切换（1营业/0打烊） */
  storeBusinessStatus: (id: number, data: StoreBusinessStatusReq) =>
    http.patch<void>(`/stores/${id}/business-status`, data),
}

/** 加盟商接口（后端 FraFranchiseeController /api/v1/franchisees，F-FRA-01） */
export const franchiseeApi = {
  /** 加盟商分页（keyword 模糊名称/编码） */
  page: (params: { page: number; size: number; keyword?: string }) =>
    http.get<PageResult<FraFranchisee>>('/franchisees', params),
  /** 新增加盟商（缺省 C 级） */
  create: (data: FranchiseeCreateReq) => http.post<number>('/franchisees', data),
  /** 编辑加盟商 */
  update: (id: number, data: FranchiseeUpdateReq) => http.put<void>(`/franchisees/${id}`, data),
  /** 调整等级（采购计价联动） */
  level: (id: number, data: FranchiseeLevelReq) => http.put<void>(`/franchisees/${id}/level`, data),
  /** 开通账号（返回 userId） */
  account: (id: number, data: FranchiseeAccountReq) => http.post<number>(`/franchisees/${id}/account`, data),
  /** 合作状态（1合作中/0已终止） */
  status: (id: number, data: FranchiseeStatusReq) => http.patch<void>(`/franchisees/${id}/status`, data),
}
