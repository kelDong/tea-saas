package com.teasaas.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 支付回调报文（接口需求 9.1；mock 模式下的简化结构）
 *
 * <p>一期微信商户资质未下（飞书 t100011），网关走 mock（tea.pay.mock=true）：
 * 验签跳过、交易号由调用方给出；正式接入后替换为微信 V3 回调验签结构。</p>
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

    @Schema(description = "结果码：SUCCESS/FAIL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "resultCode不能为空")
    private String resultCode;

    @Schema(description = "回调签名（mock 模式可空；正式模式必填）")
    private String sign;

    @NotNull(message = "回调金额不能为空")
    @Schema(description = "实付金额(分)，与流水不符将拒单", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long amount;
}
