package com.teasaas.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会员（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("mem_member")
public class MemMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String openid;

    private String unionid;

    private String nickname;

    private String avatar;

    private String phone;

    private Long levelId;

    private Integer totalPoints;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer delFlag;
}
