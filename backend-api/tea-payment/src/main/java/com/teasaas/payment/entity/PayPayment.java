package com.teasaas.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 支付流水（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("pay_payment")
public class PayPayment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String paymentNo;

    private Long orderId;

    /** 1微信/2储值 */
    private Integer channel;

    private Long amount;

    /** 1待支付/2成功/3失败/4已关闭 */
    private Integer status;

    private String transactionId;

    private LocalDateTime callbackTime;

    private LocalDateTime createTime;
}
