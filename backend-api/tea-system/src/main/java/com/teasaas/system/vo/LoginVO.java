package com.teasaas.system.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录后返回（token + 用户信息；前端随后拉 /auth/menus）
 */
@Data
@Builder
public class LoginVO {

    private String token;
    private String tokenName;
    private Long userId;
    private Long tenantId;
    private String nickname;
    private Integer userType;
    private List<String> roles;
}
