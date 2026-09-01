package com.teasaas.framework.mybatis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类（数据库设计 1.1 通用字段）
 * 所有业务表实体必须继承本类：tenant_id / 审计字段 / 逻辑删除
 */
@Data
public abstract class BaseEntity implements Serializable {

    /** 租户 ID（平台超级租户 = 0） */
    @TableField("tenant_id")
    private Long tenantId;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0 正常 / 1 已删 */
    @TableLogic
    @TableField("del_flag")
    private Integer delFlag;
}
