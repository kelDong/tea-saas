package com.teasaas.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 券模板入参（F-MKT-01，接口需求 11.1）
 *
 * <p>金额字段一律"分"（Long，红线 1）；discount 为折扣率（0.90，
 * 库设计 DECIMAL(3,2) 权威约定），非金额不适用红线 1。</p>
 */
@Data
@Schema(description = "券模板创建/编辑入参")
public class CouponTemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板名称")
    @NotBlank
    @Size(max = 32)
    private String name;

    @Schema(description = "类型：1满减/2折扣")
    @NotNull
    @Min(1)
    @Max(2)
    private Integer type;

    @Schema(description = "门槛（分，满X可用；null/0 无门槛）")
    @Min(0)
    private Long threshold;

    @Schema(description = "满减额（分，type=1 必填）")
    @Min(1)
    private Long amount;

    @Schema(description = "折扣率（type=2 必填，如 0.90）")
    @DecimalMin(value = "0.01")
    @DecimalMax(value = "0.99")
    private BigDecimal discount;

    @Schema(description = "发放总量（-1 不限）")
    @Min(-1)
    private Integer totalQty;

    @Schema(description = "每人限领")
    @Min(1)
    private Integer perLimit;

    @Schema(description = "有效期类型：1固定时段/2领取后N天")
    @NotNull
    @Min(1)
    @Max(2)
    private Integer validType;

    @Schema(description = "固定时段起（validType=1 必填）")
    private LocalDateTime validStart;

    @Schema(description = "固定时段止（validType=1 必填）")
    private LocalDateTime validEnd;

    @Schema(description = "领取后有效天数（validType=2 必填）")
    @Min(1)
    private Integer validDays;

    @Schema(description = "状态：1启用/2停用（编辑接口可停用）")
    @Min(1)
    @Max(2)
    private Integer status;
}
