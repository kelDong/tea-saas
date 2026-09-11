package com.teasaas.payment.controller;

import com.teasaas.common.result.Result;
import com.teasaas.payment.service.ReconcileService;
import com.teasaas.payment.vo.ReconcileVO;
import com.teasaas.system.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对账接口（F-PAY-03，接口需求 9.3，仅 R2）
 * 无状态按日实时计算，run 与 get 同源同果；date 缺省为昨天。
 */
@RestController
@RequestMapping("/api/v1/reconcile")
@RequiredArgsConstructor
@Tag(name = "PAY-对账", description = "日终资金/库存账实核对")
public class ReconcileController {

    private final ReconcileService reconcileService;

    @OpLog(module = "payment", action = "触发对账")
    @Operation(summary = "触发对账（当前租户，date 缺省为昨天）")
    @PostMapping("/run")
    public Result<ReconcileVO> run(
            @Parameter(description = "对账日期 yyyy-MM-dd，缺省昨天")
            @RequestParam(value = "date", required = false) String date) {
        return Result.success(reconcileService.run(date));
    }

    @Operation(summary = "对账结果查询（与 run 同源同果）")
    @GetMapping
    public Result<ReconcileVO> get(
            @Parameter(description = "对账日期 yyyy-MM-dd，缺省昨天")
            @RequestParam(value = "date", required = false) String date) {
        return Result.success(reconcileService.get(date));
    }
}
