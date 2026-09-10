package com.teasaas.product.controller;

import com.teasaas.common.result.Result;
import com.teasaas.product.dto.CategorySaveDTO;
import com.teasaas.product.entity.PrdCategory;
import com.teasaas.product.service.PrdCategoryService;
import com.teasaas.system.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类接口（F-PRD-01，接口需求 6.1 分类管理；R2/R3）
 */
@Validated
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "PRODUCT-商品分类", description = "分类管理")
public class PrdCategoryController {

    private final PrdCategoryService prdCategoryService;

    @Operation(summary = "分类列表（全量，按父级/排序）")
    @GetMapping
    public Result<List<PrdCategory>> list() {
        return Result.success(prdCategoryService.list());
    }

    @OpLog(module = "product", action = "新增商品分类")
    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Long> create(@RequestBody @Validated CategorySaveDTO dto) {
        return Result.success(prdCategoryService.create(dto));
    }

    @OpLog(module = "product", action = "编辑商品分类")
    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody @Validated CategorySaveDTO dto) {
        prdCategoryService.update(id, dto);
        return Result.success();
    }
}
