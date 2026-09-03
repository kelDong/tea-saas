package com.teasaas.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 编辑用户入参（角色/门店重新绑定；密码走独立接口）
 */
@Data
public class SysUserUpdateReq {

    private String nickname;

    private String phone;

    /** 1启用/0停用 */
    private Integer status;

    private List<Long> roleIds;

    private List<Long> storeIds;
}
