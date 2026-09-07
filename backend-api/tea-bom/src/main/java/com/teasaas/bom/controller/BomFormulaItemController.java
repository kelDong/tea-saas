package com.teasaas.bom.controller;

import com.teasaas.bom.entity.BomFormulaItem;
import com.teasaas.bom.service.BomFormulaItemService;
import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.system.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配方明细接口（生成器骨架：路径/入参 DTO/权限码以接口需求文档为准，人工 review 后校正）
 */
@Validated
@RestController
@RequestMapping("/api/v1/bom/formula_item")
@RequiredArgsConstructor
@Tag(name = "BOM-配方明细", description = "生成器骨架：接口契约以 docs/茶饮SaaS-接口需求.md 为准")
public class BomFormulaItemController {

    private final BomFormulaItemService bomFormulaItemService;

    @Operation(summary = "分页列表")
    @GetMapping
    public Result<PageResult<BomFormulaItem>> page(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return Result.success(bomFormulaItemService.page(page, size));
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<BomFormulaItem> getById(@PathVariable Long id) {
        return Result.success(bomFormulaItemService.getById(id));
    }

    @OpLog(module = "bom", action = "新增配方明细")
    @Operation(summary = "新增")
    @PostMapping
    public Result<Long> create(@RequestBody BomFormulaItem entity) {
        return Result.success(bomFormulaItemService.create(entity));
    }

    @OpLog(module = "bom", action = "修改配方明细")
    @Operation(summary = "修改")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BomFormulaItem entity) {
        bomFormulaItemService.update(id, entity);
        return Result.success();
    }

    @OpLog(module = "bom", action = "删除配方明细")
    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bomFormulaItemService.delete(id);
        return Result.success();
    }
}
