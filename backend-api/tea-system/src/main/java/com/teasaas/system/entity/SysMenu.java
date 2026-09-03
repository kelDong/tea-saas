package com.teasaas.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单/按钮（平台共享表：无 tenant_id/del_flag）
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long parentId;
    private String menuName;
    /** 1目录/2菜单/3按钮 */
    private Integer menuType;
    private String path;
    private String component;
    /** 权限码 bom:edit（PRD 5.3） */
    private String perm;
    private Integer sort;
    private Integer visible;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
