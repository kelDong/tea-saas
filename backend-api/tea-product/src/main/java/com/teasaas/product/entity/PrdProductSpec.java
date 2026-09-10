package com.teasaas.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 商品规格组（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("prd_product_spec")
public class PrdProductSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long productId;

    /** 温度/糖度 */
    private String specName;

    private Integer isRequired;

    private Integer sort;
}
