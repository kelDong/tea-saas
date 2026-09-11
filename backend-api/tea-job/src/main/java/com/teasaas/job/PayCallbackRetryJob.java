package com.teasaas.job;

import com.teasaas.payment.service.PayPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 支付回调重试 Job（P1-3，不引 MQ 的本地表兜底）
 *
 * <p>每分钟扫描 pay_callback_task：status=1 且 next_retry_time 到点，
 * 遍历租户逐单重试；失败指数退避 2^n 分钟，超 max_retry=5 置 3 终态。
 * 与超时取消错开 30 秒，避免同秒争抢 DB 连接。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayCallbackRetryJob {

    private final PayPaymentService payPaymentService;

    /** 每分钟第 30 秒执行（与 OrderTimeoutJob 错峰） */
    @Scheduled(cron = "30 * * * * ?")
    public void retryPendingTasks() {
        try {
            int processed = payPaymentService.retryPendingTasks();
            if (processed > 0) {
                log.info("[PayCallbackRetryJob] 重试成功回调任务 {} 个", processed);
            }
        } catch (Exception e) {
            log.error("[PayCallbackRetryJob] 回调重试扫描失败", e);
        }
    }
}
