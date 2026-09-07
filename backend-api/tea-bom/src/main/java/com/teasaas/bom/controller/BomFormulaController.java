package com.teasaas.bom.controller;

import com.teasaas.bom.entity.BomFormula;
import com.teasaas.bom.service.BomFormulaService;
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
 * 配方接口（生成器骨架：路径/入参 DTO/权限码以接口需求文档为准，人工 review 后校正）
 */
@Validated
@RestController
@RequestMapping("/api/v1/bom/formula")
@RequiredArgsConstructor
@Tag(name = "BOM-配方", description = "生成器骨架：接口契约以 docs/茶饮SaaS-接口需求.md 为准")
public class BomFormulaController {

    private final BomFormulaService bomFormulaService;

    @Operation(summary = "分页列表")
    @GetMapping
    public Result<PageResult<BomFormula>> page(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String keyword) {
        return Result.success(bomFormulaService.page(page, size, keyword));
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<BomFormula> getById(@PathVariable Long id) {
        return Result.success(bomFormulaService.getById(id));
    }

    @OpLog(module = "bom", action = "新增配方")
    @Operation(summary = "新增")
    @PostMapping
    public Result<Long> create(@RequestBody BomFormula entity) {
        return Result.success(bomFormulaService.create(entity));
    }

    @OpLog(module = "bom", action = "修改配方")
    @Operation(summary = "修改")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BomFormula entity) {
        bomFormulaService.update(id, entity);
        return Result.success();
    }

    @OpLog(module = "bom", action = "删除配方")
    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bomFormulaService.delete(id);
        return Result.success();
    }
}
