package com.teasaas.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 微信统一下单入参（JSAPI/Native 共用，接口需求 9.1）
 */
@Data
@Schema(description = "微信统一下单入参")
public class WxPayCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "orderId不能为空")
    private Long orderId;
}
