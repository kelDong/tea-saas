package com.teasaas.bom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.teasaas.framework.mybatis.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 原料档案（生成器骨架：业务行为由人工补齐）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bom_material")
public class BomMaterial extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String materialCode;

    private String materialName;

    private String category;

    /** g/kg/个/杯/L */
    private String unit;

    private BigDecimal warnThreshold;

    /** 参考成本(分/单位) */
    private Integer costPrice;

    private Integer status;
}
