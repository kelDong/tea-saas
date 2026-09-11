package com.teasaas.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * C 端下单入参（接口需求 8.1 F-ORD-01）
 *
 * <p>券/积分/储值随批次 4 开放：券核销、积分抵扣（超限按可抵上限截断）、
 * 储值支付（P0-4：独立通道，不与微信混合）均在下单事务内落账。</p>
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

    @Schema(description = "用户券ID（可选）")
    private Long couponId;

    @Schema(description = "使用积分数量（可选，超限自动截断到可抵上限）")
    @Min(1)
    private Integer pointsUsed;

    @Schema(description = "支付渠道：1微信/2储值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "payChannel不能为空")
    private Integer payChannel;
}
