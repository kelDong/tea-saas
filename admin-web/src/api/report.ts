import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  DailySettleVO,
  DashboardVO,
  HotProductVO,
  ReportInventoryRow,
  StoreRankVO,
  StoreReportVO,
} from '@/types/report'

/** 报表接口（后端 ReportController /api/v1/reports，F-RPT-01/02） */
export const reportApi = {
  /** 经营趋势（granularity: day/week/month；storeId 空=全品牌） */
  store: (params: { storeId?: number; granularity: string }) =>
    http.get<StoreReportVO>('/reports/store', params),
  /** 门店日结（date 缺省昨日） */
  storeDaily: (storeId: number, date?: string) =>
    http.get<DailySettleVO>(`/reports/store/${storeId}/daily`, { date }),
  /** 品牌工作台指标 */
  dashboard: () => http.get<DashboardVO>('/reports/dashboard'),
  /** 热销商品 */
  hotProducts: (params?: { days?: number; limit?: number }) =>
    http.get<HotProductVO[]>('/reports/hot-products', params),
  /** 门店排行（date 缺省昨日） */
  storeRanking: (date?: string) => http.get<StoreRankVO[]>('/reports/store-ranking', { date }),
  /** 库存总览（预警优先，分页） */
  inventorySummary: (params: { page: number; size: number }) =>
    http.get<PageResult<ReportInventoryRow>>('/reports/inventory-summary', params),
}
