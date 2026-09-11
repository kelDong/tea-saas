package com.teasaas.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付流水出参（接口需求 9.1）：统一下单返回 prepay 参数 / 查询状态共用
 */
@Data
@Schema(description = "支付流水")
public class PaymentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "支付流水号")
    private String paymentNo;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "渠道：1微信/2储值/3现金")
    private Integer channel;

    @Schema(description = "金额(分)")
    private Long amount;

    @Schema(description = "状态：1待支付/2成功/3失败/4已关闭")
    private Integer status;

    @Schema(description = "第三方交易号")
    private String transactionId;

    @Schema(description = "回调时间")
    private LocalDateTime callbackTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "JSAPI 拉起支付参数（mock 模式为模拟值）")
    private Map<String, String> payParams;

    @Schema(description = "Native 收款码链接（mock 模式为模拟值）")
    private String codeUrl;
}
