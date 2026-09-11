package com.teasaas.payment.controller;

import com.teasaas.payment.dto.PayCallbackDTO;
import com.teasaas.payment.service.PayPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付回调接口（P1-3，接口需求 9.1；白名单公开，一期 mock 网关）
 *
 * <p>网关对账应答约定返回明文 SUCCESS/FAIL（非 Result 包装）：
 * 业务校验失败抛 BizException 时全局异常兜底返回错误 JSON，
 * 微信侧会按非 SUCCESS 继续重推；处理失败但已落 pay_callback_task
 * 的场景仍应答 SUCCESS（本地定时重试兜底，见 PayPaymentServiceImpl）。
 * 验签（正式模式微信 V3 平台证书）待资质下发后接入。</p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
@Tag(name = "PAY-支付回调", description = "微信支付异步通知")
public class NotifyController {

    private final PayPaymentService payPaymentService;

    @Operation(summary = "微信支付回调（mock 验签跳过）")
    @PostMapping("/wx-pay/callback")
    public String wxPayCallback(@RequestBody PayCallbackDTO dto) {
        log.info("收到支付回调 paymentNo={} transactionId={}", dto.getPaymentNo(), dto.getTransactionId());
        return payPaymentService.handleCallback(dto);
    }
}
