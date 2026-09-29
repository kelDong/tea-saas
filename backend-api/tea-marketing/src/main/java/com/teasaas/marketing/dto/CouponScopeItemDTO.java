package com.teasaas.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 券试算商品明细（F-MKT-01 指定商品券口径）
 *
 * <p>指定商品券的门槛与优惠均按"适用商品小计(base)"计算——
 * 调用方（tea-order preview/appCreate）按 AssembledItem 的
 * productId 合并金额后传入。</p>
 */
@Data
@Schema(description = "券试算商品明细")
public class CouponScopeItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "该商品小计（分）")
    private Long amount;

    public CouponScopeItemDTO() {
    }

    public CouponScopeItemDTO(Long productId, Long amount) {
        this.productId = productId;
        this.amount = amount;
    }
}
