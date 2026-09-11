package com.teasaas.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 订单明细（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("ord_order_item")
public class OrdOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long orderId;

    private Long productId;

    private Long listingId;

    /** 快照 */
    private String productName;

    /** 规格快照 */
    private String specDesc;

    /** 成交单价(分) */
    private Integer price;

    private Integer qty;

    private Long amount;

    /** 扣库存配方快照 */
    private Long formulaId;

    private LocalDateTime createTime;
}
