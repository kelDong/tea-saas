package com.teasaas.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 积分/储值规则配置入参（F-MEM-02/03，接口需求 10.2/10.3 可配置项）
 */
@Data
@Schema(description = "积分/储值规则配置入参")
public class PointsConfigDTO {

    @Schema(description = "消费1元得N积分", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Min(value = 0, message = "积分比例不能为负")
    @Max(value = 1000)
    private Integer pointsPerYuan;

    @Schema(description = "1积分抵N分（分）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Min(value = 1, message = "抵现比例至少1分/积分")
    @Max(value = 1000)
    private Integer pointValue;

    @Schema(description = "充值赠送千分比（如100=充1000送100）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Min(value = 0, message = "赠送比例不能为负")
    @Max(value = 1000)
    private Integer rechargeBonusPermille;
}
