package com.teasaas.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * POS 收款确认出参（接口需求 8.4）：现金渠道含找零
 */
@Data
@Schema(description = "POS收款结果")
public class PosPayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "应收金额(分)")
    private Long payAmount;

    @Schema(description = "实收金额(分)")
    private Long receiveAmount;

    @Schema(description = "找零(分)，非现金渠道为0")
    private Long changeAmount;

    @Schema(description = "支付流水号")
    private String paymentNo;
}
