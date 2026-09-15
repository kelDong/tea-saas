/** BOM 模块类型（F-BOM-01~03，后端 BomMaterial/BomFormula Controller） */

/** 原料（BomMaterial 实体直出；数量用 BigDecimal，金额 Integer 分） */
export interface BomMaterial {
  id: number
  materialCode: string
  materialName: string
  category: string | null
  /** 单位：g/kg/个/杯/L */
  unit: string
  warnThreshold: number | null
  /** 参考成本（分/单位） */
  costPrice: number
  /** 0停用/1启用 */
  status: number
  createTime: string
}

/** 原料新增/编辑入参（F-BOM-01） */
export interface MaterialSaveReq {
  materialCode: string
  materialName: string
  category?: string
  unit: string
  warnThreshold?: number
  costPrice: number
}

/** 配方行（BomFormula 实体直出） */
export interface BomFormula {
  id: number
  formulaCode: string
  formulaName: string
  version: number
  /** 试算成本（分/杯） */
  costTotal: number
  /** 0停用/1正常/2已发布 */
  status: number
  updateTime: string
  createTime: string
}

/** 配方明细项（FormulaDetailVO.ItemVO） */
export interface FormulaItemVO {
  id: number
  materialId: number
  materialName: string
  unit: string
  qty: number
  sort: number
}

/** 配方详情（FormulaDetailVO，含明细） */
export interface FormulaDetailVO {
  id: number
  formulaCode: string
  formulaName: string
  version: number
  costTotal: number
  status: number
  createTime: string
  updateTime: string
  items: FormulaItemVO[]
}

/** 配方明细入参（FormulaItemReq） */
export interface FormulaItemReq {
  materialId: number
  /** 最小 0.001 */
  qty: number
  sort?: number
}

/** 配方新增/编辑入参（F-BOM-02；编辑时 version+1、明细全量重绑） */
export interface FormulaSaveReq {
  formulaCode: string
  formulaName: string
  items: FormulaItemReq[]
}

/** 毛利试算入参（F-BOM-03） */
export interface ProfitCalcReq {
  formulaId: number
  /** 建议售价（分） */
  suggestPrice: number
}

/** 毛利试算结果（ProfitCalcVO；金额分，grossMargin 为百分比整数 0-100） */
export interface ProfitCalcVO {
  costTotal: number
  suggestPrice: number
  grossProfit: number
  grossMargin: number
}
