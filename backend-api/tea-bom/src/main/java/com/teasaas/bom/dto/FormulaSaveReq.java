package com.teasaas.bom.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增/编辑配方入参（F-BOM-02）
 * 明细在服务端"全量重绑"：先 delete by formulaId 再批量 insert
 */
@Data
public class FormulaSaveReq {

    @NotBlank(message = "配方编码不能为空")
    @Size(max = 32, message = "配方编码最长 32 字符")
    private String formulaCode;

    @NotBlank(message = "配方名称不能为空")
    @Size(max = 64, message = "配方名称最长 64 字符")
    private String formulaName;

    @NotEmpty(message = "配方明细不能为空")
    @Valid
    private List<FormulaItemReq> items;

    /**
     * 配方明细行
     */
    @Data
    public static class FormulaItemReq {

        @NotNull(message = "原料 ID 不能为空")
        private Long materialId;

        @NotNull(message = "用量不能为空")
        @DecimalMin(value = "0.001", message = "用量必须大于 0")
        private BigDecimal qty;

        /** 排序号（可空，默认 0） */
        private Integer sort;
    }
}
