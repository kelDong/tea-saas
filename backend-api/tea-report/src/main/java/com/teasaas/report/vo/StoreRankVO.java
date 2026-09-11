package com.teasaas.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 门店排行出参（F-RPT-02，接口需求 12.2）
 */
@Data
@Schema(description = "门店排行")
public class StoreRankVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "门店名")
    private String storeName;

    @Schema(description = "营业额(分)")
    private Long salesAmount;

    @Schema(description = "订单量")
    private Long orderCount;
}
