package com.teasaas.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.order.entity.OrdOrder;
import com.teasaas.order.mapper.OrdOrderMapper;
import com.teasaas.payment.entity.PayPayment;
import com.teasaas.payment.mapper.PayPaymentMapper;
import com.teasaas.payment.mapper.ReconcileMapper;
import com.teasaas.payment.service.ReconcileService;
import com.teasaas.payment.vo.ReconcileVO;
import com.teasaas.system.entity.Tenant;
import com.teasaas.system.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 对账服务实现（F-PAY-03，接口需求 9.3，数据库设计 13.4）
 *
 * <p>无状态按日实时计算：当日已支付订单（pay_time 落于当日，status∈{2,3,4,5,7}）
 * vs 当日成功流水（callback_time 落于当日），逐单匹配出资金差异；
 * 库存账实核对 SUM(inv_stock_log.change_qty) = inv_store_material_stock.stock_qty。
 * 微信商户账单核对待正式资质（t100011）下发后接入。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconcileServiceImpl implements ReconcileService {

    private static final String ROLE_R2 = "R2";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 已支付及其后状态（1待支付/6已取消不计入） */
    private static final List<Integer> PAID_STATUSES = List.of(2, 3, 4, 5, 7);
    private static final int PAY_STATUS_SUCCESS = 2;
    private static final int CHANNEL_WX = 1;

    private final OrdOrderMapper ordOrderMapper;
    private final PayPaymentMapper payPaymentMapper;
    private final ReconcileMapper reconcileMapper;
    private final TenantMapper tenantMapper;

    @Override
    public ReconcileVO run(String date) {
        requireHqRole();
        return compute(defaultYesterday(date));
    }

    @Override
    public ReconcileVO get(String date) {
        requireHqRole();
        return compute(defaultYesterday(date));
    }

    @Override
    public int runDaily(String date) {
        String target = defaultYesterday(date);
        int diffTenants = 0;
        List<Tenant> tenants = tenantMapper.selectList(
                new LambdaQueryWrapper<Tenant>().eq(Tenant::getStatus, 1));
        for (Tenant tenant : tenants) {
            TenantContext.setTenantId(tenant.getId());
            try {
                ReconcileVO vo = compute(target);
                if ((vo.getDiffCount() != null && vo.getDiffCount() > 0)
                        || (vo.getStockDiffCount() != null && vo.getStockDiffCount() > 0)) {
                    diffTenants++;
                    log.warn("日终对账发现差异 tenantId={} date={} 资金差异={}笔 库存差异={}笔",
                            tenant.getId(), target, vo.getDiffCount(), vo.getStockDiffCount());
                }
            } catch (Exception e) {
                log.error("日终对账失败 tenantId={} date={}", tenant.getId(), target, e);
            } finally {
                TenantContext.clear();
            }
        }
        return diffTenants;
    }

    // ==================== 私有工具 ====================

    private ReconcileVO compute(String date) {
        LocalDate day;
        try {
            day = LocalDate.parse(date, DAY);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "日期格式须为 yyyy-MM-dd");
        }
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();

        // 仅微信单对账支付流水（pay_channel=1）；储值单（channel=2）扣款在
        // 钱包侧落迹（total_consume），不产生 pay_payment，纳入则必误报缺流水
        List<OrdOrder> orders = ordOrderMapper.selectList(new LambdaQueryWrapper<OrdOrder>()
                .in(OrdOrder::getStatus, PAID_STATUSES)
                .eq(OrdOrder::getPayChannel, CHANNEL_WX)
                .ge(OrdOrder::getPayTime, start)
                .lt(OrdOrder::getPayTime, end));
        List<PayPayment> payments = payPaymentMapper.selectList(new LambdaQueryWrapper<PayPayment>()
                .eq(PayPayment::getStatus, PAY_STATUS_SUCCESS)
                .ge(PayPayment::getCallbackTime, start)
                .lt(PayPayment::getCallbackTime, end));

        ReconcileVO vo = new ReconcileVO();
        vo.setDate(day.toString());
        vo.setOrderCount((long) orders.size());
        vo.setOrderAmount(orders.stream()
                .mapToLong(o -> o.getPayAmount() == null ? 0L : o.getPayAmount()).sum());
        vo.setPaymentCount((long) payments.size());
        vo.setPaymentAmount(payments.stream()
                .mapToLong(p -> p.getAmount() == null ? 0L : p.getAmount()).sum());

        // 逐单匹配：订单侧缺流水/金额不符；剩余流水即无对应当日订单
        Map<Long, PayPayment> byOrderId = payments.stream()
                .collect(Collectors.toMap(PayPayment::getOrderId, p -> p, (a, b) -> a));
        List<ReconcileVO.DiffNode> diffs = new ArrayList<>();
        for (OrdOrder order : orders) {
            PayPayment payment = byOrderId.remove(order.getId());
            if (payment == null) {
                diffs.add(node("ORDER_NO_PAYMENT", order.getOrderNo(), null, order.getPayAmount(), null));
            } else if (!Objects.equals(order.getPayAmount(), payment.getAmount())) {
                diffs.add(node("AMOUNT_MISMATCH", order.getOrderNo(), payment.getPaymentNo(),
                        order.getPayAmount(), payment.getAmount()));
            }
        }
        for (PayPayment payment : byOrderId.values()) {
            diffs.add(node("PAYMENT_NO_ORDER", null, payment.getPaymentNo(), null, payment.getAmount()));
        }
        vo.setDiffCount(diffs.size());
        vo.setDiffs(diffs);

        List<ReconcileVO.StockDiffNode> stockDiffs = reconcileMapper.selectStockDiffs();
        vo.setStockDiffCount(stockDiffs.size());
        vo.setStockDiffs(stockDiffs);
        return vo;
    }

    private ReconcileVO.DiffNode node(String type, String orderNo, String paymentNo,
                                      Long orderAmount, Long paymentAmount) {
        ReconcileVO.DiffNode diff = new ReconcileVO.DiffNode();
        diff.setType(type);
        diff.setOrderNo(orderNo);
        diff.setPaymentNo(paymentNo);
        diff.setOrderAmount(orderAmount);
        diff.setPaymentAmount(paymentAmount);
        return diff;
    }

    private String defaultYesterday(String date) {
        return (date == null || date.isBlank()) ? LocalDate.now().minusDays(1).toString() : date;
    }

    private void requireHqRole() {
        if (!SecurityUtils.getRolesFromSession().contains(ROLE_R2)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部运营可对账");
        }
    }
}
