package com.teasaas.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 分级价格项（F-SUP-02）
 */
@Data
@Schema(description = "分级价格项")
public class TierPriceItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原料ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "materialId不能为空")
    private Long materialId;

    @Schema(description = "加盟商等级：A/B/C", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "levelCode不能为空")
    @Pattern(regexp = "^[ABC]$", message = "levelCode必须为A/B/C")
    @Size(max = 8)
    private String levelCode;

    @Schema(description = "单价(分)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "price不能为空")
    @Positive(message = "价格必须大于0")
    private Integer price;
}
