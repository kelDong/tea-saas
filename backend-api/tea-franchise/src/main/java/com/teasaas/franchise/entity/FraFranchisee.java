package com.teasaas.franchise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.teasaas.framework.mybatis.entity.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 加盟商（生成器骨架：业务行为由人工补齐）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fra_franchisee")
public class FraFranchisee extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String franchiseeCode;

    private String franchiseeName;

    private String contactName;

    private String contactPhone;

    /** A/B/C */
    private String levelCode;

    private LocalDate joinDate;

    /** 1合作中/0已终止 */
    private Integer status;
}
