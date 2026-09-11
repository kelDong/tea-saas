package com.teasaas.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 订单预览（试算）出参（接口需求 8.1）
 */
@Data
@Schema(description = "订单试算结果")
public class OrderPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品总额(分)")
    private Long totalAmount;

    @Schema(description = "优惠金额(分)，一期固定0")
    private Long discountAmount;

    @Schema(description = "应收金额(分)")
    private Long payAmount;

    @Schema(description = "明细试算")
    private List<PreviewItemNode> items;

    @Data
    @Schema(description = "明细试算项")
    public static class PreviewItemNode implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "商品名")
        private String productName;

        @Schema(description = "规格快照")
        private String specDesc;

        @Schema(description = "成交单价(分)")
        private Integer price;

        @Schema(description = "数量")
        private Integer qty;

        @Schema(description = "小计(分)")
        private Long amount;
    }
}
