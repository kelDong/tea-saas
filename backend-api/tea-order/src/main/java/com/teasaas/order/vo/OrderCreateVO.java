package com.teasaas.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 下单出参（接口需求 8.1）：C 端/POS 共用
 */
@Data
@Schema(description = "下单结果")
public class OrderCreateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "取餐码")
    private String takeCode;

    @Schema(description = "实付金额(分)")
    private Long payAmount;
}
