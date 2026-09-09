package com.teasaas.franchise.controller;

import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.franchise.dto.StoreBusinessStatusReq;
import com.teasaas.franchise.dto.StoreCreateReq;
import com.teasaas.franchise.dto.StoreUpdateReq;
import com.teasaas.franchise.entity.FraStore;
import com.teasaas.franchise.service.FraStoreService;
import com.teasaas.franchise.vo.StoreNearbyVO;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.system.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 门店接口（F-FRA-02，接口需求 §5 + §13；权限：R2/R5 全量，R6/R7 仅名下门店）
 */
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "门店管理", description = "门店档案/营业状态切换/C 端附近门店")
public class FraStoreController {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final FraStoreService fraStoreService;

    @Operation(summary = "门店列表（分页；R2/R5 看全部，R6/R7 只看名下）")
    @GetMapping("/api/v1/stores")
    public Result<PageResult<FraStore>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String keyword) {
        return Result.success(fraStoreService.page(page, size, keyword));
    }

    @Operation(summary = "门店详情")
    @GetMapping("/api/v1/stores/{id}")
    public Result<FraStore> getById(@PathVariable("id") Long id) {
        return Result.success(fraStoreService.getById(id));
    }

    @Operation(summary = "新增门店")
    @OpLog(module = "franchise", action = "新增门店")
    @PostMapping("/api/v1/stores")
    public Result<Long> create(@RequestBody @Validated StoreCreateReq req) {
        return Result.success(fraStoreService.create(req));
    }

    @Operation(summary = "编辑门店")
    @OpLog(module = "franchise", action = "编辑门店")
    @PutMapping("/api/v1/stores/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody @Validated StoreUpdateReq req) {
        fraStoreService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "营业状态切换（R2 任意本租户门店；R7 仅本店；R6 无权限）")
    @OpLog(module = "franchise", action = "门店营业状态切换")
    @PatchMapping("/api/v1/stores/{id}/business-status")
    public Result<Void> switchBusinessStatus(@PathVariable("id") Long id,
                                             @RequestBody @Validated StoreBusinessStatusReq req) {
        fraStoreService.switchBusinessStatus(id, req);
        return Result.success();
    }

    /**
     * C 端公开接口（白名单，不走 Sa-Token 登录校验/自动租户注入）。
     * 【多租户红线唯一例外声明】公开路径不会经过 SaTokenConfig 的会话租户注入，
     * 故从此处读取 X-Tenant-Id 请求头手动设置 TenantContext，后续查询仍由租户插件
     * 自动拼 tenant_id 隔离；请求结束由 TenantCleanupInterceptor 统一清理 ThreadLocal。
     */
    @Operation(summary = "附近门店（C 端公开，经纬度必填，返回最近 20 家）")
    @GetMapping("/api/app/v1/stores/nearby")
    public Result<List<StoreNearbyVO>> nearby(
            @RequestHeader(value = TENANT_HEADER, required = false) @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestParam("lng") @NotNull(message = "经度不能为空") BigDecimal lng,
            @RequestParam("lat") @NotNull(message = "纬度不能为空") BigDecimal lat) {
        if (tenantId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少品牌标识");
        }
        TenantContext.setTenantId(tenantId);
        return Result.success(fraStoreService.nearby(lng, lat));
    }
}
