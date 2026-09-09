package com.teasaas.supply.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.teasaas.framework.mybatis.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 采购单（生成器骨架：业务行为由人工补齐）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sup_purchase_order")
public class SupPurchaseOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long storeId;

    private Long franchiseeId;

    /** 计价等级快照 */
    private String levelCode;

    /** 1待确认/2已确认/3已发货/4已入库/5已取消 */
    private Integer status;

    /** 总额(分) */
    private Long totalAmount;

    private Long confirmBy;

    private Long shipBy;

    private Long receiveBy;

    private LocalDateTime confirmTime;

    private LocalDateTime shipTime;

    private LocalDateTime receiveTime;

    private String remark;
}
