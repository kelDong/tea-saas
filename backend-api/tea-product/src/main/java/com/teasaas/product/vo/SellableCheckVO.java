package com.teasaas.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品可售校验结果 VO（F-PRD-03）
 */
@Data
@Schema(description = "可售校验结果")
public class SellableCheckVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "上架记录ID")
    private Long listingId;

    @Schema(description = "是否可售：1可售/0缺料")
    private Boolean sellable;

    @Schema(description = "缺料明细（不可售时返回）")
    private List<ShortageNode> shortages;

    /**
     * 缺料节点
     */
    @Data
    @Schema(description = "缺料明细")
    public static class ShortageNode implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "原料ID")
        private Long materialId;

        @Schema(description = "原料名称")
        private String materialName;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "配方用量")
        private BigDecimal needQty;

        @Schema(description = "当前库存")
        private BigDecimal stockQty;
    }
}
