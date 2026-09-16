package com.teasaas.order.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.framework.security.context.MemberContext;
import com.teasaas.order.dto.AppOrderCreateDTO;
import com.teasaas.order.dto.AppOrderPreviewDTO;
import com.teasaas.order.entity.OrdOrder;
import com.teasaas.order.service.OrdOrderService;
import com.teasaas.order.vo.OrderCreateVO;
import com.teasaas.order.vo.OrderPreviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端订单接口（F-ORD-01/02，接口需求 8.1/8.2）
 *
 * <p>身份与会话（Review §2 会话化）：AppMemberInterceptor 解析
 * Authorization: Bearer token（兼容期 X-Member-Id 双头 WARN 放行）注入
 * MemberContext + TenantContext，本类统一从 MemberContext 取身份；
 * 试算/下单均要求已登录（小程序进入即静默 wx-login），请求结束由
 * 拦截器统一清理 ThreadLocal。</p>
 */
@Validated
@RestController
@RequestMapping("/api/app/v1/orders")
@RequiredArgsConstructor
@Tag(name = "ORDER-C端订单", description = "下单/试算/我的订单/取消")
public class AppOrderController {

    private final OrdOrderService ordOrderService;

    @Operation(summary = "订单预览试算（不落库不扣库存；用券/积分抵扣）")
    @PostMapping("/preview")
    public Result<OrderPreviewVO> preview(@RequestBody @Validated AppOrderPreviewDTO dto) {
        return Result.success(ordOrderService.preview(dto, MemberContext.getMemberId()));
    }

    @Operation(summary = "下单（下单即扣库存，返回取餐码）")
    @PostMapping
    public Result<OrderCreateVO> create(@RequestBody @Validated AppOrderCreateDTO dto) {
        return Result.success(ordOrderService.appCreate(dto, MemberContext.getMemberId()));
    }

    @Operation(summary = "我的订单列表")
    @GetMapping
    public Result<PageResult<OrdOrder>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(ordOrderService.appPage(page, size, MemberContext.getMemberId()));
    }

    @Operation(summary = "顾客取消（仅本人待支付单，回补库存）")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable("id") Long id) {
        ordOrderService.appCancel(id, MemberContext.getMemberId());
        return Result.success();
    }
}
