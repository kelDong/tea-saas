package com.teasaas.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志（无 del_flag）
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long tenantId;
    private Long userId;
    private String module;
    private String action;
    private String method;
    private String params;
    private String ip;
    private Integer costMs;
    /** 1成功/0失败 */
    private Integer status;
    private LocalDateTime createTime;
}
