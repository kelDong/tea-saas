package com.teasaas.franchise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 加盟商开通账号入参（F-FRA-01；密码策略复用 SysUserService 既有逻辑）
 */
@Data
public class FranchiseeAccountReq {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名最长 32 字符")
    private String username;

    @Size(max = 64, message = "昵称最长 64 字符")
    private String nickname;

    /** 可选；缺省时使用初始默认密码 */
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;
}
