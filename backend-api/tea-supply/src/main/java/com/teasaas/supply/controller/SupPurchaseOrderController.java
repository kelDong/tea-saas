package com.teasaas.supply.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.supply.dto.PurchaseOrderCreateDTO;
import com.teasaas.supply.entity.SupPurchaseOrder;
import com.teasaas.supply.service.SupPurchaseOrderService;
import com.teasaas.supply.vo.PurchaseOrderDetailVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 采购单接口（F-SUP-03/04，接口需求 4.3）
 * 发起：仅 R6（P1-1）；确认/发货/取消：R2/R4（R2 蕴含 R4 权限）；入库：R6/R7（名下门店）；R6 仅待确认前可取消
 */
@Validated
@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "SUPPLY-采购单", description = "采购单发起/计价确认/发货/入库/取消")
public class SupPurchaseOrderController {

    private final SupPurchaseOrderService supPurchaseOrderService;

    @Operation(summary = "采购单列表（R4 全部 / R6 名下门店；status/storeId 可选筛选）")
    @GetMapping
    public Result<PageResult<SupPurchaseOrder>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态：1待确认/2已确认/3已发货/4已入库/5已取消")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId) {
        return Result.success(supPurchaseOrderService.page(page, size, status, storeId));
    }

    @Operation(summary = "采购单详情（含明细计价快照）")
    @GetMapping("/{id}")
    public Result<PurchaseOrderDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(supPurchaseOrderService.detail(id));
    }

    @OpLog(module = "supply", action = "发起采购单")
    @Operation(summary = "发起采购单（仅 R6，服务端按加盟商等级自动计价）")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody PurchaseOrderCreateDTO dto) {
        return Result.success(supPurchaseOrderService.create(dto));
    }

    @OpLog(module = "supply", action = "采购单确认计价")
    @Operation(summary = "确认计价（R4，待确认→已确认）")
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable("id") Long id) {
        supPurchaseOrderService.confirm(id);
        return Result.success();
    }

    @OpLog(module = "supply", action = "采购单发货")
    @Operation(summary = "发货（R4，已确认→已发货）")
    @PostMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable("id") Long id) {
        supPurchaseOrderService.ship(id);
        return Result.success();
    }

    @OpLog(module = "supply", action = "采购单确认入库")
    @Operation(summary = "确认入库（R6/R7 名下门店，已发货→已入库）")
    @PostMapping("/{id}/receive")
    public Result<Void> receive(@PathVariable("id") Long id) {
        supPurchaseOrderService.receive(id);
        return Result.success();
    }

    @OpLog(module = "supply", action = "取消采购单")
    @Operation(summary = "取消采购单（R4 未完成均可 / R6 仅待确认）")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable("id") Long id) {
        supPurchaseOrderService.cancel(id);
        return Result.success();
    }
}
