import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { FraStore } from '@/types/franchise'

/** 加盟/门店接口（后端 FraStoreController /api/v1/stores，F-FRA-02）
 *  F4 加盟页扩展其余端点；F1 仅取门店下拉 */
export const franchiseApi = {
  storePage: (params: { page: number; size: number; keyword?: string }) =>
    http.get<PageResult<FraStore>>('/stores', params),
}
