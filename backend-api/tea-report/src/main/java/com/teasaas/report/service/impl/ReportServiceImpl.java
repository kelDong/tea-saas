package com.teasaas.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.report.mapper.ReportMapper;
import com.teasaas.report.service.ReportService;
import com.teasaas.report.vo.DailySettleVO;
import com.teasaas.report.vo.DashboardVO;
import com.teasaas.report.vo.HotProductVO;
import com.teasaas.report.vo.InventorySummaryVO;
import com.teasaas.report.vo.MaterialConsumptionVO;
import com.teasaas.report.vo.StoreRankVO;
import com.teasaas.report.vo.StoreReportVO;
import com.teasaas.system.entity.SysUserStore;
import com.teasaas.system.mapper.SysUserStoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 报表服务实现（F-RPT-01/02，接口需求 12.1/12.2）
 *
 * <p>无状态实时聚合，不落报表中间表；门店权限经 sys_user_store 名下校验。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final String ROLE_R2 = "R2";
    private static final String ROLE_R4 = "R4";
    private static final String ROLE_R5 = "R5";
    private static final String ROLE_R6 = "R6";
    private static final String ROLE_R7 = "R7";

    private static final Set<String> GRANULARITIES = Set.of("day", "week", "month");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportMapper reportMapper;
    private final SysUserStoreMapper sysUserStoreMapper;

    @Override
    public StoreReportVO storeTrend(String granularity, Long storeId) {
        String gran = GRANULARITIES.contains(granularity) ? granularity : "day";
        if (hasRole(ROLE_R2)) {
            // R2：storeId 可空=全品牌
        } else if (hasRole(ROLE_R6) || hasRole(ROLE_R7)) {
            requireBoundStore(storeId);
        } else {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部/加盟商/店长可看门店报表");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime start;
        switch (gran) {
            case "week" -> start = today.minusDays(55).atStartOfDay();   // 近 8 周
            case "month" -> start = today.withDayOfMonth(1).minusMonths(11).atStartOfDay(); // 近 12 月
            default -> start = today.minusDays(29).atStartOfDay();       // 近 30 天
        }
        StoreReportVO vo = new StoreReportVO();
        vo.setGranularity(gran);
        vo.setStoreId(storeId);
        vo.setItems(reportMapper.selectStoreTrend(storeId, start,
                today.plusDays(1).atStartOfDay(), gran));
        return vo;
    }

    @Override
    public DailySettleVO dailySettle(Long storeId, String date) {
        if (hasRole(ROLE_R2)) {
            requireStoreParam(storeId);
        } else if (hasRole(ROLE_R7)) {
            requireBoundStore(storeId);
        } else {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部/店长可看日结");
        }
        LocalDate day = parseDateOrDefaultYesterday(date);
        DailySettleVO vo = reportMapper.selectDailySettle(storeId,
                day.atStartOfDay(), day.plusDays(1).atStartOfDay());
        if (vo == null) {
            vo = new DailySettleVO();
            vo.setOrderCount(0L);
            vo.setSalesAmount(0L);
            vo.setAvgOrderAmount(0L);
            vo.setRefundCount(0L);
            vo.setRefundAmount(0L);
            vo.setWxAmount(0L);
            vo.setWalletAmount(0L);
        }
        vo.setDate(day.toString());
        vo.setStoreId(storeId);
        return vo;
    }

    @Override
    public DashboardVO dashboard() {
        requireBrandRole();
        LocalDate today = LocalDate.now();
        DashboardVO vo = new DashboardVO();
        StoreReportVO.TrendNode todayNode = orZero(reportMapper.selectSalesBetween(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        StoreReportVO.TrendNode yesterdayNode = orZero(reportMapper.selectSalesBetween(
                today.minusDays(1).atStartOfDay(), today.atStartOfDay()));
        vo.setTodaySales(todayNode.getSalesAmount());
        vo.setTodayOrders(todayNode.getOrderCount());
        vo.setYesterdaySales(yesterdayNode.getSalesAmount());
        vo.setYesterdayOrders(yesterdayNode.getOrderCount());
        vo.setMemberTotal(nvl(reportMapper.countMembers()));
        vo.setStoreTotal(nvl(reportMapper.countStores()));
        vo.setStockWarnCount(nvl(reportMapper.countStockWarn()));
        return vo;
    }

    @Override
    public List<HotProductVO> hotProducts(Integer days, Integer limit) {
        requireBrandRole();
        int d = (days == null || days < 1) ? 7 : Math.min(days, 90);
        int l = (limit == null || limit < 1) ? 10 : Math.min(limit, 50);
        return reportMapper.selectHotProducts(LocalDate.now().atStartOfDay().minusDays(d), l);
    }

    @Override
    public List<StoreRankVO> storeRanking(String date) {
        requireBrandRole();
        LocalDate day = parseDateOrDefaultYesterday(date);
        return reportMapper.selectStoreRanking(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }

    @Override
    public PageResult<InventorySummaryVO> inventorySummary(int page, int size) {
        requireBrandRole();
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        List<InventorySummaryVO> records =
                reportMapper.selectInventorySummary(s, (p - 1) * s);
        return PageResult.of(records, nvl(reportMapper.countInventory()), p, s);
    }

    @Override
    public MaterialConsumptionVO materialConsumption(Integer days, Long storeId, Integer limit) {
        // 权限：R4 总部供应链/R2 品牌（storeId 可空=全部门店）；R6 名下门店（对齐 12.1 storeTrend 口径）
        if (hasRole(ROLE_R4) || hasRole(ROLE_R2)) {
            // R2/R4：全部门店视角
        } else if (hasRole(ROLE_R6)) {
            requireBoundStore(storeId);
        } else {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部供应链/总部/加盟商可看原料消耗");
        }
        int d = (days == null || days < 1) ? 7 : Math.min(days, 90);
        int l = (limit == null || limit < 1) ? 10 : Math.min(limit, 50);
        LocalDate today = LocalDate.now();
        List<MaterialConsumptionVO.Row> rows = reportMapper.selectMaterialConsumption(
                today.minusDays(d - 1L).atStartOfDay(), today.plusDays(1).atStartOfDay(),
                storeId, l);

        // 平铺行（材料×日，SQL 已按 material_id 排序）聚合成嵌套节点
        Map<Long, MaterialConsumptionVO.ItemNode> byMaterial = new LinkedHashMap<>();
        for (MaterialConsumptionVO.Row row : rows) {
            MaterialConsumptionVO.ItemNode node = byMaterial.computeIfAbsent(
                    row.getMaterialId(), k -> {
                        MaterialConsumptionVO.ItemNode n = new MaterialConsumptionVO.ItemNode();
                        n.setMaterialId(row.getMaterialId());
                        n.setMaterialName(row.getMaterialName());
                        n.setUnit(row.getUnit());
                        return n;
                    });
            node.setTotalQty(node.getTotalQty().add(row.getQty()));
            MaterialConsumptionVO.DailyNode daily = new MaterialConsumptionVO.DailyNode();
            daily.setDate(row.getStatDate());
            daily.setQty(row.getQty());
            node.getDaily().add(daily);
        }
        MaterialConsumptionVO vo = new MaterialConsumptionVO();
        vo.setDays(d);
        vo.setStoreId(storeId);
        // 外层 GROUP BY 按 material_id 排序会打散 TOP N 降序，这里按期内合计回排
        vo.setItems(byMaterial.values().stream()
                .sorted((a, b) -> b.getTotalQty().compareTo(a.getTotalQty()))
                .toList());
        return vo;
    }

    // ==================== 私有工具 ====================

    private StoreReportVO.TrendNode orZero(StoreReportVO.TrendNode node) {
        if (node == null) {
            StoreReportVO.TrendNode zero = new StoreReportVO.TrendNode();
            zero.setOrderCount(0L);
            zero.setSalesAmount(0L);
            return zero;
        }
        if (node.getSalesAmount() == null) {
            node.setSalesAmount(0L);
        }
        if (node.getOrderCount() == null) {
            node.setOrderCount(0L);
        }
        return node;
    }

    private Long nvl(Long v) {
        return v == null ? 0L : v;
    }

    private LocalDate parseDateOrDefaultYesterday(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now().minusDays(1);
        }
        try {
            return LocalDate.parse(date, DAY);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "日期格式须为 yyyy-MM-dd");
        }
    }

    /** R6/R7：storeId 必填且在名下 */
    private void requireBoundStore(Long storeId) {
        requireStoreParam(storeId);
        List<Long> bound = sysUserStoreMapper.selectList(
                        new LambdaQueryWrapper<SysUserStore>()
                                .eq(SysUserStore::getUserId, SecurityUtils.getUserId()))
                .stream().map(SysUserStore::getStoreId).distinct().toList();
        if (!bound.contains(storeId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "非名下门店");
        }
    }

    private void requireStoreParam(Long storeId) {
        if (storeId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少门店标识");
        }
    }

    /** 品牌级（12.2）：R2/R5 */
    private void requireBrandRole() {
        if (!hasRole(ROLE_R2) && !hasRole(ROLE_R5)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部/运营可查看");
        }
    }

    private boolean hasRole(String roleCode) {
        return SecurityUtils.getRolesFromSession().contains(roleCode);
    }
}
