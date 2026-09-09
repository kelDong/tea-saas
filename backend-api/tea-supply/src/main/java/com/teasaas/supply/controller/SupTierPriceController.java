package com.teasaas.supply.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.supply.dto.TierPriceBatchDTO;
import com.teasaas.supply.entity.SupTierPrice;
import com.teasaas.supply.service.SupTierPriceService;
import com.teasaas.system.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分级价格接口（F-SUP-02，接口需求 4.2；查询 R2/R4/R6，设置 R2/R4）
 */
@Validated
@RestController
@RequestMapping("/api/v1/tier-prices")
@RequiredArgsConstructor
@Tag(name = "SUPPLY-分级价格", description = "原料 × 加盟商等级(A/B/C) 价格表")
public class SupTierPriceController {

    private final SupTierPriceService supTierPriceService;

    @Operation(summary = "价格表查询（materialId/level 均可空=全量分页）")
    @GetMapping
    public Result<PageResult<SupTierPrice>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "原料ID") @RequestParam(required = false) Long materialId,
            @Parameter(description = "加盟商等级 A/B/C") @RequestParam(required = false) String level) {
        return Result.success(supTierPriceService.page(page, size, materialId, level));
    }

    @OpLog(module = "supply", action = "批量设置分级价格")
    @Operation(summary = "批量设置价格（按 原料×等级 upsert）")
    @PutMapping("/batch")
    public Result<Void> batchSet(@Validated @RequestBody TierPriceBatchDTO dto) {
        supTierPriceService.batchSet(dto);
        return Result.success();
    }
}
