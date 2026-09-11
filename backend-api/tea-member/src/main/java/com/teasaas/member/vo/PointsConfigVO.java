package com.teasaas.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 积分/储值规则配置出参（F-MEM-02/03，接口需求 10.2/10.3）
 */
@Data
@Schema(description = "积分/储值规则配置")
public class PointsConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "消费1元得N积分")
    private Integer pointsPerYuan;

    @Schema(description = "1积分抵N分（分）")
    private Integer pointValue;

    @Schema(description = "充值赠送千分比")
    private Integer rechargeBonusPermille;
}
