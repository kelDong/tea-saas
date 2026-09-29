package com.teasaas.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 门店收款确认入参（接口需求 8.4，P0-4：三选一不混合）
 *
 * <p>payChannel 为 POS 端编号：1现金/2微信收款码Native/3储值（与订单表
 * pay_channel 1微信/2储值/3现金 不同，服务内做映射）。</p>
 */
@Data
@Schema(description = "POS收款确认入参")
public class PosPayDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "收款渠道：1现金/2微信收款码Native/3储值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "payChannel不能为空")
    private Integer payChannel;

    @Schema(description = "实收金额(分)，现金找零用；现金渠道必传且不得小于应收")
    @Min(value = 1, message = "实收金额必须为正数")
    private Long receiveAmount;

    @Schema(description = "会员ID：储值渠道必填；现金/微信可选（传则绑会员并发放积分）")
    private Long memberId;
}
