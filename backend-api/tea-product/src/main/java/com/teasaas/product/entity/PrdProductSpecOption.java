package com.teasaas.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 规格选项（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("prd_product_spec_option")
public class PrdProductSpecOption implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long specId;

    private String optionName;

    /** 加价(分) */
    private Integer extraPrice;

    private Integer sort;
}
