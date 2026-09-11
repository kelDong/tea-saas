package com.teasaas.marketing.controller;

import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.marketing.service.MktCouponService;
import com.teasaas.marketing.vo.CouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端优惠券接口（F-MKT-01，接口需求 11.1；白名单公开）
 *
 * <p>【多租户红线唯一例外声明】公开路径不经过会话租户注入，从此处读取
 * X-Tenant-Id 请求头手动设置 TenantContext，领券/查询仍由租户插件自动拼
 * tenant_id 隔离；请求结束由 TenantCleanupInterceptor 统一清理 ThreadLocal。</p>
 *
 * <p>会员身份经 X-Member-Id 请求头携带（开发桥接，正式 C 端会话二期替换）。</p>
 */
@RestController
@RequestMapping("/api/app/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "MKT-C端优惠券", description = "领券/我的券包")
public class AppCouponController {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String MEMBER_HEADER = "X-Member-Id";

    private final MktCouponService mktCouponService;

    @Operation(summary = "领券（模板启用+库存+限领校验）")
    @PostMapping("/{templateId}/claim")
    public Result<CouponVO> claim(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestHeader(value = MEMBER_HEADER, required = false)
            @Parameter(description = "会员ID，必填") Long memberId,
            @PathVariable("templateId") Long templateId) {
        require(tenantId, memberId);
        return Result.success(mktCouponService.claim(templateId, memberId));
    }

    @Operation(summary = "我的券包")
    @GetMapping
    public Result<PageResult<CouponVO>> myCoupons(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestHeader(value = MEMBER_HEADER, required = false)
            @Parameter(description = "会员ID，必填") Long memberId,
            @Parameter(description = "状态：1未用/2已用/3过期") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        require(tenantId, memberId);
        return Result.success(mktCouponService.myCoupons(memberId, status, page, size));
    }

    private void require(Long tenantId, Long memberId) {
        if (tenantId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少品牌标识");
        }
        if (memberId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少会员标识");
        }
        TenantContext.setTenantId(tenantId);
    }
}
