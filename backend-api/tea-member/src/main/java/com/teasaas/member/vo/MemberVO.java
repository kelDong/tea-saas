package com.teasaas.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员信息出参（F-MEM-01，接口需求 10.1）
 */
@Data
@Schema(description = "会员信息")
public class MemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会员ID")
    private Long id;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "会员等级名（等级自动化二期，一期回显配置等级）")
    private String levelName;

    @Schema(description = "累计获得积分")
    private Integer totalPoints;

    @Schema(description = "可用积分（流水实时汇总）")
    private Integer availablePoints;

    @Schema(description = "储值余额(分)")
    private Long walletBalance;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;
}
