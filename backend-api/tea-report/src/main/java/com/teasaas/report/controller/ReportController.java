package com.teasaas.report.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.report.service.ReportService;
import com.teasaas.report.vo.DailySettleVO;
import com.teasaas.report.vo.DashboardVO;
import com.teasaas.report.vo.HotProductVO;
import com.teasaas.report.vo.InventorySummaryVO;
import com.teasaas.report.vo.StoreRankVO;
import com.teasaas.report.vo.StoreReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报表接口（F-RPT-01/02，接口需求 12.1/12.2；会话鉴权）
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "RPT-报表", description = "门店报表/品牌看板")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "门店营业趋势（日/周/月）")
    @GetMapping("/store")
    public Result<StoreReportVO> storeTrend(
            @Parameter(description = "门店ID（R2 可空=全品牌；R6/R7 必填本店）")
            @RequestParam(required = false) Long storeId,
            @Parameter(description = "粒度：day/week/month") @RequestParam(defaultValue = "day") String granularity) {
        return Result.success(reportService.storeTrend(granularity, storeId));
    }

    @Operation(summary = "门店日结数据")
    @GetMapping("/store/{storeId}/daily")
    public Result<DailySettleVO> dailySettle(
            @PathVariable("storeId") Long storeId,
            @Parameter(description = "日期 yyyy-MM-dd，默认昨日") @RequestParam(required = false) String date) {
        return Result.success(reportService.dailySettle(storeId, date));
    }

    @Operation(summary = "品牌汇总看板（R2/R5）")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(reportService.dashboard());
    }

    @Operation(summary = "热销商品 TOP（R2/R5）")
    @GetMapping("/hot-products")
    public Result<List<HotProductVO>> hotProducts(
            @Parameter(description = "统计天数，默认7") @RequestParam(required = false) Integer days,
            @Parameter(description = "TOP N，默认10") @RequestParam(required = false) Integer limit) {
        return Result.success(reportService.hotProducts(days, limit));
    }

    @Operation(summary = "门店排行（R2/R5，默认昨日）")
    @GetMapping("/store-ranking")
    public Result<List<StoreRankVO>> storeRanking(
            @Parameter(description = "日期 yyyy-MM-dd，默认昨日") @RequestParam(required = false) String date) {
        return Result.success(reportService.storeRanking(date));
    }

    @Operation(summary = "库存总览（R2/R5 只读，预警优先）")
    @GetMapping("/inventory-summary")
    public Result<PageResult<InventorySummaryVO>> inventorySummary(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return Result.success(reportService.inventorySummary(page, size));
    }
}
