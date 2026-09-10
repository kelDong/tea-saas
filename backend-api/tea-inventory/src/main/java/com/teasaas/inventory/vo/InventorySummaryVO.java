package com.teasaas.inventory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 总部库存总览 VO（F-INV-03，接口需求 7.2：按原料聚合全租户）
 */
@Data
@Schema(description = "总部库存总览项")
public class InventorySummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原料ID")
    private Long materialId;

    @Schema(description = "原料编码")
    private String materialCode;

    @Schema(description = "原料名称")
    private String materialName;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "库存总量")
    private BigDecimal totalQty;

    @Schema(description = "有库存门店数")
    private Long storeCount;
}
