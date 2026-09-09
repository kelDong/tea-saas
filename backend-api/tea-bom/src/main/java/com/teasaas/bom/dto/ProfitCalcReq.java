package com.teasaas.bom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * 毛利试算入参（F-BOM-03）
 * suggestPrice 单位：分（红线：金额一律 Integer 存分）
 */
@Data
public class ProfitCalcReq {

    @NotNull(message = "配方 ID 不能为空")
    private Long formulaId;

    /** 建议售价（分） */
    @NotNull(message = "建议售价不能为空")
    @PositiveOrZero(message = "建议售价不能为负数")
    private Integer suggestPrice;
}
