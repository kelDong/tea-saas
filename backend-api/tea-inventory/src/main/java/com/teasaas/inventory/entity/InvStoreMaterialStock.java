package com.teasaas.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 门店原料库存（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("inv_store_material_stock")
public class InvStoreMaterialStock implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long materialId;

    /** 禁止负值 */
    private BigDecimal stockQty;

    /** 乐观锁 */
    private Integer version;

    private Long updateBy;

    private LocalDateTime updateTime;
}
