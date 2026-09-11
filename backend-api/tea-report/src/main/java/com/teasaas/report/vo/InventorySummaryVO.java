package com.teasaas.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存总览出参（F-RPT-02，接口需求 12.2；R2/R5 只读）
 */
@Data
@Schema(description = "库存总览项")
public class InventorySummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "门店名")
    private String storeName;

    @Schema(description = "原料ID")
    private Long materialId;

    @Schema(description = "原料名")
    private String materialName;

    @Schema(description = "库存量")
    private BigDecimal stockQty;

    @Schema(description = "预警阈值（空=未配置）")
    private BigDecimal warnThreshold;

    @Schema(description = "是否预警（1是/0否）")
    private Integer warn;
}
