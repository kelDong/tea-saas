package com.teasaas.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * C 端门店可售商品 VO（F-PRD-03：仅返回库存充足的可售商品）
 */
@Data
@Schema(description = "C端可售商品")
public class AppProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "上架记录ID")
    private Long listingId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "本店售价（分）")
    private Integer salePrice;

    @Schema(description = "图片")
    private String image;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "规格组")
    private List<ProductDetailVO.SpecNode> specs;
}
