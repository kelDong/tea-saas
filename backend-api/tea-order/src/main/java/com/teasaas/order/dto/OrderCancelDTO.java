package com.teasaas.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * B 端取消订单入参（接口需求 8.2；仅待支付单可取消，PRD 5.3：R6/R7 有取消钮、R8 无）
 */
@Data
@Schema(description = "B 端取消订单入参")
public class OrderCancelDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "取消原因（空则默认「门店取消」）")
    @Size(max = 128, message = "取消原因最长128字")
    private String reason;
}
