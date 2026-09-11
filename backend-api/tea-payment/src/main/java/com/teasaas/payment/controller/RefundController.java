package com.teasaas.payment.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.payment.dto.RefundApplyDTO;
import com.teasaas.payment.entity.PayRefund;
import com.teasaas.payment.service.PayRefundService;
import com.teasaas.payment.vo.RefundVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款接口（F-PAY-02，接口需求 9.2，P0-5 决策：一期仅整单退）
 * R2 全租户 / R6/R7 名下门店；R8 无退款权限。
 */
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "PAY-退款", description = "整单退款/退款列表")
public class RefundController {

    private final PayRefundService payRefundService;

    @OpLog(module = "payment", action = "整单退款")
    @Operation(summary = "申请退款（整单退=实付，回补库存）")
    @PostMapping("/api/v1/orders/{id}/refund")
    public Result<RefundVO> apply(
            @Parameter(description = "订单ID") @PathVariable("id") Long id,
            @RequestBody @Validated RefundApplyDTO dto) {
        return Result.success(payRefundService.apply(id, dto));
    }

    @Operation(summary = "退款列表（R2 全部 / R6/R7 名下门店）")
    @GetMapping("/api/v1/refunds")
    public Result<PageResult<PayRefund>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        return Result.success(payRefundService.page(page, size));
    }
}
