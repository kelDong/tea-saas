package com.teasaas.payment.controller;

import com.teasaas.common.result.Result;
import com.teasaas.payment.dto.WxPayCreateDTO;
import com.teasaas.payment.service.PayPaymentService;
import com.teasaas.payment.vo.PaymentVO;
import com.teasaas.system.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * B 端支付接口（F-PAY-01，接口需求 9.1；一期 mock 网关）
 * Native 收款码限 R7/R8 名下门店；流水查询需登录。
 */
@Validated
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "PAY-B端支付", description = "微信Native收款码/流水查询")
public class PaymentController {

    private final PayPaymentService payPaymentService;

    @OpLog(module = "payment", action = "POS微信Native收款")
    @Operation(summary = "微信 Native 收款码（POS 收银单，R7/R8）")
    @PostMapping("/wx-native")
    public Result<PaymentVO> wxNative(@RequestBody @Validated WxPayCreateDTO dto) {
        return Result.success(payPaymentService.wxNative(dto.getOrderId()));
    }

    @Operation(summary = "支付流水查询")
    @GetMapping("/{paymentNo}")
    public Result<PaymentVO> detail(
            @Parameter(description = "支付流水号") @PathVariable("paymentNo") String paymentNo) {
        return Result.success(payPaymentService.getByPaymentNo(paymentNo));
    }
}
