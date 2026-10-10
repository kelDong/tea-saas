package com.teasaas.framework.ws;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.framework.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * 订单 WS 握手鉴权（接口需求 8.3：连接鉴权用 Sa-Token token，仅 R7/R8）
 *
 * <p>浏览器 WebSocket API 无法自定义请求头，token 经 query 参数
 * （/ws/orders?token=xxx）传入，Sa-Token 校验后从会话取 tenantId/roles。
 * 名下门店解析经 SPI（tea-system 实现）——查询走租户插件，
 * 握手期显式注入/清理租户上下文（多租户红线：防线程复用串租户）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderWsAuthInterceptor implements HandshakeInterceptor {

    private static final String ROLE_R7 = "R7";
    private static final String ROLE_R8 = "R8";

    /** tea-system 提供实现；缺失时按无订阅放行（连上收不到推送） */
    private final ObjectProvider<WsStoreBindingResolver> resolverProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build().getQueryParams().getFirst("token");
        Object loginId = (token == null || token.isBlank()) ? null : StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        Long userId = Long.parseLong(loginId.toString());
        SaSession saSession = StpUtil.getSessionByLoginId(userId);
        Object tenantObj = saSession.get(SecurityUtils.SESSION_KEY_TENANT);
        if (tenantObj == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        Long tenantId = Long.parseLong(tenantObj.toString());
        List<String> roles = toRoles(saSession.get(SecurityUtils.SESSION_KEY_ROLES));
        if (!roles.contains(ROLE_R7) && !roles.contains(ROLE_R8)) {
            // 非 R7/R8：403 拒绝（前端按握手失败处理，不进入重连循环）
            log.info("订单WS握手拒绝（非R7/R8）userId={} roles={}", userId, roles);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        List<Long> storeIds;
        TenantContext.setTenantId(tenantId);
        try {
            WsStoreBindingResolver resolver = resolverProvider.getIfAvailable();
            storeIds = resolver == null ? List.of() : resolver.listBoundStoreIds(userId);
        } finally {
            TenantContext.clear();
        }

        attributes.put("userId", userId);
        attributes.put("tenantId", tenantId);
        attributes.put("storeIds", storeIds);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }

    @SuppressWarnings("unchecked")
    private static List<String> toRoles(Object roles) {
        return roles == null ? List.of() : (List<String>) roles;
    }
}
