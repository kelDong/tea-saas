package com.teasaas.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 券模板（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("mkt_coupon_template")
public class MktCouponTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String name;

    /** 1满减/2折扣 */
    private Integer type;

    /** 满X分 */
    private Long threshold;

    /** 减Y分 */
    private Long amount;

    /** 0.90 */
    private BigDecimal discount;

    /** -1不限 */
    private Integer totalQty;

    private Integer perLimit;

    /** 1固定/2领取后N天 */
    private Integer validType;

    private LocalDateTime validStart;

    private LocalDateTime validEnd;

    private Integer validDays;

    private Integer status;

    private Long createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer delFlag;
}
