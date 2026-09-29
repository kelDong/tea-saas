package com.teasaas.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * POS 会员搜索出参（F-ORD-04 二期，接口需求 8.4；收款选会员用）
 *
 * <p>最小字段集：不透出 openid 等敏感字段；balance 供收银员预判储值可扣。</p>
 */
@Data
@Schema(description = "POS会员搜索结果")
public class PosMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会员ID")
    private Long id;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "储值余额（分）")
    private Long balance;
}
