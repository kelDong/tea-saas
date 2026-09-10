package com.teasaas.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 售价调整入参（F-PRD-01 建议价 / F-PRD-02 本店价，单位分）
 */
@Data
@Schema(description = "售价调整入参")
public class PriceAdjustDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "售价（分）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "price不能为空")
    @Min(value = 1, message = "售价必须大于0")
    private Integer price;
}
