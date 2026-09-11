package com.teasaas.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 对账结果出参（接口需求 9.3，数据库设计 13.4）：无状态按日实时计算
 */
@Data
@Schema(description = "日终对账结果")
public class ReconcileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "对账日期 yyyy-MM-dd")
    private String date;

    @Schema(description = "当日已支付订单数")
    private Long orderCount;

    @Schema(description = "当日订单实付合计(分)")
    private Long orderAmount;

    @Schema(description = "当日成功支付流水数")
    private Long paymentCount;

    @Schema(description = "当日支付流水合计(分)")
    private Long paymentAmount;

    @Schema(description = "资金差异数")
    private Integer diffCount;

    @Schema(description = "资金差异明细")
    private List<DiffNode> diffs;

    @Schema(description = "库存账实差异数（流水累计 vs 台账现值）")
    private Integer stockDiffCount;

    @Schema(description = "库存账实差异明细")
    private List<StockDiffNode> stockDiffs;

    @Data
    @Schema(description = "资金差异项")
    public static class DiffNode implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "差异类型：ORDER_NO_PAYMENT订单缺流水/PAYMENT_NO_ORDER流水无单/AMOUNT_MISMATCH金额不符")
        private String type;

        @Schema(description = "订单号")
        private String orderNo;

        @Schema(description = "支付流水号")
        private String paymentNo;

        @Schema(description = "订单实付(分)")
        private Long orderAmount;

        @Schema(description = "流水金额(分)")
        private Long paymentAmount;
    }

    @Data
    @Schema(description = "库存账实差异项")
    public static class StockDiffNode implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "门店ID")
        private Long storeId;

        @Schema(description = "原料ID")
        private Long materialId;

        @Schema(description = "原料名称")
        private String materialName;

        @Schema(description = "流水累计变动")
        private BigDecimal loggedQty;

        @Schema(description = "台账现值")
        private BigDecimal stockQty;
    }
}
