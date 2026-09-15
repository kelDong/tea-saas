/** 商品与上架模块类型（F-PRD-01~03，后端 Prd* Controller） */

/** 商品分类（PrdCategory 实体直出） */
export interface PrdCategory {
  id: number
  categoryName: string
  /** 顶级为 0 */
  parentId: number
  sort: number
  createTime: string
}

/** 分类新增/编辑入参 */
export interface CategorySaveDTO {
  categoryName: string
  /** 顶级传 0 */
  parentId: number
  sort?: number
}

/** 商品行（PrdProduct 实体直出） */
export interface PrdProduct {
  id: number
  categoryId: number | null
  productCode: string
  productName: string
  formulaId: number
  /** 建议售价（分） */
  suggestPrice: number
  image: string | null
  description: string | null
  /** 1已发布/0草稿（下架） */
  publishStatus: number
  status: number
  createTime: string
  updateTime: string
}

/** 规格选项（ProductDetailVO.OptionNode / ProductSpecOptionDTO 同构） */
export interface SpecOption {
  id?: number
  optionName: string
  /** 加价（分） */
  extraPrice?: number
  sort?: number
}

/** 规格（ProductDetailVO.SpecNode / ProductSpecDTO 同构） */
export interface ProductSpec {
  id?: number
  specName: string
  /** 1必选/0可选 */
  isRequired?: number
  sort?: number
  options: SpecOption[]
}

/** 商品详情（ProductDetailVO，含规格树） */
export interface ProductDetailVO {
  id: number
  categoryId: number | null
  productCode: string
  productName: string
  formulaId: number
  formulaName: string | null
  suggestPrice: number
  image: string | null
  description: string | null
  publishStatus: number
  createTime: string
  specs: ProductSpec[]
}

/** 商品新增/编辑入参（F-PRD-01；编辑时规格全量重绑） */
export interface ProductSaveDTO {
  categoryId?: number
  productCode: string
  productName: string
  formulaId: number
  suggestPrice: number
  image?: string
  description?: string
  /** 1发布/0草稿 */
  publishStatus?: number
  specs?: ProductSpec[]
}

/** 上架行（ListingVO） */
export interface ListingVO {
  id: number
  storeId: number
  productId: number
  productCode: string
  productName: string
  image: string | null
  /** 本店售价（分） */
  salePrice: number
  /** 总部建议价（分） */
  suggestPrice: number
  /** 1上架/0下架 */
  status: number
  /** 1可售/0缺料 */
  sellable: number
}

/** 上架入参（F-PRD-02；salePrice 不得高于总部建议价） */
export interface ListingCreateDTO {
  storeId: number
  productId: number
  salePrice: number
}

/** 可售校验缺料项（SellableCheckVO.ShortageNode） */
export interface ShortageNode {
  materialId: number
  materialName: string
  unit: string
  needQty: number
  stockQty: number
}

/** 可售校验结果（SellableCheckVO，F-PRD-03） */
export interface SellableCheckVO {
  listingId: number
  sellable: boolean
  shortages: ShortageNode[]
}
