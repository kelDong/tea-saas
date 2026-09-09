package com.teasaas.bom.controller;

import com.teasaas.bom.dto.FormulaSaveReq;
import com.teasaas.bom.dto.ProfitCalcReq;
import com.teasaas.bom.dto.StatusUpdateReq;
import com.teasaas.bom.entity.BomFormula;
import com.teasaas.bom.service.BomFormulaService;
import com.teasaas.bom.vo.FormulaDetailVO;
import com.teasaas.bom.vo.ProfitCalcVO;
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
 * 配方接口（F-BOM-02 / F-BOM-03，契约见 docs/茶饮SaaS-接口需求.md §3）
 */
@Validated
@RestController
@RequestMapping("/api/v1/formulas")
@RequiredArgsConstructor
@Tag(name = "BOM-配方", description = "配方列表/详情/新增/编辑/复制/停用启用/发布/毛利试算")
public class BomFormulaController {

    private final BomFormulaService bomFormulaService;

    @Operation(summary = "配方列表（分页，keyword 模糊匹配名称/编码）")
    @GetMapping
    public Result<PageResult<BomFormula>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String keyword) {
        return Result.success(bomFormulaService.page(page, size, keyword));
    }

    @Operation(summary = "配方详情（含明细）")
    @GetMapping("/{id}")
    public Result<FormulaDetailVO> getDetail(@PathVariable("id") Long id) {
        return Result.success(bomFormulaService.getDetail(id));
    }

    @OpLog(module = "bom", action = "新增配方")
    @Operation(summary = "新增配方")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody FormulaSaveReq req) {
        return Result.success(bomFormulaService.create(req));
    }

    @OpLog(module = "bom", action = "编辑配方")
    @Operation(summary = "编辑配方（version+1，明细全量重绑）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody FormulaSaveReq req) {
        bomFormulaService.update(id, req);
        return Result.success();
    }

    @OpLog(module = "bom", action = "复制配方")
    @Operation(summary = "复制配方")
    @PostMapping("/{id}/copy")
    public Result<Long> copy(@PathVariable("id") Long id) {
        return Result.success(bomFormulaService.copy(id));
    }

    @OpLog(module = "bom", action = "停用启用配方")
    @Operation(summary = "停用/启用配方")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Long id,
                                     @Valid @RequestBody StatusUpdateReq req) {
        bomFormulaService.updateStatus(id, req.getStatus());
        return Result.success();
    }

    @OpLog(module = "bom", action = "发布配方")
    @Operation(summary = "发布配方（进入产品库候选）")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable("id") Long id) {
        bomFormulaService.publish(id);
        return Result.success();
    }

    @OpLog(module = "bom", action = "配方毛利试算")
    @Operation(summary = "毛利试算（金额单位均为分，毛利率为百分比整数）")
    @PostMapping("/calc-profit")
    public Result<ProfitCalcVO> calcProfit(@Valid @RequestBody ProfitCalcReq req) {
        return Result.success(bomFormulaService.calcProfit(req));
    }
}
