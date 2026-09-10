package com.teasaas.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 盘点修正入参（F-INV-01，接口需求 7.1：{storeId, materialId, actualQty, remark}）
 */
@Data
@Schema(description = "盘点修正入参")
public class StockAdjustDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "storeId不能为空")
    private Long storeId;

    @Schema(description = "原料ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "materialId不能为空")
    private Long materialId;

    @Schema(description = "实盘数量（以实盘为准修正账面）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "actualQty不能为空")
    @DecimalMin(value = "0", message = "实盘数量不能为负")
    private BigDecimal actualQty;

    @Schema(description = "备注（留痕）")
    @Size(max = 128, message = "备注最长128字符")
    private String remark;
}
