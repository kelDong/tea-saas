package com.teasaas.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 储值记录（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("mem_recharge_record")
public class MemRechargeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long memberId;

    private Long amount;

    /** 赠送(分) */
    private Long bonus;

    private Long balanceAfter;

    private String bizNo;

    private LocalDateTime createTime;
}
