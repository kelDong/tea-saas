package com.teasaas.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 微信登录出参（F-MEM-01）
 */
@Data
@Schema(description = "微信登录出参")
public class WxLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会员ID（后续 C 端请求经 X-Member-Id 携带）")
    private Long memberId;

    @Schema(description = "C 端会话 token（后续请求经 Authorization: Bearer <token> 携带；兼容期也可用 X-Member-Id 头）")
    private String token;

    @Schema(description = "是否新建档")
    private Boolean isNew;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;
}
