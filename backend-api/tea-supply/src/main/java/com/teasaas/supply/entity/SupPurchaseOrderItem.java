package com.teasaas.supply.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 采购单明细（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("sup_purchase_order_item")
public class SupPurchaseOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long orderId;

    private Long materialId;

    private BigDecimal qty;

    /** 成交单价快照(分) */
    private Integer unitPrice;

    /** 小计(分) */
    private Long amount;
}
