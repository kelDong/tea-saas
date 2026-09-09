package com.teasaas.supply.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 分级价格（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("sup_tier_price")
public class SupTierPrice implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long materialId;

    /** A/B/C */
    private String levelCode;

    /** 单价(分) */
    private Integer price;

    private Long updateBy;

    private LocalDateTime updateTime;
}
