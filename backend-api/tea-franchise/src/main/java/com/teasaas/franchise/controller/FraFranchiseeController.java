package com.teasaas.franchise.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.franchise.dto.FranchiseeAccountReq;
import com.teasaas.franchise.dto.FranchiseeCreateReq;
import com.teasaas.franchise.dto.FranchiseeLevelReq;
import com.teasaas.franchise.dto.FranchiseeStatusReq;
import com.teasaas.franchise.dto.FranchiseeUpdateReq;
import com.teasaas.franchise.entity.FraFranchisee;
import com.teasaas.franchise.service.FraFranchiseeService;
import com.teasaas.system.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
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
 * 加盟商接口（F-FRA-01，接口需求 §5；权限：R2/R5）
 */
@Validated
@RestController
@RequestMapping("/api/v1/franchisees")
@RequiredArgsConstructor
@Tag(name = "加盟商管理", description = "加盟商档案/等级评定/开通账号/启停合作")
public class FraFranchiseeController {

    private final FraFranchiseeService fraFranchiseeService;

    @Operation(summary = "加盟商列表（分页，keyword 模糊名称/编码）")
    @GetMapping
    public Result<PageResult<FraFranchisee>> page(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) String keyword) {
        return Result.success(fraFranchiseeService.page(page, size, keyword));
    }

    @Operation(summary = "新增加盟商")
    @OpLog(module = "franchise", action = "新增加盟商")
    @PostMapping
    public Result<Long> create(@RequestBody @Validated FranchiseeCreateReq req) {
        return Result.success(fraFranchiseeService.create(req));
    }

    @Operation(summary = "编辑加盟商")
    @OpLog(module = "franchise", action = "编辑加盟商")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody @Validated FranchiseeUpdateReq req) {
        fraFranchiseeService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "等级评定（A/B/C，决定采购分级价 F-SUP-02）")
    @OpLog(module = "franchise", action = "加盟商等级评定")
    @PutMapping("/{id}/level")
    public Result<Void> updateLevel(@PathVariable("id") Long id,
                                    @RequestBody @Validated FranchiseeLevelReq req) {
        fraFranchiseeService.updateLevel(id, req);
        return Result.success();
    }

    @Operation(summary = "开通账号（R6 加盟商老板账号，自动绑定名下门店）")
    @OpLog(module = "franchise", action = "加盟商开通账号")
    @PostMapping("/{id}/account")
    public Result<Long> openAccount(@PathVariable("id") Long id,
                                    @RequestBody @Validated FranchiseeAccountReq req) {
        return Result.success(fraFranchiseeService.openAccount(id, req));
    }

    @Operation(summary = "启停合作（1合作中/0已终止）")
    @OpLog(module = "franchise", action = "加盟商启停合作")
    @PatchMapping("/{id}/status")
    public Result<Void> switchStatus(@PathVariable("id") Long id,
                                     @RequestBody @Validated FranchiseeStatusReq req) {
        fraFranchiseeService.switchStatus(id, req);
        return Result.success();
    }
}
