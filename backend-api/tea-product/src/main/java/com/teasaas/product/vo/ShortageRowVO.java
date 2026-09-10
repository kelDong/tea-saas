package com.teasaas.product.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 缺料明细行（Mapper 查询投影，F-PRD-03 可售校验内部用）
 */
@Data
public class ShortageRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long listingId;

    private Long materialId;

    private String materialName;

    private String unit;

    private BigDecimal needQty;

    private BigDecimal stockQty;
}
