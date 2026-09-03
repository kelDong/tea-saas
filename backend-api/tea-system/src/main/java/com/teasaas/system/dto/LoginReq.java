package com.teasaas.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参（F-SYS-03）
 */
@Data
public class LoginReq {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码 key（一期可选：为空则跳过校验） */
    private String captchaKey;

    /** 验证码 code */
    private String captchaCode;
}
