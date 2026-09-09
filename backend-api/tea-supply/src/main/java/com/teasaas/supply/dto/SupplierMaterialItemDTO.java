package com.teasaas.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

/**
 * 供应商关联原料项（F-SUP-01）
 */
@Data
@Schema(description = "供应商关联原料项")
public class SupplierMaterialItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原料ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "materialId不能为空")
    private Long materialId;

    @Schema(description = "参考供货价(分)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "供货价不能为空")
    @Positive(message = "供货价必须大于0")
    private Integer supplyPrice;
}
