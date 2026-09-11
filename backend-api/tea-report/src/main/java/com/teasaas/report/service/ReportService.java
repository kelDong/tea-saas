package com.teasaas.report.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.report.vo.DailySettleVO;
import com.teasaas.report.vo.DashboardVO;
import com.teasaas.report.vo.HotProductVO;
import com.teasaas.report.vo.InventorySummaryVO;
import com.teasaas.report.vo.StoreRankVO;
import com.teasaas.report.vo.StoreReportVO;

import java.util.List;

/**
 * 报表服务（F-RPT-01/02，接口需求 12.1/12.2；F-RPT-03 原料消耗二期）
 *
 * <p>营业口径：pay_time 落于时段且订单 status∈{2,3,4,5}
 * （已支付未退款）；门店权限：R6/R7 名下、R2 全品牌、R5 品牌只读。</p>
 */
public interface ReportService {

    /** 门店营业趋势（granularity: day近30天/week近8周/month近12月；R6/R7 须本店） */
    StoreReportVO storeTrend(String granularity, Long storeId);

    /** 门店日结（date 默认昨日；R7 本店/R2） */
    DailySettleVO dailySettle(Long storeId, String date);

    /** 品牌汇总看板（R2/R5） */
    DashboardVO dashboard();

    /** 热销商品 TOP（days 默认 7，limit 默认 10；R2/R5） */
    List<HotProductVO> hotProducts(Integer days, Integer limit);

    /** 门店排行（date 默认昨日；R2/R5） */
    List<StoreRankVO> storeRanking(String date);

    /** 库存总览（预警优先排序；R2/R5 只读） */
    PageResult<InventorySummaryVO> inventorySummary(int page, int size);
}
