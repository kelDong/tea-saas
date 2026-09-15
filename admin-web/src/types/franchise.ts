/** 加盟模块类型（F-FRA-01/02，后端 FraFranchiseeController / FraStoreController） */

/** 门店（FraStore 实体直出） */
export interface FraStore {
  id: number
  storeCode: string
  storeName: string
  /** NULL=直营 */
  franchiseeId: number | null
  address: string | null
  longitude: number | null
  latitude: number | null
  /** 1营业/0打烊 */
  businessStatus: number
  openTime: string | null
  closeTime: string | null
  status: number
}

/** 加盟商（FraFranchisee 实体直出） */
export interface FraFranchisee {
  id: number
  franchiseeCode: string
  franchiseeName: string
  contactName: string | null
  contactPhone: string | null
  /** A/B/C 计价等级，缺省 C */
  levelCode: string
  joinDate: string | null
  /** 1合作中/0已终止 */
  status: number
  createTime: string
}

/** 加盟商新建入参（F-FRA-01） */
export interface FranchiseeCreateReq {
  franchiseeCode: string
  franchiseeName: string
  contactName?: string
  contactPhone?: string
  /** ^[ABC]$，缺省 C */
  levelCode?: string
  /** yyyy-MM-dd */
  joinDate?: string
}

/** 加盟商编辑入参（等级走 /level 独立接口） */
export interface FranchiseeUpdateReq {
  franchiseeName: string
  contactName?: string
  contactPhone?: string
  joinDate?: string
}

/** 等级调整入参（等级计价联动，流水留痕） */
export interface FranchiseeLevelReq {
  levelCode: string
  remark?: string
}

/** 加盟商开账号入参（缺省密码时用系统默认） */
export interface FranchiseeAccountReq {
  username: string
  nickname?: string
  password?: string
}

/** 加盟商状态入参：1合作中/0已终止 */
export interface FranchiseeStatusReq {
  status: number
}

/** 门店新建/编辑入参（两 DTO 字段一致，F-FRA-02） */
export interface StoreSaveReq {
  storeCode: string
  storeName: string
  /** NULL=直营 */
  franchiseeId?: number
  address?: string
  longitude?: number
  latitude?: number
  openTime?: string
  closeTime?: string
}

/** 门店营业状态入参：1营业/0打烊 */
export interface StoreBusinessStatusReq {
  businessStatus: number
}
