package com.teasaas.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 门店日结出参（F-RPT-01，接口需求 12.1）
 */
@Data
@Schema(description = "门店日结")
public class DailySettleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日结日期 yyyy-MM-dd")
    private String date;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "订单量（已支付未退款）")
    private Long orderCount;

    @Schema(description = "营业额(分)")
    private Long salesAmount;

    @Schema(description = "客单价(分)")
    private Long avgOrderAmount;

    @Schema(description = "退款笔数（当日退款成功的退款单）")
    private Long refundCount;

    @Schema(description = "退款额(分)")
    private Long refundAmount;

    @Schema(description = "微信实收(分)")
    private Long wxAmount;

    @Schema(description = "储值实收(分)")
    private Long walletAmount;
}
