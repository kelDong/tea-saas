package com.teasaas.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 门店上架列表 VO（F-PRD-02：含总部建议价与实时可售标记）
 */
@Data
@Schema(description = "门店上架记录")
public class ListingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "上架记录ID")
    private Long id;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品编码")
    private String productCode;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "图片")
    private String image;

    @Schema(description = "本店售价（分）")
    private Integer salePrice;

    @Schema(description = "总部建议售价（分）")
    private Integer suggestPrice;

    @Schema(description = "上架状态：1上架/0下架")
    private Integer status;

    @Schema(description = "实时可售：1可售/0缺料（F-PRD-03 BOM 反算）")
    private Integer sellable;
}
