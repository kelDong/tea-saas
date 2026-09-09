package com.teasaas.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 采购单明细入参项（F-SUP-03）
 */
@Data
@Schema(description = "采购单明细入参项")
public class PurchaseOrderItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原料ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "materialId不能为空")
    private Long materialId;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "qty不能为空")
    @DecimalMin(value = "0.001", message = "数量必须大于0")
    private BigDecimal qty;
}
