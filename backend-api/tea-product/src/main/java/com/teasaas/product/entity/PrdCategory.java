package com.teasaas.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商品分类（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("prd_category")
public class PrdCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String categoryName;

    private Long parentId;

    private Integer sort;

    private LocalDateTime createTime;

    @TableLogic
    private Integer delFlag;
}
