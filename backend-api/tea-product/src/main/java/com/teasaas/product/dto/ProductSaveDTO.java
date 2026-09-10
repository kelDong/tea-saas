package com.teasaas.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发布/编辑商品入参（F-PRD-01，接口需求 6.1）
 */
@Data
@Schema(description = "发布/编辑商品入参")
public class ProductSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "商品编码（租户内唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "productCode不能为空")
    @Size(max = 32, message = "商品编码最长32字符")
    private String productCode;

    @Schema(description = "商品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "productName不能为空")
    @Size(max = 64, message = "商品名称最长64字符")
    private String productName;

    @Schema(description = "配方ID（关联 bom_formula）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "formulaId不能为空")
    private Long formulaId;

    @Schema(description = "建议售价（分）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "suggestPrice不能为空")
    @Min(value = 1, message = "建议售价必须大于0")
    private Integer suggestPrice;

    @Schema(description = "商品图片")
    @Size(max = 255, message = "图片地址最长255字符")
    private String image;

    @Schema(description = "商品描述")
    @Size(max = 255, message = "描述最长255字符")
    private String description;

    @Schema(description = "发布状态：1发布 / 0草稿（编辑时可用于重新发布）")
    private Integer publishStatus;

    @Schema(description = "规格组")
    @Valid
    private List<ProductSpecDTO> specs;
}
