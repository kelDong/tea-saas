package com.teasaas.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * C 端下单入参（接口需求 8.1 F-ORD-01）
 *
 * <p>couponId/pointsUsed 一期未开放（F-MKT/F-MEM 批次 4），传值将拒绝；
 * payChannel 仅支持 1 微信（2 储值待批次 4 储值通道）。</p>
 */
@Data
@Schema(description = "C端下单入参")
public class AppOrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "storeId不能为空")
    private Long storeId;

    @Schema(description = "明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<OrderItemDTO> items;

    @Schema(description = "优惠券ID（一期未开放，禁止传值）")
    private Long couponId;

    @Schema(description = "使用积分（一期未开放，禁止传值）")
    private Integer pointsUsed;

    @Schema(description = "支付渠道：1微信/2储值（一期仅1）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "payChannel不能为空")
    private Integer payChannel;
}
