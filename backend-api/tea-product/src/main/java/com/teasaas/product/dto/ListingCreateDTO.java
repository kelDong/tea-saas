package com.teasaas.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 门店上架入参（F-PRD-02，接口需求 6.2：{storeId, productId, salePrice(分)}）
 */
@Data
@Schema(description = "门店上架入参")
public class ListingCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店ID（须为当前加盟商名下门店）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "storeId不能为空")
    private Long storeId;

    @Schema(description = "商品ID（须为产品库已发布商品）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "productId不能为空")
    private Long productId;

    @Schema(description = "本店售价（分，不得高于总部建议售价）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "salePrice不能为空")
    @Min(value = 1, message = "售价必须大于0")
    private Integer salePrice;
}
