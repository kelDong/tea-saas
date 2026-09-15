package com.teasaas.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 品牌租户 VO（F-SYS-01）
 */
@Data
public class TenantVO {

    private Long id;

    private String tenantCode;

    private String tenantName;

    private String contactName;

    private String contactPhone;

    private String packageCode;

    /** 门店数上限（0=不限） */
    private Integer storeLimit;

    /** 1启用/0停用 */
    private Integer status;

    /** 到期时间（yyyy-MM-dd HH:mm:ss） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
