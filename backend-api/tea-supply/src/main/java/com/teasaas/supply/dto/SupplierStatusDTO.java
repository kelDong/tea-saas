package com.teasaas.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 供应商启停入参（F-SUP-01）
 */
@Data
@Schema(description = "供应商启停入参")
public class SupplierStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "状态：1启用 / 0停用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "status不能为空")
    @Min(value = 0, message = "status仅允许0或1")
    @Max(value = 1, message = "status仅允许0或1")
    private Integer status;
}
