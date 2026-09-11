package com.teasaas.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 热销商品出参（F-RPT-02，接口需求 12.2）
 */
@Data
@Schema(description = "热销商品")
public class HotProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名（订单快照）")
    private String productName;

    @Schema(description = "销量（份）")
    private Long qty;

    @Schema(description = "销售额(分)")
    private Long salesAmount;
}
