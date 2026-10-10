package com.teasaas.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 支付回调报文（接口需求 9.1；双协议统一内部结构）
 *
 * <p>mock=true（默认，资质 t100011 未下发）：联调方直发明文 JSON；
 * mock=false：微信 V3 加密报文经 NotifyController/WxPayClient 验签解密为 Transaction
 * 后映射回本结构（tradeState.name() → resultCode，金额天然为分），
 * 复用幂等/金额核对/任务重试链路。</p>
 */
@Data
@Schema(description = "支付回调报文（mock）")
public class PayCallbackDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "支付流水号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "paymentNo不能为空")
    private String paymentNo;

    @Schema(description = "第三方交易号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "transactionId不能为空")
    private String transactionId;

    @Schema(description = "结果码：SUCCESS 或 V3 tradeState（CLOSED/PAYERROR 等）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "resultCode不能为空")
    private String resultCode;

    @Schema(description = "遗留字段：V3 验签已前置到 Controller，不再校验")
    private String sign;

    @NotNull(message = "回调金额不能为空")
    @Schema(description = "实付金额(分)，与流水不符将拒单", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long amount;
}
