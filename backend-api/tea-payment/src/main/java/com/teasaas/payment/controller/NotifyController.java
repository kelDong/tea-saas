package com.teasaas.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.payment.config.PayProperties;
import com.teasaas.payment.dto.PayCallbackDTO;
import com.teasaas.payment.service.PayPaymentService;
import com.teasaas.payment.wx.WxPayClient;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付回调接口（P1-3，接口需求 9.1；白名单公开）
 *
 * <p>网关对账应答约定返回明文 SUCCESS/FAIL（非 Result 包装）：
 * 业务校验失败抛 BizException 时全局异常兜底返回错误 JSON，
 * 微信侧会按非 SUCCESS 继续重推；处理失败但已落 pay_callback_task
 * 的场景仍应答 SUCCESS（本地定时重试兜底，见 PayPaymentServiceImpl）。</p>
 *
 * <p>双协议分派（tea.pay.mock 开关）：
 * mock=true 收明文 JSON（PayCallbackDTO，联调/测试直连）；
 * mock=false 收微信 V3 加密报文——Wechatpay-* 四请求头 + AES-GCM 密文体，
 * 经 WxPayClient 验签解密为 Transaction 后映射回 PayCallbackDTO，
 * 复用既有幂等/金额核对/任务重试链路。</p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
@Tag(name = "PAY-支付回调", description = "微信支付异步通知")
public class NotifyController {

    private final PayPaymentService payPaymentService;
    private final PayProperties payProperties;
    private final ObjectMapper objectMapper;
    /** mock=true 时 WxPayClient 不装配，必须 ObjectProvider 可空注入 */
    private final ObjectProvider<WxPayClient> wxPayClientProvider;

    @Operation(summary = "微信支付回调（mock 明文 / V3 验签解密）")
    @PostMapping("/wx-pay/callback")
    public String wxPayCallback(
            @RequestBody String body,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp) {
        PayCallbackDTO dto;
        if (payProperties.isMock()) {
            dto = parseMockBody(body);
        } else {
            WxPayClient client = wxPayClientProvider.getIfAvailable();
            if (client == null) {
                throw new BizException(ErrorCode.SYSTEM_ERROR, "真实支付客户端未装配");
            }
            dto = toCallbackDTO(client.parsePayNotify(body, serial, nonce, signature, timestamp));
        }
        log.info("收到支付回调 paymentNo={} transactionId={} state={}",
                dto.getPaymentNo(), dto.getTransactionId(), dto.getResultCode());
        return payPaymentService.handleCallback(dto);
    }

    /** mock 直连报文（明文 JSON） */
    private PayCallbackDTO parseMockBody(String body) {
        try {
            return objectMapper.readValue(body, PayCallbackDTO.class);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "回调报文格式错误");
        }
    }

    /** V3 解密报文 → 内部回调 DTO（tradeState 即 resultCode；金额 V3 天然为分） */
    private PayCallbackDTO toCallbackDTO(Transaction tx) {
        PayCallbackDTO dto = new PayCallbackDTO();
        dto.setPaymentNo(tx.getOutTradeNo());
        dto.setTransactionId(tx.getTransactionId());
        dto.setResultCode(tx.getTradeState() == null ? null : tx.getTradeState().name());
        dto.setAmount(tx.getAmount() == null || tx.getAmount().getTotal() == null
                ? null : Long.valueOf(tx.getAmount().getTotal()));
        return dto;
    }
}
