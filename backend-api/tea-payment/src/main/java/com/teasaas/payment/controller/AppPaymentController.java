package com.teasaas.payment.controller;

import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.Result;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.payment.dto.WxPayCreateDTO;
import com.teasaas.payment.service.PayPaymentService;
import com.teasaas.payment.vo.PaymentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端支付接口（F-PAY-01，接口需求 9.1；白名单公开，一期 mock 网关）
 *
 * <p>【多租户红线唯一例外声明】公开路径不经过会话租户注入，从此处读取
 * X-Tenant-Id 请求头手动设置 TenantContext，查询仍由租户插件自动拼
 * tenant_id 隔离；请求结束由 TenantCleanupInterceptor 统一清理 ThreadLocal。</p>
 */
@Validated
@RestController
@RequestMapping("/api/app/v1/payments")
@RequiredArgsConstructor
@Tag(name = "PAY-C端支付", description = "微信JSAPI统一下单")
public class AppPaymentController {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final PayPaymentService payPaymentService;

    @Operation(summary = "微信 JSAPI 统一下单（小程序待支付单）")
    @PostMapping("/wx-jsapi")
    public Result<PaymentVO> wxJsapi(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestBody @Validated WxPayCreateDTO dto) {
        requireTenant(tenantId);
        return Result.success(payPaymentService.wxJsapi(dto.getOrderId()));
    }

    private void requireTenant(Long tenantId) {
        if (tenantId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少品牌标识");
        }
        TenantContext.setTenantId(tenantId);
    }
}
