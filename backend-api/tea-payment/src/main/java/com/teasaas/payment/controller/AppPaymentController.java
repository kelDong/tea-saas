package com.teasaas.payment.controller;

import com.teasaas.common.result.Result;
import com.teasaas.payment.dto.WxPayCreateDTO;
import com.teasaas.payment.service.PayPaymentService;
import com.teasaas.payment.vo.PaymentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端支付接口（F-PAY-01，接口需求 9.1；一期 mock 网关）
 *
 * <p>身份与会话（Review §2 会话化）：AppMemberInterceptor 解析
 * Authorization: Bearer token（兼容期 X-Member-Id 双头 WARN 放行）注入
 * MemberContext + TenantContext，本类不再手动解析请求头；
 * 请求结束由拦截器统一清理 ThreadLocal。</p>
 */
@Validated
@RestController
@RequestMapping("/api/app/v1/payments")
@RequiredArgsConstructor
@Tag(name = "PAY-C端支付", description = "微信JSAPI统一下单")
public class AppPaymentController {

    private final PayPaymentService payPaymentService;

    @Operation(summary = "微信 JSAPI 统一下单（小程序待支付单）")
    @PostMapping("/wx-jsapi")
    public Result<PaymentVO> wxJsapi(@RequestBody @Validated WxPayCreateDTO dto) {
        return Result.success(payPaymentService.wxJsapi(dto.getOrderId()));
    }
}
