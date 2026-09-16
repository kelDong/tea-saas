package com.teasaas.framework.security.config;

import cn.dev33.satoken.exception.NotLoginException;
import com.teasaas.framework.security.context.MemberContext;
import com.teasaas.framework.security.context.MemberTokenService;
import com.teasaas.framework.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * C 端会员身份拦截器（Review §2 会话化）
 *
 * <p>注册于 /api/app/**（排除 wx-login 与门店公开接口）：
 * ① Authorization: Bearer &lt;token&gt; → Redis 解析注入 MemberContext + TenantContext；
 * ② 兼容期（一个迭代，之后硬切）：无 token 但携带 X-Tenant-Id/X-Member-Id 头 → WARN 放行注入；
 * ③ 两者皆无 → 未登录拒绝（对齐 Sa-Token NotLoginException 全局口径）。
 * 公开的 门店nearby 与 门店商品列表（路径含通配符，此处不展开）不经过本拦截器，仍由各自 Controller 读 X-Tenant-Id。</p>
 */
@Slf4j
public class AppMemberInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String MEMBER_HEADER = "X-Member-Id";

    private final MemberTokenService memberTokenService;

    public AppMemberInterceptor(MemberTokenService memberTokenService) {
        this.memberTokenService = memberTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        MemberContext.MemberInfo info = resolveFromBearer(request);
        if (info != null) {
            inject(info);
            return true;
        }
        // 兼容期：纯头传递（资金语义接口裸奔，会话化前的过渡），到期删除本分支
        Long tenantId = parseLong(request.getHeader(TENANT_HEADER));
        Long memberId = parseLong(request.getHeader(MEMBER_HEADER));
        if (tenantId != null && memberId != null) {
            log.warn("C端请求走兼容头传递（将于近期下线），请改用 Authorization: Bearer <token> uri={}",
                    request.getRequestURI());
            inject(new MemberContext.MemberInfo(tenantId, memberId));
            return true;
        }
        throw NotLoginException.newInstance(NotLoginException.NOT_TOKEN_MESSAGE, "member", null,
                NotLoginException.NOT_TOKEN);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        MemberContext.clear();
    }

    private MemberContext.MemberInfo resolveFromBearer(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return memberTokenService.resolve(header.substring(BEARER_PREFIX.length()).trim());
    }

    private void inject(MemberContext.MemberInfo info) {
        MemberContext.set(info.tenantId(), info.memberId());
        TenantContext.setTenantId(info.tenantId());
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
