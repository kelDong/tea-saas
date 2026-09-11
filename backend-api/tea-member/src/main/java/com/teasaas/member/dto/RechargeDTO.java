package com.teasaas.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 储值充值入参（F-MEM-03，接口需求 10.3，P0-4 决策）
 */
@Data
@Schema(description = "储值充值入参")
public class RechargeDTO {

    @Schema(description = "充值金额(分)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Min(value = 100, message = "单笔充值不低于1元")
    @Max(value = 100000000, message = "单笔充值超出上限")
    private Long amount;
}
