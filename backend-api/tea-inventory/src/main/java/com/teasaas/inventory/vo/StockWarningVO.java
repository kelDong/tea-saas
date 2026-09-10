package com.teasaas.inventory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 低库存预警 VO（F-INV-03，接口需求 7.2）
 */
@Data
@Schema(description = "低库存预警项")
public class StockWarningVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "原料ID")
    private Long materialId;

    @Schema(description = "原料编码")
    private String materialCode;

    @Schema(description = "原料名称")
    private String materialName;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "当前库存")
    private BigDecimal stockQty;

    @Schema(description = "预警阈值")
    private BigDecimal warnThreshold;

    @Schema(description = "缺口（阈值-库存）")
    private BigDecimal gap;
}
