/** 报表模块类型（F-RPT-01/02，后端 ReportController /api/v1/reports）
 *  金额字段单位均为「分」（Long），展示层用 fen2yuan */

/** 经营趋势（F-RPT-01；口径：pay_time 落于时段，status∈{2,3,4,5}，退款/取消不计） */
export interface StoreReportVO {
  granularity: string
  /** 空=全品牌（R2） */
  storeId: number | null
  items: TrendNode[]
}

export interface TrendNode {
  /** 日 yyyy-MM-dd / 周 xxxx-Www / 月 yyyy-MM */
  period: string
  orderCount: number
  /** 分 */
  salesAmount: number
}

/** 门店日结（F-RPT-02；已支付未退款口径） */
export interface DailySettleVO {
  /** yyyy-MM-dd */
  date: string
  storeId: number
  orderCount: number
  /** 分 */
  salesAmount: number
  /** 客单价，分 */
  avgOrderAmount: number
  refundCount: number
  /** 分 */
  refundAmount: number
  /** 微信实收，分 */
  wxAmount: number
  /** 储值实收，分 */
  walletAmount: number
}

/** 品牌工作台指标 */
export interface DashboardVO {
  /** 分 */
  todaySales: number
  todayOrders: number
  /** 分 */
  yesterdaySales: number
  yesterdayOrders: number
  memberTotal: number
  /** 在营门店数 */
  storeTotal: number
  /** 库存预警条数 */
  stockWarnCount: number
}

/** 热销商品（按订单快照名聚合） */
export interface HotProductVO {
  productId: number
  productName: string
  /** 份数 */
  qty: number
  /** 分 */
  salesAmount: number
}

/** 门店排行（按日实收） */
export interface StoreRankVO {
  storeId: number
  storeName: string
  /** 分 */
  salesAmount: number
  orderCount: number
}

/** 库存总览行（报表口径，F-RPT-02；与 tea-inventory 的 InventorySummaryVO 字段不同） */
export interface ReportInventoryRow {
  storeId: number
  storeName: string
  materialId: number
  materialName: string
  stockQty: number
  warnThreshold: number | null
  /** 1预警/0正常 */
  warn: number
}
