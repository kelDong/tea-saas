package com.teasaas.framework.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;

/**
 * 订单 WS 端点处理器（/ws/orders）
 *
 * <p>一期单向推送：连接（握手拦截器已完成鉴权与门店订阅解析）→ 按事件推送。
 * 客户端文本仅处理心跳 PING→PONG，其余忽略；断连由客户端指数退避重连。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderWsHandler extends TextWebSocketHandler {

    private final OrderWsRegistry registry;

    @Override
    @SuppressWarnings("unchecked")
    public void afterConnectionEstablished(WebSocketSession session) {
        Long tenantId = (Long) session.getAttributes().get("tenantId");
        Long userId = (Long) session.getAttributes().get("userId");
        List<Long> storeIds = (List<Long>) session.getAttributes().get("storeIds");
        registry.register(session, tenantId, storeIds);
        log.info("订单WS已连接 userId={} tenantId={} stores={} online={}",
                userId, tenantId, storeIds, registry.onlineCount());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.remove(session.getId());
        log.info("订单WS已断开 sessionId={} status={} online={}",
                session.getId(), status, registry.onlineCount());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if ("PING".equals(message.getPayload())) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage("PONG"));
                }
            } catch (Exception e) {
                log.warn("订单WS心跳应答失败 sessionId={}", session.getId(), e);
            }
        }
    }
}
