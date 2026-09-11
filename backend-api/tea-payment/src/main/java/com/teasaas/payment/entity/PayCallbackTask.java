package com.teasaas.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 支付回调重试任务（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("pay_callback_task")
public class PayCallbackTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String taskNo;

    /** 1支付/2退款 */
    private Integer bizType;

    private String bizNo;

    private String payload;

    private Integer retryCount;

    private Integer maxRetry;

    /** 1待处理/2成功/3失败终态 */
    private Integer status;

    private LocalDateTime nextRetryTime;

    private String lastError;

    private LocalDateTime createTime;
}
