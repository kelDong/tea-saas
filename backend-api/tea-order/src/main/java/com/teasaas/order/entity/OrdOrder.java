package com.teasaas.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.teasaas.framework.mybatis.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单（生成器骨架：业务行为由人工补齐）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ord_order")
public class OrdOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long storeId;

    /** 门店收银可空 */
    private Long memberId;

    /** 1小程序/2门店收银 */
    private Integer orderSource;

    /** 1待支付/2已支付/3制作中/4待取餐/5已完成/6已取消/7已退款 */
    private Integer status;

    private Long totalAmount;

    private Long discountAmount;

    /** 实付(分) */
    private Long payAmount;

    private Long couponId;

    private Long couponAmount;

    private Integer pointsUsed;

    private Long pointsAmount;

    /** 储值支付(分) */
    private Long balanceAmount;

    /** 1微信/2储值/3现金 */
    private Integer payChannel;

    private String takeCode;

    private LocalDateTime payTime;

    private LocalDateTime finishTime;

    private String cancelReason;

    private String remark;
}
