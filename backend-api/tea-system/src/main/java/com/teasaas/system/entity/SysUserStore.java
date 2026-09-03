package com.teasaas.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-门店绑定（数据权限"名下门店/本店"依据；一人多店）
 */
@Data
@TableName("sys_user_store")
public class SysUserStore implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long userId;
    private Long storeId;
    private Integer isDefault;
    private LocalDateTime createTime;
}
