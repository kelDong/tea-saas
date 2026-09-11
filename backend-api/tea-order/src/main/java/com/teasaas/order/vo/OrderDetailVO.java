package com.teasaas.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情（接口需求 8.2）：B 端/C 端共用
 */
@Data
@Schema(description = "订单详情")
public class OrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "会员ID（门店收银单可空）")
    private Long memberId;

    @Schema(description = "订单来源：1小程序/2门店收银")
    private Integer orderSource;

    @Schema(description = "状态：1待支付/2已支付/3制作中/4待取餐/5已完成/6已取消/7已退款")
    private Integer status;

    @Schema(description = "商品总额(分)")
    private Long totalAmount;

    @Schema(description = "优惠金额(分)")
    private Long discountAmount;

    @Schema(description = "实付金额(分)")
    private Long payAmount;

    @Schema(description = "支付渠道：1微信/2储值/3现金")
    private Integer payChannel;

    @Schema(description = "取餐码")
    private String takeCode;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "下单时间")
    private LocalDateTime createTime;

    @Schema(description = "订单明细")
    private List<OrderItemNode> items;

    @Data
    @Schema(description = "订单明细项")
    public static class OrderItemNode implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "商品ID")
        private Long productId;

        @Schema(description = "门店上架ID")
        private Long listingId;

        @Schema(description = "商品名快照")
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
