package com.teasaas.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 后台会员资产汇总出参（F-MEM-01，接口需求 10.1 二期补缺）
 */
@Data
@Schema(description = "会员资产汇总")
public class MemberAssetsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "储值余额(分)")
    private Long balance;

    @Schema(description = "累计充值(分，不含赠送)")
    private Long totalRecharge;

    @Schema(description = "累计消费(分)")
    private Long totalConsume;

    @Schema(description = "可用积分(SUM流水)")
    private Integer availablePoints;

    @Schema(description = "累计获得积分")
    private Integer totalPoints;
}
