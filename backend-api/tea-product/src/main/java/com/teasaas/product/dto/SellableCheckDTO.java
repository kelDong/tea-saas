package com.teasaas.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 商品可售校验入参（F-PRD-03，接口需求 6.3）
 */
@Data
@Schema(description = "商品可售校验入参")
public class SellableCheckDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "上架记录ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "listingIds不能为空")
    private List<Long> listingIds;
}
