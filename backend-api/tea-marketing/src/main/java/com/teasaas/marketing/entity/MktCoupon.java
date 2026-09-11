package com.teasaas.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户券（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("mkt_coupon")
public class MktCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long templateId;

    private Long memberId;

    /** 1未用/2已用/3过期 */
    private Integer status;

    private Long useOrderId;

    private LocalDateTime validStart;

    private LocalDateTime validEnd;

    private LocalDateTime receiveTime;

    private LocalDateTime useTime;
}
