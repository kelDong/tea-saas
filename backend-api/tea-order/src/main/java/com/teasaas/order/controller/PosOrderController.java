package com.teasaas.order.controller;

import com.teasaas.common.result.Result;
import com.teasaas.order.dto.PosOrderCreateDTO;
import com.teasaas.order.service.OrdOrderService;
import com.teasaas.order.vo.OrderCreateVO;
import com.teasaas.system.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门店手工收银（F-ORD-04，接口需求 8.4，P0-1 决策）
 * 下单：R7/R8 名下门店（order_source=2，顾客免登录）；收款确认在 tea-payment
 * PosPayController（POST /api/v1/pos/orders/{id}/pay，支付流水归支付模块）。
 */
@Validated
@RestController
@RequestMapping("/api/v1/pos/orders")
@RequiredArgsConstructor
@Tag(name = "ORDER-POS收银", description = "门店手工下单")
public class PosOrderController {

    private final OrdOrderService ordOrderService;

    @OpLog(module = "order", action = "POS手工下单")
    @Operation(summary = "手工下单（R7/R8，下单即扣库存）")
    @PostMapping
    public Result<OrderCreateVO> create(@RequestBody @Validated PosOrderCreateDTO dto) {
        return Result.success(ordOrderService.posCreate(dto));
    }
}
