package com.teasaas.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 门店手工收银下单入参（接口需求 8.4 F-ORD-04，P0-1）
 */
@Data
@Schema(description = "POS手工下单入参")
public class PosOrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "storeId不能为空")
    private Long storeId;

    @Schema(description = "明细（本店已上架商品+规格）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<OrderItemDTO> items;
}
