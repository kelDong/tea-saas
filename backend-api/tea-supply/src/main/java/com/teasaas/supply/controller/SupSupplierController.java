package com.teasaas.supply.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.supply.dto.SupplierMaterialBindDTO;
import com.teasaas.supply.dto.SupplierSaveDTO;
import com.teasaas.supply.dto.SupplierStatusDTO;
import com.teasaas.supply.entity.SupSupplier;
import com.teasaas.supply.entity.SupSupplierMaterial;
import com.teasaas.supply.service.SupSupplierService;
import com.teasaas.system.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;

/**
 * 供应商接口（F-SUP-01，接口需求 4.1；角色 R2/R4）
 */
@Validated
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "SUPPLY-供应商", description = "供应商档案/关联原料/启停")
public class SupSupplierController {

    private final SupSupplierService supSupplierService;

    @Operation(summary = "供应商列表（分页，keyword 匹配编码/名称）")
    @GetMapping
    public Result<PageResult<SupSupplier>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "关键词（编码/名称模糊匹配）") @RequestParam(required = false) String keyword) {
        return Result.success(supSupplierService.page(page, size, keyword));
    }

    @Operation(summary = "供应商关联原料列表")
    @GetMapping("/{id}/materials")
    public Result<List<SupSupplierMaterial>> listMaterials(@PathVariable("id") Long id) {
        return Result.success(supSupplierService.listMaterials(id));
    }

    @OpLog(module = "supply", action = "新增供应商")
    @Operation(summary = "新增供应商")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody SupplierSaveDTO dto) {
        return Result.success(supSupplierService.create(dto));
    }

    @OpLog(module = "supply", action = "编辑供应商")
    @Operation(summary = "编辑供应商")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @Validated @RequestBody SupplierSaveDTO dto) {
        supSupplierService.update(id, dto);
        return Result.success();
    }

    @OpLog(module = "supply", action = "供应商关联原料")
    @Operation(summary = "关联可供应原料（全量重绑）")
    @PutMapping("/{id}/materials")
    public Result<Void> bindMaterials(@PathVariable("id") Long id,
                                      @Valid @RequestBody SupplierMaterialBindDTO dto) {
        supSupplierService.bindMaterials(id, dto);
        return Result.success();
    }

    @OpLog(module = "supply", action = "供应商启停")
    @Operation(summary = "启用/停用供应商")
    @PatchMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable("id") Long id,
                                     @Valid @RequestBody SupplierStatusDTO dto) {
        supSupplierService.changeStatus(id, dto.getStatus());
        return Result.success();
    }
}
