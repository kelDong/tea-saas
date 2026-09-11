package com.teasaas.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 门店营业趋势出参（F-RPT-01，接口需求 12.1）
 *
 * <p>口径：营业额/订单量按 pay_time 落于时段、status∈{2,3,4,5}
 * （已支付未退款；已退款 7 与已取消 6 不计入）。</p>
 */
@Data
@Schema(description = "门店营业趋势")
public class StoreReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "粒度：day/week/month")
    private String granularity;

    @Schema(description = "门店ID（空=全品牌汇总）")
    private Long storeId;

    @Schema(description = "趋势明细")
    private List<TrendNode> items;

    @Data
    @Schema(description = "时段营业点")
    public static class TrendNode implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "时段标识（日 yyyy-MM-dd / 周 xxxx-Www / 月 yyyy-MM）")
        private String period;

        @Schema(description = "订单量")
        private Long orderCount;

        @Schema(description = "营业额(分)")
        private Long salesAmount;
    }
}
