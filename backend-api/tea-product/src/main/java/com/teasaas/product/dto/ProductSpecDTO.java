package com.teasaas.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 商品规格组入参（F-PRD-01，温度/糖度等）
 */
@Data
@Schema(description = "商品规格组入参")
public class ProductSpecDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "规格名（如温度/糖度）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "specName不能为空")
    @Size(max = 16, message = "规格名最长16字符")
    private String specName;

    @Schema(description = "是否必选：1是/0否")
    private Integer isRequired;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "规格选项")
    @Valid
    private List<ProductSpecOptionDTO> options;
}
