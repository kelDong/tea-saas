package com.teasaas.framework.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：注册 /ws/orders（门店端订单实时推送，P1-2 决策）
 *
 * <p>鉴权在握手拦截器（query token，浏览器 WS 无法带请求头）；
 * allowedOriginPatterns="*"——准入已由 token 控制，放开跨域便于
 * 前后台分离部署。单实例单体，无 sticky session 问题。</p>
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class OrderWsConfig implements WebSocketConfigurer {

    private final OrderWsHandler orderWsHandler;
    private final OrderWsAuthInterceptor orderWsAuthInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(orderWsHandler, "/ws/orders")
                .addInterceptors(orderWsAuthInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
