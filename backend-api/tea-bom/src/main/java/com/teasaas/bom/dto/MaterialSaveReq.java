package com.teasaas.bom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新增/编辑原料入参（F-BOM-01）
 * costPrice 单位：分（红线：金额一律 Integer 存分）
 */
@Data
public class MaterialSaveReq {

    @NotBlank(message = "原料编码不能为空")
    @Size(max = 32, message = "原料编码最长 32 字符")
    private String materialCode;

    @NotBlank(message = "原料名称不能为空")
    @Size(max = 64, message = "原料名称最长 64 字符")
    private String materialName;

    @Size(max = 32, message = "分类最长 32 字符")
    private String category;

    @NotBlank(message = "单位不能为空")
    @Size(max = 8, message = "单位最长 8 字符")
    private String unit;

    /** 预警阈值（数量字段，允许小数） */
    @PositiveOrZero(message = "预警阈值不能为负数")
    private BigDecimal warnThreshold;

    /** 参考成本（分/单位） */
    @NotNull(message = "参考成本不能为空")
    @PositiveOrZero(message = "参考成本不能为负数")
    private Integer costPrice;
}
