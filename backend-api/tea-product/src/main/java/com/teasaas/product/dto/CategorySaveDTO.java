package com.teasaas.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 分类新增/编辑入参（F-PRD-01，接口需求 6.1 分类管理）
 */
@Data
@Schema(description = "商品分类入参")
public class CategorySaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "categoryName不能为空")
    @Size(max = 32, message = "分类名称最长32字符")
    private String categoryName;

    @Schema(description = "父分类ID（顶级传0）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "parentId不能为空")
    @Min(value = 0, message = "parentId不合法")
    private Long parentId;

    @Schema(description = "排序（小在前）")
    @Min(0)
    @Max(9999)
    private Integer sort;
}
