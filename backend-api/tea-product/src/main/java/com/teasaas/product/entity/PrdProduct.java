package com.teasaas.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.teasaas.framework.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品（生成器骨架：业务行为由人工补齐）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prd_product")
public class PrdProduct extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long categoryId;

    private String productCode;

    private String productName;

    private Long formulaId;

    /** 建议售价(分) */
    private Integer suggestPrice;

    private String image;

    private String description;

    /** 1已发布/0草稿 */
    private Integer publishStatus;

    private Integer status;
}
