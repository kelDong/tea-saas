package com.teasaas.bom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 停用/启用入参（F-BOM-01 / F-BOM-02 通用）
 * status：0 停用 / 1 启用
 */
@Data
public class StatusUpdateReq {

    @NotNull(message = "status 不能为空")
    @Min(value = 0, message = "status 仅允许 0(停用)/1(启用)")
    @Max(value = 1, message = "status 仅允许 0(停用)/1(启用)")
    private Integer status;
}
