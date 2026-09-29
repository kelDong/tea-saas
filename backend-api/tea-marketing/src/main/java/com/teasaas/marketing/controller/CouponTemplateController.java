package com.teasaas.marketing.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.marketing.dto.CouponTemplateDTO;
import com.teasaas.marketing.entity.MktCouponTemplate;
import com.teasaas.marketing.service.MktCouponTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台券模板接口（F-MKT-01，接口需求 11.1；会话鉴权 R2/R5 配置、R2/R5/R6 查看）
 */
@Validated
@RestController
@RequestMapping("/api/v1/coupon-templates")
@RequiredArgsConstructor
@Tag(name = "MKT-券模板", description = "券模板配置")
public class CouponTemplateController {

    private final MktCouponTemplateService mktCouponTemplateService;

    @Operation(summary = "券模板列表")
    @GetMapping
    public Result<PageResult<MktCouponTemplate>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态：1启用/2停用") @RequestParam(required = false) Integer status) {
        return Result.success(mktCouponTemplateService.page(page, size, status));
    }

    @Operation(summary = "券模板详情（编辑回填）")
    @GetMapping("/{id}")
    public Result<MktCouponTemplate> detail(
            @Parameter(description = "模板ID") @PathVariable("id") Long id) {
        return Result.success(mktCouponTemplateService.detail(id));
    }

    @Operation(summary = "创建券模板")
    @PostMapping
    public Result<MktCouponTemplate> create(@RequestBody @Validated CouponTemplateDTO dto) {
        return Result.success(mktCouponTemplateService.create(dto));
    }

    @Operation(summary = "编辑/停用券模板（已领券不受影响）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
            @RequestBody @Validated CouponTemplateDTO dto) {
        mktCouponTemplateService.update(id, dto);
        return Result.success();
    }
}
