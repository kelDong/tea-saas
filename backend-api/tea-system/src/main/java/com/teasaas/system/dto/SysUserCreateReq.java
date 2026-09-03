package com.teasaas.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建用户入参（F-SYS-02）
 */
@Data
public class SysUserCreateReq {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名最长 32 字符")
    private String username;

    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;

    private String nickname;

    private String phone;

    @NotEmpty(message = "至少分配一个角色")
    private List<Long> roleIds;

    /** 绑定门店（加盟商/门店员工必填，总部员工可空） */
    private List<Long> storeIds;
}
