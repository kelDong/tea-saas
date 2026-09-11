package com.teasaas.report.mapper;

import com.teasaas.report.vo.DailySettleVO;
import com.teasaas.report.vo.HotProductVO;
import com.teasaas.report.vo.InventorySummaryVO;
import com.teasaas.report.vo.StoreRankVO;
import com.teasaas.report.vo.StoreReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报表聚合查询（F-RPT-01/02，接口需求 12.1/12.2）
 *
 * <p>跨模块表（fra_store/bom_material/mem_member/pay_refund）只读 join，
 * 先例 ReconcileMapper；租户插件对 join 各表自动拼 tenant_id。
 * 营业口径：pay_time 落于时段且 status∈{2,3,4,5}（已退款 7/取消 6 不计）。</p>
 */
@Mapper
public interface ReportMapper {

    /** 营业趋势（granularity: day/week/month 决定分组格式） */
    List<StoreReportVO.TrendNode> selectStoreTrend(@Param("storeId") Long storeId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end,
                                                   @Param("granularity") String granularity);

    /** 门店日结（订单聚合 + 当日退款单 + 渠道分布） */
    DailySettleVO selectDailySettle(@Param("storeId") Long storeId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    /** 热销商品 TOP（近 N 天订单明细聚合） */
    List<HotProductVO> selectHotProducts(@Param("start") LocalDateTime start,
                                         @Param("limit") int limit);

    /** 门店排行（指定日按营业额降序） */
    List<StoreRankVO> selectStoreRanking(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    /** 时段营业汇总（看板今日/昨日复用） */
    StoreReportVO.TrendNode selectSalesBetween(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    /** 会员总数 */
    Long countMembers();

    /** 在营门店数 */
    Long countStores();

    /** 库存预警条数 */
    Long countStockWarn();

    /** 库存台账总行数（总览分页总量） */
    Long countInventory();

    /** 库存总览（预警缺口优先，分页） */
    List<InventorySummaryVO> selectInventorySummary(@Param("limit") int limit,
                                                    @Param("offset") int offset);
}
