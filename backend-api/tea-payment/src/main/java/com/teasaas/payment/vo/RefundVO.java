package com.teasaas.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退款流水出参（接口需求 9.2）
 */
@Data
@Schema(description = "退款流水")
public class RefundVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "退款单号")
    private String refundNo;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "原支付流水ID")
    private Long paymentId;

    @Schema(description = "退款金额(分)，整单退=实付")
    private Long amount;

    @Schema(description = "退款类型：1微信原路/2退储值")
    private Integer refundType;

    @Schema(description = "已核销券不退还固定0")
    private Integer couponReturned;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "状态：1处理中/2成功/3失败")
    private Integer status;

    @Schema(description = "退款完成时间")
    private LocalDateTime refundTime;

    @Schema(description = "申请时间")
    private LocalDateTime createTime;
}
