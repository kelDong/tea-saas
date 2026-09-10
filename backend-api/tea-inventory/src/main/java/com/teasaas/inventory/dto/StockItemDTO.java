package com.teasaas.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存变动明细项（跨模块服务调用入参：采购入库/下单扣减/回补共用）
 */
@Data
@Schema(description = "库存变动明细项")
public class StockItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原料ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "materialId不能为空")
    private Long materialId;

    @Schema(description = "数量（扣减/入库为正数，内部按方向取符号）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "qty不能为空")
    @DecimalMin(value = "0", message = "数量不能为负")
    private BigDecimal qty;
}
