package com.teasaas.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 微信登录入参（F-MEM-01，接口需求 10.1）
 */
@Data
@Schema(description = "微信登录入参")
public class WxLoginDTO {

    @Schema(description = "wx.login 返回的临时凭证（一期 mock：code 即模拟 openid）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "code 不能为空")
    @Size(max = 64)
    private String code;
}
