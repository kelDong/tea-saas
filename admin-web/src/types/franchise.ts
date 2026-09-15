/** 加盟模块类型（F-FRA-02，后端 FraStoreController） */

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
