import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  BomFormula,
  BomMaterial,
  FormulaDetailVO,
  FormulaSaveReq,
  MaterialSaveReq,
  ProfitCalcReq,
  ProfitCalcVO,
} from '@/types/bom'

/** 原料档案接口（后端 BomMaterialController /api/v1/materials，F-BOM-01） */
export const materialApi = {
  page: (params: { page: number; size: number; keyword?: string }) =>
    http.get<PageResult<BomMaterial>>('/materials', params),
  detail: (id: number) => http.get<BomMaterial>(`/materials/${id}`),
  create: (data: MaterialSaveReq) => http.post<number>('/materials', data),
  update: (id: number, data: MaterialSaveReq) => http.put<void>(`/materials/${id}`, data),
  updateStatus: (id: number, status: number) => http.patch<void>(`/materials/${id}/status`, { status }),
}

/** 配方 BOM 接口（后端 BomFormulaController /api/v1/formulas，F-BOM-02/03） */
export const formulaApi = {
  page: (params: { page: number; size: number; keyword?: string }) =>
    http.get<PageResult<BomFormula>>('/formulas', params),
  detail: (id: number) => http.get<FormulaDetailVO>(`/formulas/${id}`),
  create: (data: FormulaSaveReq) => http.post<number>('/formulas', data),
  update: (id: number, data: FormulaSaveReq) => http.put<void>(`/formulas/${id}`, data),
  copy: (id: number) => http.post<number>(`/formulas/${id}/copy`),
  updateStatus: (id: number, status: number) => http.patch<void>(`/formulas/${id}/status`, { status }),
  publish: (id: number) => http.post<void>(`/formulas/${id}/publish`),
  /** 毛利试算 */
  calcProfit: (data: ProfitCalcReq) => http.post<ProfitCalcVO>('/formulas/calc-profit', data),
}
