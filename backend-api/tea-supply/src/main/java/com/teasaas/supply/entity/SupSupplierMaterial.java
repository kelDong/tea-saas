package com.teasaas.supply.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 供应商可供应原料（生成器骨架：业务行为由人工补齐）
 */
@Data
@TableName("sup_supplier_material")
public class SupSupplierMaterial implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long supplierId;

    private Long materialId;

    /** 参考供货价(分) */
    private Integer supplyPrice;

    private LocalDateTime createTime;
}
