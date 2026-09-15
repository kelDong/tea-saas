/** 平台侧类型（F-SYS-01，后端 TenantController / SysPackageController，前缀 /api/admin/v1，R1 专属）
 *  金额字段单位「分」 */

/** 品牌租户行（TenantVO） */
export interface TenantVO {
  id: number
  tenantCode: string
  tenantName: string
  contactName: string | null
  contactPhone: string | null
  packageCode: string | null
  /** 门店数上限（0=不限） */
  storeLimit: number
  /** 1启用/0停用 */
  status: number
  /** yyyy-MM-dd HH:mm:ss */
  expireTime: string | null
  createTime: string
}

/** 创建品牌入参（tenantCode 仅字母/数字/下划线/中划线） */
export interface TenantCreateReq {
  tenantCode: string
  tenantName: string
  contactName?: string
  contactPhone?: string
  packageCode?: string
  storeLimit?: number
  /** yyyy-MM-dd HH:mm:ss */
  expireTime?: string
}

/** 编辑品牌入参（tenantCode 不可改） */
export interface TenantUpdateReq {
  tenantName: string
  contactName?: string
  contactPhone?: string
  packageCode?: string
  storeLimit?: number
  expireTime?: string
}

/** 套餐行（SysPackageVO；套餐 CRUD 二期） */
export interface SysPackageVO {
  id: number
  packageCode: string
  packageName: string
  /** 单价（分/店/月） */
  pricePerStore: number
  /** 门店数上限 */
  storeLimit: number
  /** 功能开关 JSON */
  featureJson: string | null
  /** 1启用/0停用 */
  status: number
}
