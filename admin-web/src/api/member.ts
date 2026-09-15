import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { MemMember, PointsConfig } from '@/types/member'

/** 会员接口（后端 MemberController /api/v1/members，F-MEM-01） */
export const memberApi = {
  /** 会员分页（keyword 模糊昵称/手机号） */
  page: (params: { page: number; size: number; keyword?: string }) =>
    http.get<PageResult<MemMember>>('/members', params),
}

/** 积分/储值规则接口（后端 MemberConfigController /api/v1/member-configs，F-MEM-02/03） */
export const memberConfigApi = {
  /** 规则查询（配置页回显） */
  points: () => http.get<PointsConfig>('/member-configs/points'),
  /** 规则保存 */
  savePoints: (data: PointsConfig) => http.put<void>('/member-configs/points', data),
}
