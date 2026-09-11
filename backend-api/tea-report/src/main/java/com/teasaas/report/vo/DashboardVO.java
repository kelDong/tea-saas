package com.teasaas.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 品牌汇总看板出参（F-RPT-02，接口需求 12.2）
 */
@Data
@Schema(description = "品牌看板")
public class DashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "今日营业额(分)")
    private Long todaySales;

    @Schema(description = "今日订单量")
    private Long todayOrders;

    @Schema(description = "昨日营业额(分)")
    private Long yesterdaySales;

    @Schema(description = "昨日订单量")
    private Long yesterdayOrders;

    @Schema(description = "会员总数")
    private Long memberTotal;

    @Schema(description = "门店总数（在营）")
    private Long storeTotal;

    @Schema(description = "库存预警条数（stock_qty<=预警线）")
    private Long stockWarnCount;
}
