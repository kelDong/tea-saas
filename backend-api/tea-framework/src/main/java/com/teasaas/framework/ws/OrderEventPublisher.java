package com.teasaas.framework.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 订单事件发布器（业务模块唯一入口）
 *
 * <p>tea-order / tea-payment 在状态流转成功后调用。事务内调用时挂
 * afterCommit 再推送——避免前端收到通知先于事务提交重查到旧状态；
 * 无事务（理论不存在，防御）立即推送。推送是尽力而为：任何异常只记
 * 日志，绝不影响业务主流程（前端断线重连 + 手动刷新兜底）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final OrderWsRegistry registry;
    private final ObjectMapper objectMapper;

    public void publish(WsOrderMessage message) {
        message.setTime(System.currentTimeMillis());
        String payload;
        try {
            payload = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.warn("订单事件序列化失败 orderId={}", message.getOrderId(), e);
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    registry.push(message.getTenantId(), message.getStoreId(), payload);
                }
            });
        } else {
            registry.push(message.getTenantId(), message.getStoreId(), payload);
        }
    }
}
