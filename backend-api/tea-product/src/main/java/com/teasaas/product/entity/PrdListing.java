package com.teasaas.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.teasaas.framework.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 门店上架（生成器骨架：业务行为由人工补齐）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prd_listing")
public class PrdListing extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long storeId;

    private Long productId;

    /** 本店售价(分) */
    private Integer salePrice;

    private Integer sort;

    /** 1上架/0下架 */
    private Integer status;
}
