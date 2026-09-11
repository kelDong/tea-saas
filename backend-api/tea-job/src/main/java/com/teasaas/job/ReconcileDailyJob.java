package com.teasaas.job;

import com.teasaas.payment.service.ReconcileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日终对账 Job（F-PAY-03，接口需求 9.3，数据库设计 13.4）
 *
 * <p>每日 02:30 遍历启用租户，对昨日做资金对账（已支付订单 vs 成功流水）
 * 与库存账实核对（SUM(change_qty)=stock_qty），仅记录告警日志；
 * 差异明细由 R2 白天经 GET /api/v1/reconcile 查询。date 传 null 即昨天。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconcileDailyJob {

    private final ReconcileService reconcileService;

    /** 每日 02:30 执行 */
    @Scheduled(cron = "0 30 2 * * ?")
    public void runDaily() {
        try {
            int diffTenants = reconcileService.runDaily(null);
            if (diffTenants > 0) {
                log.warn("[ReconcileDailyJob] 昨日对账存在差异的租户数：{}", diffTenants);
            } else {
                log.info("[ReconcileDailyJob] 昨日全租户对账平账");
            }
        } catch (Exception e) {
            log.error("[ReconcileDailyJob] 日终对账失败", e);
        }
    }
}
