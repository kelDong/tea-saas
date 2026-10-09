package com.teasaas.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * C 端门店商品分类 VO（F-APP-02：仅返回该店有可售商品的分类，按 sort 排序）
 */
@Data
@Schema(description = "C端门店商品分类")
public class AppCategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "分类名称")
    private String name;
}
