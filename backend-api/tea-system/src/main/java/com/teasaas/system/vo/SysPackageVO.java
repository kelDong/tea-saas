package com.teasaas.system.vo;

import lombok.Data;

/**
 * 套餐 VO（F-SYS-01）
 */
@Data
public class SysPackageVO {

    private Long id;

    private String packageCode;

    private String packageName;

    /** 单价（分/店/月）——红线：金额 Integer */
    private Integer pricePerStore;

    /** 门店数上限 */
    private Integer storeLimit;

    /** 功能开关 JSON */
    private String featureJson;

    /** 1启用/0停用 */
    private Integer status;
}
