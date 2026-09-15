package com.teasaas.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建品牌入参（F-SYS-01，接口需求 2.2）
 */
@Data
public class TenantCreateReq {

    @NotBlank(message = "品牌编码不能为空")
    @Size(max = 32, message = "品牌编码最长 32 字符")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "品牌编码仅限字母/数字/下划线/中划线")
    private String tenantCode;

    @NotBlank(message = "品牌名称不能为空")
    @Size(max = 64, message = "品牌名称最长 64 字符")
    private String tenantName;

    @Size(max = 32, message = "联系人最长 32 字符")
    private String contactName;

    @Size(max = 20, message = "联系电话最长 20 字符")
    private String contactPhone;

    /** 套餐编码（可空=未选套餐） */
    private String packageCode;

    /** 门店数上限（0=不限） */
    private Integer storeLimit;

    /** 到期时间（yyyy-MM-dd HH:mm:ss） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
