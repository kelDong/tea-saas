package com.teasaas.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会员等级（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("mem_member_level")
public class MemMemberLevel implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String levelName;

    private Integer minPoints;

    /** 95=95折 */
    private Integer discount;

    private LocalDateTime createTime;

    @TableLogic
    private Integer delFlag;
}
