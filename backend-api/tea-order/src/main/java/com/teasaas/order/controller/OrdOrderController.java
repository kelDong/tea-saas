package com.teasaas.order.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.order.entity.OrdOrder;
import com.teasaas.order.service.OrdOrderService;
import com.teasaas.order.vo.OrderDetailVO;
import com.teasaas.system.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * B 端订单接口（F-ORD-02/03，接口需求 8.2/8.3）
 * 列表/详情：R2 全租户，R6/R7/R8 名下门店；接单/制作/核销：R6/R7/R8 名下门店。
 * （新订单 WS 推送 /ws/orders 属 P1-2，随门店端 store-terminal 一期落地，本批次预留）
 */
@Validated
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "ORDER-订单", description = "订单列表/详情/接单流转")
public class OrdOrderController {

    private final OrdOrderService ordOrderService;

    @Operation(summary = "订单列表（R6/R7/R8 名下门店，R2 全部）")
    @GetMapping
    public Result<PageResult<OrdOrder>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId,
            @Parameter(description = "状态：1待支付/2已支付/3制作中/4待取餐/5已完成/6已取消/7已退款")
            @RequestParam(required = false) Integer status) {
        return Result.success(ordOrderService.page(page, size, storeId, status));
    }

    @Operation(summary = "订单详情（含明细）")
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(ordOrderService.detail(id));
    }

    @OpLog(module = "order", action = "门店接单")
    @Operation(summary = "接单（已支付→制作中）")
    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable("id") Long id) {
        ordOrderService.accept(id);
        return Result.success();
    }

    @OpLog(module = "order", action = "标记制作完成")
    @Operation(summary = "标记制作完成（制作中→待取餐）")
    @PostMapping("/{id}/make")
    public Result<Void> make(@PathVariable("id") Long id) {
        ordOrderService.make(id);
        return Result.success();
    }

    @OpLog(module = "order", action = "核销取餐")
    @Operation(summary = "核销取餐（待取餐→已完成）")
    @PostMapping("/{id}/finish")
    public Result<Void> finish(@PathVariable("id") Long id) {
        ordOrderService.finish(id);
        return Result.success();
    }
}
