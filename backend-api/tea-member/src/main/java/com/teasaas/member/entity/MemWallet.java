package com.teasaas.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 储值账户（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("mem_wallet")
public class MemWallet implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long memberId;

    /** 分 */
    private Long balance;

    private Long totalRecharge;

    private Long totalConsume;

    private LocalDateTime updateTime;
}
