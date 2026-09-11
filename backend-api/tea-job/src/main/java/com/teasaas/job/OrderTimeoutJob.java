package com.teasaas.job;

import com.teasaas.order.service.OrdOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时取消 Job（P1-5，tea.order.pay-timeout-minutes=15）
 *
 * <p>每分钟扫描：遍历租户，status=1 且 create_time 超时的单，
 * 逐单事务内条件更新 1→6 + 库存回补（bizType=2，bizNo=orderNo 幂等）。
 * 服务内部自管理 TenantContext，无会话依赖（job 安全）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrdOrderService ordOrderService;

    /** 每分钟第 0 秒执行 */
    @Scheduled(cron = "0 * * * * ?")
    public void cancelExpiredOrders() {
        try {
            int cancelled = ordOrderService.cancelExpiredOrders();
            if (cancelled > 0) {
                log.info("[OrderTimeoutJob] 超时取消订单 {} 单", cancelled);
            }
        } catch (Exception e) {
            log.error("[OrderTimeoutJob] 超时取消扫描失败", e);
        }
    }
}
