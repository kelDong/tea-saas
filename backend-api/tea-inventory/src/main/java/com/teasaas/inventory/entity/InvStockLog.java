package com.teasaas.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 库存变动流水（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("inv_stock_log")
public class InvStockLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long materialId;

    /** 入正出负 */
    private BigDecimal changeQty;

    private BigDecimal beforeQty;

    private BigDecimal afterQty;

    /** 1下单扣/2超时回补/3退款回补/4采购入库/5盘点 */
    private Integer bizType;

    private String bizNo;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;
}
