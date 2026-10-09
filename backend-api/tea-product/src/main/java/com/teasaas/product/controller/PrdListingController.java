package com.teasaas.product.controller;

import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.product.dto.ListingCreateDTO;
import com.teasaas.product.dto.PriceAdjustDTO;
import com.teasaas.product.dto.SellableCheckDTO;
import com.teasaas.product.entity.PrdProduct;
import com.teasaas.product.service.PrdListingService;
import com.teasaas.product.vo.AppCategoryVO;
import com.teasaas.product.vo.AppProductVO;
import com.teasaas.product.vo.ListingVO;
import com.teasaas.product.vo.SellableCheckVO;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门店上架接口（F-PRD-02/03，接口需求 6.2/6.3）
 * 上架/调价/下架：R6；列表：R6/R7 名下门店；可售校验：R6/R7；C 端商品：公开。
 * 注意：类上不设 @RequestMapping（C 端公开接口与 B 端前缀不同，方法级绝对路径）。
 */
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "PRODUCT-门店上架", description = "加盟商上架/本店售价/可售校验/C端商品")
public class PrdListingController {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final PrdListingService prdListingService;

    @Operation(summary = "可上架商品（产品库已发布，R6）")
    @GetMapping("/api/v1/listings/available-products")
    public Result<PageResult<PrdProduct>> availableProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        return Result.success(prdListingService.availableProducts(page, size, keyword));
    }

    @Operation(summary = "本店上架列表（含实时可售标记；R6/R7 名下门店）")
    @GetMapping("/api/v1/listings")
    public Result<List<ListingVO>> listByStore(
            @Parameter(description = "门店ID", required = true) @RequestParam Long storeId) {
        return Result.success(prdListingService.listByStore(storeId));
    }

    @OpLog(module = "product", action = "门店上架商品")
    @Operation(summary = "上架商品（R6，售价不得高于总部建议价）")
    @PostMapping("/api/v1/listings")
    public Result<Long> create(@RequestBody @Validated ListingCreateDTO dto) {
        return Result.success(prdListingService.create(dto));
    }

    @OpLog(module = "product", action = "调整本店售价")
    @Operation(summary = "调整本店售价（R6）")
    @PatchMapping("/api/v1/listings/{id}/price")
    public Result<Void> adjustPrice(@PathVariable("id") Long id,
                                    @RequestBody @Validated PriceAdjustDTO dto) {
        prdListingService.adjustPrice(id, dto.getPrice());
        return Result.success();
    }

    @OpLog(module = "product", action = "门店下架商品")
    @Operation(summary = "下架（R6）")
    @PatchMapping("/api/v1/listings/{id}/off")
    public Result<Void> off(@PathVariable("id") Long id) {
        prdListingService.offShelf(id);
        return Result.success();
    }

    @Operation(summary = "商品可售校验（R6/R7，返回缺料明细）")
    @PostMapping("/api/v1/listings/check-sellable")
    public Result<List<SellableCheckVO>> checkSellable(@RequestBody @Validated SellableCheckDTO dto) {
        return Result.success(prdListingService.checkSellable(dto.getListingIds()));
    }

    /**
     * C 端公开接口（白名单，不走 Sa-Token 登录校验/自动租户注入）。
     * 【多租户红线唯一例外声明】公开路径不经过会话租户注入，从此处读取
     * X-Tenant-Id 请求头手动设置 TenantContext，查询仍由租户插件自动拼
     * tenant_id 隔离；请求结束由 TenantCleanupInterceptor 统一清理 ThreadLocal。
     */
    @Operation(summary = "门店可售商品（C 端公开，仅返回库存充足商品，F-PRD-03）")
    @GetMapping("/api/app/v1/stores/{id}/products")
    public Result<List<AppProductVO>> appStoreProducts(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @PathVariable("id") Long id,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long category) {
        if (tenantId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少品牌标识");
        }
        TenantContext.setTenantId(tenantId);
        return Result.success(prdListingService.appStoreProducts(id, category));
    }

    /**
     * C 端公开接口（白名单，同上：多租户红线唯一例外声明，读 X-Tenant-Id 手动注入，
     * 查询仍由租户插件自动拼 tenant_id 隔离，请求结束 TenantCleanupInterceptor 清理）。
     */
    @Operation(summary = "门店商品分类（C 端公开，仅该店有可售商品的分类，F-APP-02）")
    @GetMapping("/api/app/v1/stores/{id}/categories")
    public Result<List<AppCategoryVO>> appStoreCategories(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @PathVariable("id") Long id) {
        if (tenantId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少品牌标识");
        }
        TenantContext.setTenantId(tenantId);
        return Result.success(prdListingService.appStoreCategories(id));
    }
}
