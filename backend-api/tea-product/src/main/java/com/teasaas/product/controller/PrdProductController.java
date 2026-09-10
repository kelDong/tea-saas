package com.teasaas.product.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.product.dto.PriceAdjustDTO;
import com.teasaas.product.dto.ProductSaveDTO;
import com.teasaas.product.entity.PrdProduct;
import com.teasaas.product.service.PrdProductService;
import com.teasaas.product.vo.ProductDetailVO;
import com.teasaas.system.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口（F-PRD-01，接口需求 6.1；R2/R3 总部产品研发）
 */
@Validated
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "PRODUCT-商品", description = "产品库商品发布/编辑/建议价/下架")
public class PrdProductController {

    private final PrdProductService prdProductService;

    @Operation(summary = "商品列表（categoryId/publishStatus/keyword 可选筛选）")
    @GetMapping
    public Result<PageResult<PrdProduct>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "发布状态：1已发布/0草稿") @RequestParam(required = false) Integer publishStatus,
            @Parameter(description = "关键词（名称/编码）") @RequestParam(required = false) String keyword) {
        return Result.success(prdProductService.page(page, size, categoryId, publishStatus, keyword));
    }

    @Operation(summary = "商品详情（含配方与规格）")
    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(prdProductService.detail(id));
    }

    @OpLog(module = "product", action = "发布商品")
    @Operation(summary = "发布商品（创建即发布；含规格组）")
    @PostMapping
    public Result<Long> create(@RequestBody @Validated ProductSaveDTO dto) {
        return Result.success(prdProductService.create(dto));
    }

    @OpLog(module = "product", action = "编辑商品")
    @Operation(summary = "编辑商品（规格全量重绑；publishStatus=1 可重新发布）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody @Validated ProductSaveDTO dto) {
        prdProductService.update(id, dto);
        return Result.success();
    }

    @OpLog(module = "product", action = "调整建议售价")
    @Operation(summary = "调整建议售价（分）")
    @PatchMapping("/{id}/price")
    public Result<Void> adjustPrice(@PathVariable("id") Long id,
                                    @RequestBody @Validated PriceAdjustDTO dto) {
        prdProductService.adjustPrice(id, dto.getPrice());
        return Result.success();
    }

    @OpLog(module = "product", action = "商品下架")
    @Operation(summary = "下架（publishStatus=0，产品库层）")
    @PatchMapping("/{id}/off")
    public Result<Void> off(@PathVariable("id") Long id) {
        prdProductService.offShelf(id);
        return Result.success();
    }
}
