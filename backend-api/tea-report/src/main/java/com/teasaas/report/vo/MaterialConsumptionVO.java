package com.teasaas.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 原料消耗趋势出参（F-RPT-03，接口需求 12.3；总部供应链集采计划）
 *
 * <p>净消耗口径：inv_stock_log biz_type∈{1,2,3}（下单扣−超时回补−退款回补，
 * SUM(-change_qty) 天然得净额）；采购入库/盘点不计。数量为 DECIMAL(10,3)
 * → BigDecimal（非金额，红线 1 不适用）。</p>
 */
@Data
@Schema(description = "原料消耗趋势")
public class MaterialConsumptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "统计天数（含今日）")
    private Integer days;

    @Schema(description = "门店ID（空=全部门店）")
    private Long storeId;

    @Schema(description = "TOP N 材料消耗（按期内净消耗降序）")
    private List<ItemNode> items = new ArrayList<>();

    /**
     * 材料消耗节点（平铺行由 Service 按材料聚合）
     */
    @Data
    @Schema(description = "材料消耗")
    public static class ItemNode implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "原料ID")
        private Long materialId;

        @Schema(description = "原料名称")
        private String materialName;

        @Schema(description = "单位（g/kg/个/杯/L）")
        private String unit;

        @Schema(description = "期内净消耗合计")
        private BigDecimal totalQty = BigDecimal.ZERO;

        @Schema(description = "逐日消耗")
        private List<DailyNode> daily = new ArrayList<>();
    }

    /**
     * 逐日消耗节点
     */
    @Data
    @Schema(description = "逐日消耗")
    public static class DailyNode implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "日期 yyyy-MM-dd")
        private LocalDate date;

        @Schema(description = "当日净消耗")
        private BigDecimal qty;
    }

    /**
     * Mapper 平铺行（材料×日），Service 聚合成嵌套结构
     */
    @Data
    @Schema(description = "材料消耗平铺行（内部聚合用）")
    public static class Row implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long materialId;

        private String materialName;

        private String unit;

        private LocalDate statDate;

        private BigDecimal qty;
    }
}
