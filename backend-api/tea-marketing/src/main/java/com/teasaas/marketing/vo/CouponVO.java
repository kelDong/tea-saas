package com.teasaas.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户券出参（F-MKT-01，接口需求 11.1；券面额信息由模板带出）
 */
@Data
@Schema(description = "我的券")
public class CouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户券ID")
    private Long id;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "类型：1满减/2折扣")
    private Integer type;

    @Schema(description = "门槛（分）")
    private Long threshold;

    @Schema(description = "满减额（分）")
    private Long amount;

    @Schema(description = "折扣率")
    private BigDecimal discount;

    @Schema(description = "状态：1未用/2已用/3过期")
    private Integer status;

    @Schema(description = "有效期起")
    private LocalDateTime validStart;

    @Schema(description = "有效期止")
    private LocalDateTime validEnd;
}
