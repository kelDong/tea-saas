import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { MemberAssets, MemMember, PointsConfig, PointsLog, RechargeRecord } from '@/types/member'

/** 会员接口（后端 MemberController /api/v1/members，F-MEM-01；二期A补资产/流水） */
export const memberApi = {
  /** 会员分页（keyword 模糊昵称/手机号） */
  page: (params: { page: number; size: number; keyword?: string }) =>
    http.get<PageResult<MemMember>>('/members', params),
  /** 会员资产汇总（余额/累计充值消费/可用累计积分） */
  assets: (id: number) => http.get<MemberAssets>(`/members/${id}/assets`),
  /** 会员流水（type=points 积分明细 / wallet 储值充值记录） */
  logs: (id: number, params: { type: 'points' | 'wallet'; page: number; size: number }) =>
    http.get<PageResult<PointsLog | RechargeRecord>>(`/members/${id}/logs`, params),
}

/** 积分/储值规则接口（后端 MemberConfigController /api/v1/member-configs，F-MEM-02/03） */
export const memberConfigApi = {
  /** 规则查询（配置页回显） */
  points: () => http.get<PointsConfig>('/member-configs/points'),
  /** 规则保存 */
  savePoints: (data: PointsConfig) => http.put<void>('/member-configs/points', data),
}
