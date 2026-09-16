package com.teasaas.marketing.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.framework.security.context.MemberContext;
import com.teasaas.marketing.service.MktCouponService;
import com.teasaas.marketing.vo.CouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端优惠券接口（F-MKT-01，接口需求 11.1）
 *
 * <p>身份与会话（Review §2 会话化）：AppMemberInterceptor 解析
 * Authorization: Bearer token（兼容期 X-Member-Id 双头 WARN 放行）注入
 * MemberContext + TenantContext，本类统一从 MemberContext 取身份；
 * 请求结束由拦截器统一清理 ThreadLocal。</p>
 */
@RestController
@RequestMapping("/api/app/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "MKT-C端优惠券", description = "领券/我的券包")
public class AppCouponController {

    private final MktCouponService mktCouponService;

    @Operation(summary = "领券（模板启用+库存+限领校验）")
    @PostMapping("/{templateId}/claim")
    public Result<CouponVO> claim(@PathVariable("templateId") Long templateId) {
        return Result.success(mktCouponService.claim(templateId, MemberContext.getMemberId()));
    }

    @Operation(summary = "我的券包")
    @GetMapping
    public Result<PageResult<CouponVO>> myCoupons(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(mktCouponService.myCoupons(MemberContext.getMemberId(), status, page, size));
    }
}
