package com.teasaas.framework.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单 WS 会话注册表（单实例内存版，单体一期不引 MQ/广播）
 *
 * <p>订阅模型：会话 ↔ (tenantId, 名下 storeIds)。推送按 tenantId:storeId
 * 定位订阅会话；单会话发送串行化（WebSocketSession 非并发安全），
 * 单会话推送异常只记日志不影响其他会话与业务主流程。</p>
 */
@Slf4j
@Component
public class OrderWsRegistry {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    /** key = tenantId:storeId → 订阅会话 id 集 */
    private final Map<String, Set<String>> storeSubscribers = new ConcurrentHashMap<>();

    public void register(WebSocketSession session, Long tenantId, List<Long> storeIds) {
        sessions.put(session.getId(), session);
        for (Long storeId : storeIds) {
            storeSubscribers
                    .computeIfAbsent(tenantId + ":" + storeId, k -> ConcurrentHashMap.newKeySet())
                    .add(session.getId());
        }
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
        storeSubscribers.values().forEach(ids -> ids.remove(sessionId));
    }

    /** 向订阅该门店的所有在线会话推送（payload 已序列化；失败静默，前端断线重连兜底） */
    public void push(Long tenantId, Long storeId, String payload) {
        Set<String> subscribers = storeSubscribers.get(tenantId + ":" + storeId);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        for (String sessionId : subscribers) {
            WebSocketSession session = sessions.get(sessionId);
            if (session == null || !session.isOpen()) {
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(payload));
                }
            } catch (Exception e) {
                log.warn("订单WS推送失败 sessionId={} storeId={}", sessionId, storeId, e);
            }
        }
    }

    public int onlineCount() {
        return sessions.size();
    }
}
