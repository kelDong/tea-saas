package com.teasaas.bom.vo;

import lombok.Data;

/**
 * 毛利试算结果 VO（F-BOM-03）
 * 三个金额字段单位均为分；grossMargin 为百分比整数（0-100，四舍五入）
 */
@Data
public class ProfitCalcVO {

    /** 成本合计（分） */
    private Integer costTotal;

    /** 建议售价（分） */
    private Integer suggestPrice;

    /** 毛利（分，可为负） */
    private Integer grossProfit;

    /** 毛利率（百分比整数 0-100，四舍五入；suggestPrice 为 0 时取 0） */
    private Integer grossMargin;
}
