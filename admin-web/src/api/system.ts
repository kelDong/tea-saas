import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { SysOperationLog, SysRole, SysUserCreateReq, SysUserUpdateReq, SysUserVO } from '@/types/system'

/** 用户管理接口（后端 SysUserController /api/v1/users，F-SYS-02） */
export const sysUserApi = {
  page: (params: { page: number; size: number; keyword?: string }) =>
    http.get<PageResult<SysUserVO>>('/users', params),
  create: (data: SysUserCreateReq) => http.post<number>('/users', data),
  update: (id: number, data: SysUserUpdateReq) => http.put<void>(`/users/${id}`, data),
  /** 重置密码（重置后该账号强制下线） */
  resetPassword: (id: number, newPassword: string) =>
    http.patch<void>(`/users/${id}/password`, { newPassword }),
}

/** 角色接口（后端 SysRoleController /api/v1/roles，F-SYS-02） */
export const sysRoleApi = {
  list: () => http.get<SysRole[]>('/roles'),
  /** 菜单授权（全量重绑） */
  grantMenus: (id: number, menuIds: number[]) => http.put<void>(`/roles/${id}/menus`, menuIds),
}

/** 操作日志接口（后端 SysOperationLogController /api/v1/operation-logs，F-SYS-04） */
export const opLogApi = {
  page: (params: { page: number; size: number; module?: string }) =>
    http.get<PageResult<SysOperationLog>>('/operation-logs', params),
}
