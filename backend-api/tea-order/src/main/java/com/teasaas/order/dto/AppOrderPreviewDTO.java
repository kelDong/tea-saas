package com.teasaas.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * C 端订单预览（试算）入参（接口需求 8.1）：只算钱不落库不扣库存
 */
@Data
@Schema(description = "订单预览入参")
public class AppOrderPreviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "storeId不能为空")
    private Long storeId;

    @Schema(description = "明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<OrderItemDTO> items;
}
