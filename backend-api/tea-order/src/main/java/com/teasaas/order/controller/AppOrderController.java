package com.teasaas.order.controller;

import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.order.dto.AppOrderCreateDTO;
import com.teasaas.order.dto.AppOrderPreviewDTO;
import com.teasaas.order.entity.OrdOrder;
import com.teasaas.order.service.OrdOrderService;
import com.teasaas.order.vo.OrderCreateVO;
import com.teasaas.order.vo.OrderPreviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端订单接口（F-ORD-01/02，接口需求 8.1/8.2；白名单公开）
 *
 * <p>【多租户红线唯一例外声明】公开路径不经过会话租户注入，从此处读取
 * X-Tenant-Id 请求头手动设置 TenantContext，查询仍由租户插件自动拼
 * tenant_id 隔离；请求结束由 TenantCleanupInterceptor 统一清理 ThreadLocal。</p>
 *
 * <p>会员身份：F-MEM-01 微信登录（批次 4）落地前，经 X-Member-Id 请求头
 * 传入会员 ID（开发桥接，落地后替换为会话身份）。</p>
 */
@Validated
@RestController
@RequestMapping("/api/app/v1/orders")
@RequiredArgsConstructor
@Tag(name = "ORDER-C端订单", description = "下单/试算/我的订单/取消")
public class AppOrderController {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String MEMBER_HEADER = "X-Member-Id";

    private final OrdOrderService ordOrderService;

    @Operation(summary = "订单预览试算（不落库不扣库存）")
    @PostMapping("/preview")
    public Result<OrderPreviewVO> preview(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestBody @Validated AppOrderPreviewDTO dto) {
        requireTenant(tenantId);
        return Result.success(ordOrderService.preview(dto));
    }

    @Operation(summary = "下单（下单即扣库存，返回取餐码）")
    @PostMapping
    public Result<OrderCreateVO> create(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestHeader(value = MEMBER_HEADER, required = false)
            @Parameter(description = "会员ID（批次4登录落地前的开发桥接），必填") Long memberId,
            @RequestBody @Validated AppOrderCreateDTO dto) {
        requireTenant(tenantId);
        if (memberId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少会员标识");
        }
        return Result.success(ordOrderService.appCreate(dto, memberId));
    }

    @Operation(summary = "我的订单列表")
    @GetMapping
    public Result<PageResult<OrdOrder>> page(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestHeader(value = MEMBER_HEADER, required = false)
            @Parameter(description = "会员ID，必填") Long memberId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        requireTenant(tenantId);
        if (memberId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少会员标识");
        }
        return Result.success(ordOrderService.appPage(page, size, memberId));
    }

    @Operation(summary = "顾客取消（仅本人待支付单，回补库存）")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestHeader(value = MEMBER_HEADER, required = false)
            @Parameter(description = "会员ID，必填") Long memberId,
            @PathVariable("id") Long id) {
        requireTenant(tenantId);
        if (memberId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少会员标识");
        }
        ordOrderService.appCancel(id, memberId);
        return Result.success();
    }

    private void requireTenant(Long tenantId) {
        if (tenantId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少品牌标识");
        }
        TenantContext.setTenantId(tenantId);
    }
}
