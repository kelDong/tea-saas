package com.teasaas.system.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.system.annotation.OpLog;
import com.teasaas.system.dto.TenantCreateReq;
import com.teasaas.system.dto.TenantUpdateReq;
import com.teasaas.system.service.TenantService;
import com.teasaas.system.vo.TenantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
 * 品牌租户管理接口（F-SYS-01 前半；接口需求 2.2）
 * 权限：R1 平台管理员（Service 内校验）；平台表无租户隔离，走独立 /api/admin/v1 前缀
 */
@Validated
@RestController
@RequestMapping("/api/admin/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "品牌管理", description = "平台侧品牌租户 CRUD/启停（R1 专属）")
public class TenantController {

    private final TenantService tenantService;

    @Operation(summary = "品牌列表（分页）")
    @GetMapping
    public Result<PageResult<TenantVO>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String keyword) {
        return Result.success(tenantService.page(page, size, keyword));
    }

    @OpLog(module = "system", action = "创建品牌")
    @Operation(summary = "创建品牌（默认启用，storeLimit 缺省 0=不限）")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody TenantCreateReq req) {
        return Result.success(tenantService.create(req));
    }

    @OpLog(module = "system", action = "编辑品牌")
    @Operation(summary = "编辑品牌（tenantCode 不可改）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TenantUpdateReq req) {
        tenantService.update(id, req);
        return Result.success();
    }

    @OpLog(module = "system", action = "启停品牌")
    @Operation(summary = "启停品牌（1启用/0停用）")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody @Valid StatusBody body) {
        tenantService.updateStatus(id, body.status());
        return Result.success();
    }

    /** 启停入参 */
    public record StatusBody(
            @NotNull(message = "status 不能为空")
            Integer status) {
    }
}
