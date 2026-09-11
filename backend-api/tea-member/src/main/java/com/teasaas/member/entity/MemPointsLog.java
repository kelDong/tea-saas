package com.teasaas.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 积分流水（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("mem_points_log")
public class MemPointsLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long memberId;

    private Integer changePoints;

    private Integer beforePoints;

    private Integer afterPoints;

    /** 1消费得/2抵现扣/3赠送/4过期扣 */
    private Integer bizType;

    private String bizNo;

    private LocalDateTime createTime;
}
