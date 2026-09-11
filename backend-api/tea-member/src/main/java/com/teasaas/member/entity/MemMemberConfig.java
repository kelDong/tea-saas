package com.teasaas.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会员规则配置(积分/储值,接口需求10.2/10.3可配置项)（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("mem_member_config")
public class MemMemberConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 消费1元得N积分 */
    private Integer pointsPerYuan;

    /** 1积分抵N分(分) */
    private Integer pointValue;

    /** 充值赠送千分比 */
    private Integer rechargeBonusPermille;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
