/** 系统模块类型（F-SYS-02/04，后端 SysUserController/SysRoleController/SysOperationLogController） */

/** 用户 VO（SysUserVO） */
export interface SysUserVO {
  id: number
  tenantId: number
  username: string
  nickname: string | null
  phone: string | null
  /** 0平台/1总部/2加盟商/3门店 */
  userType: number
  /** 1启用/0停用 */
  status: number
  createTime: string
  roleIds?: number[]
  roleNames?: string[]
  storeIds?: number[]
}

/** 创建用户入参（F-SYS-02） */
export interface SysUserCreateReq {
  username: string
  password: string
  nickname?: string
  phone?: string
  roleIds: number[]
  storeIds?: number[]
}

/** 编辑用户入参（后端 update 全量覆盖 nickname/phone，须带上原值） */
export interface SysUserUpdateReq {
  nickname?: string
  phone?: string
  status?: number
  roleIds?: number[]
  storeIds?: number[]
}

/** 角色（SysRole 实体直出；R1-R8 预置） */
export interface SysRole {
  id: number
  roleName: string
  roleCode: string
  /** 1全部品牌/2本租户/3名下门店/4本店 */
  dataScope: number
  remark: string | null
  status: number
}

/** 操作日志（SysOperationLog 实体直出） */
export interface SysOperationLog {
  id: number
  tenantId: number
  userId: number
  module: string
  action: string
  method: string
  params: string | null
  ip: string | null
  costMs: number | null
  /** 1成功/0失败 */
  status: number
  createTime: string
}
