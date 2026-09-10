package com.teasaas.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 规格选项入参（F-PRD-01，加价为分）
 */
@Data
@Schema(description = "规格选项入参")
public class ProductSpecOptionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "选项名（如少冰/全糖）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "optionName不能为空")
    @Size(max = 16, message = "选项名最长16字符")
    private String optionName;

    @Schema(description = "加价（分）")
    @Min(value = 0, message = "加价不能为负")
    private Integer extraPrice;

    @Schema(description = "排序")
    private Integer sort;
}
