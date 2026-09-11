package com.teasaas.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 退款流水（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("pay_refund")
public class PayRefund implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String refundNo;

    private Long orderId;

    private Long paymentId;

    /** 整单退=实付 */
    private Long amount;

    /** 1微信原路/2退储值 */
    private Integer refundType;

    /** 券不退还=0 */
    private Integer couponReturned;

    private String reason;

    /** 1处理中/2成功/3失败 */
    private Integer status;

    private LocalDateTime refundTime;

    private LocalDateTime createTime;
}
