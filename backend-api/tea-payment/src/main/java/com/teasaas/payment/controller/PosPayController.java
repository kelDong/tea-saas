package com.teasaas.payment.controller;

import com.teasaas.common.result.Result;
import com.teasaas.order.dto.PosPayDTO;
import com.teasaas.payment.service.PayPaymentService;
import com.teasaas.payment.vo.PosPayVO;
import com.teasaas.system.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POS 收款接口（F-ORD-04，接口需求 8.4；R7/R8 名下门店）
 * 1现金含找零 / 2微信Native / 3储值未开放；收款成功即时记账，订单 1→2。
 */
@Validated
@RestController
@RequestMapping("/api/v1/pos/orders")
@RequiredArgsConstructor
@Tag(name = "PAY-POS收款", description = "收银收款确认")
public class PosPayController {

    private final PayPaymentService payPaymentService;

    @OpLog(module = "payment", action = "POS收款确认")
    @Operation(summary = "POS 收款确认（现金找零/微信Native）")
    @PostMapping("/{id}/pay")
    public Result<PosPayVO> pay(
            @Parameter(description = "收银单ID") @PathVariable("id") Long id,
            @RequestBody @Validated PosPayDTO dto) {
        return Result.success(payPaymentService.posPay(id, dto));
    }
}
