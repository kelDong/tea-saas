package com.teasaas.system.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户列表/详情 VO
 */
@Data
public class SysUserVO {

    private Long id;
    private Long tenantId;
    private String username;
    private String nickname;
    private String phone;
    private Integer userType;
    private Integer status;
    private List<Long> roleIds;
    private List<String> roleNames;
    private List<Long> storeIds;
    private LocalDateTime createTime;
}
