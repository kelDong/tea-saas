package com.teasaas.bom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 配方明细（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("bom_formula_item")
public class BomFormulaItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long formulaId;

    private Long materialId;

    /** 用量 */
    private BigDecimal qty;

    private Integer sort;

    private LocalDateTime createTime;
}
