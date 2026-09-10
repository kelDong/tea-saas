package com.teasaas.inventory.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.inventory.dto.StockAdjustDTO;
import com.teasaas.inventory.entity.InvStoreMaterialStock;
import com.teasaas.inventory.entity.InvStockLog;
import com.teasaas.inventory.service.InvStoreMaterialStockService;
import com.teasaas.inventory.vo.InventorySummaryVO;
import com.teasaas.inventory.vo.StockWarningVO;
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

import java.util.List;

/**
 * 库存接口（F-INV-01/03，接口需求 7.1/7.2）
 * 台账/流水：R6/R7 名下门店、R2 读；盘点：R6/R7 仅；低库存：R6/R7、R2；总览：R2/R5 读
 */
@Validated
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
@Tag(name = "INVENTORY-库存", description = "库存台账/变动流水/盘点修正/低库存预警/总部总览")
public class InventoryController {

    private final InvStoreMaterialStockService invStoreMaterialStockService;

    @Operation(summary = "门店库存列表（R6/R7 名下门店，R2 读全部）")
    @GetMapping
    public Result<PageResult<InvStoreMaterialStock>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId,
            @Parameter(description = "原料ID") @RequestParam(required = false) Long materialId) {
        return Result.success(invStoreMaterialStockService.page(page, size, storeId, materialId));
    }

    @Operation(summary = "库存变动流水（R6/R7 名下门店）")
    @GetMapping("/{storeId}/{materialId}/logs")
    public Result<PageResult<InvStockLog>> logs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "门店ID") @PathVariable("storeId") Long storeId,
            @Parameter(description = "原料ID") @PathVariable("materialId") Long materialId) {
        return Result.success(invStoreMaterialStockService.logs(page, size, storeId, materialId));
    }

    @OpLog(module = "inventory", action = "库存盘点修正")
    @Operation(summary = "盘点修正（仅 R6/R7，以实盘为准，差异写流水留痕）")
    @PostMapping("/adjust")
    public Result<Void> adjust(@RequestBody @Validated StockAdjustDTO dto) {
        invStoreMaterialStockService.adjust(dto);
        return Result.success();
    }

    @Operation(summary = "低库存列表（R6/R7 名下门店，R2 全部）")
    @GetMapping("/warnings")
    public Result<List<StockWarningVO>> warnings(
            @Parameter(description = "门店ID（门店侧必传）") @RequestParam(required = false) Long storeId) {
        return Result.success(invStoreMaterialStockService.warnings(storeId));
    }

    @Operation(summary = "总部库存总览（R2/R5 读，按原料聚合）")
    @GetMapping("/summary")
    public Result<List<InventorySummaryVO>> summary() {
        return Result.success(invStoreMaterialStockService.summary());
    }
}
