package com.teasaas.franchise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.teasaas.framework.mybatis.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 门店（生成器骨架：业务行为由人工补齐）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fra_store")
public class FraStore extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String storeCode;

    private String storeName;

    /** NULL=直营 */
    private Long franchiseeId;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    /** 1营业/0打烊 */
    private Integer businessStatus;

    private LocalTime openTime;

    private LocalTime closeTime;

    private Integer status;
}
