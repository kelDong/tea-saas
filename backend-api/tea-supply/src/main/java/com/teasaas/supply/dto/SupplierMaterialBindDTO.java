package com.teasaas.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 供应商关联可供应原料入参（F-SUP-01，全量重绑）
 */
@Data
@Schema(description = "供应商关联可供应原料入参（全量重绑）")
public class SupplierMaterialBindDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "可供应原料列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "原料列表不能为空")
    @Valid
    private List<SupplierMaterialItemDTO> items;
}
