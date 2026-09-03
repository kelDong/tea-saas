package com.teasaas.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 套餐（平台表：无 tenant_id）
 */
@Data
@TableName("sys_package")
public class SysPackage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String packageCode;
    private String packageName;
    /** 单价（分/店/月）——红线：金额 Integer */
    private Integer pricePerStore;
    private Integer storeLimit;
    private String featureJson;
    private Integer status;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    private Integer delFlag;
}
