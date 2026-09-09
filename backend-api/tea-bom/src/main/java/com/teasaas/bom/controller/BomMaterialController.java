package com.teasaas.bom.controller;

import com.teasaas.bom.dto.MaterialSaveReq;
import com.teasaas.bom.dto.StatusUpdateReq;
import com.teasaas.bom.entity.BomMaterial;
import com.teasaas.bom.service.BomMaterialService;
import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.system.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * 原料档案接口（F-BOM-01，契约见 docs/茶饮SaaS-接口需求.md §3）
 */
@Validated
@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
@Tag(name = "BOM-原料档案", description = "原料列表/详情/新增/编辑/停用启用")
public class BomMaterialController {

    private final BomMaterialService bomMaterialService;

    @Operation(summary = "原料列表（分页，keyword 模糊匹配名称/编码）")
    @GetMapping
    public Result<PageResult<BomMaterial>> page(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String keyword) {
        return Result.success(bomMaterialService.page(page, size, keyword));
    }

    @Operation(summary = "原料详情")
    @GetMapping("/{id}")
    public Result<BomMaterial> getById(@PathVariable("id") Long id) {
        return Result.success(bomMaterialService.getById(id));
    }

    @OpLog(module = "bom", action = "新增原料")
    @Operation(summary = "新增原料")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody MaterialSaveReq req) {
        return Result.success(bomMaterialService.create(req));
    }

    @OpLog(module = "bom", action = "编辑原料")
    @Operation(summary = "编辑原料")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody MaterialSaveReq req) {
        bomMaterialService.update(id, req);
        return Result.success();
    }

    @OpLog(module = "bom", action = "停用启用原料")
    @Operation(summary = "停用/启用原料")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Long id,
                                     @Valid @RequestBody StatusUpdateReq req) {
        bomMaterialService.updateStatus(id, req.getStatus());
        return Result.success();
    }
}
