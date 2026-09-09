package com.teasaas.bom.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 配方详情 VO（F-BOM-02）：配方主体 + 明细列表（含原料名称/单位冗余便于前端展示）
 */
@Data
public class FormulaDetailVO {

    private Long id;

    private Long tenantId;

    private String formulaCode;

    private String formulaName;

    private Integer version;

    /** 试算成本（分/杯） */
    private Integer costTotal;

    /** 0 停用 / 1 正常 / 2 已发布 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<ItemVO> items;

    /**
     * 配方明细行
     */
    @Data
    public static class ItemVO {

        private Long id;

        private Long materialId;

        /** 原料名称（组装自 bom_material） */
        private String materialName;

        /** 原料单位（组装自 bom_material） */
        private String unit;

        private BigDecimal qty;

        private Integer sort;
    }
}
