package com.teasaas.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 券适用范围（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("mkt_coupon_scope")
public class MktCouponScope implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long couponId;

    /** 1全场/2指定商品/3指定门店 */
    private Integer scopeType;

    private Long targetId;
}
