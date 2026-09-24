package com.teasaas.framework.security.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.teasaas.framework.security.context.MemberTokenService;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.framework.tenant.TenantContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置（工程规范 8 章：B 端账号密码登录）
 * 拦截器职责：
 * 1. 登录校验（除白名单）
 * 2. 每次请求把会话中的 tenantId/userId 注入 TenantContext（多租户主线）
 * 3. 请求结束清理 ThreadLocal
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    private final MemberTokenService memberTokenService;

    public SaTokenConfig(MemberTokenService memberTokenService) {
        this.memberTokenService = memberTokenService;
    }

    /** 登录/回调/文档白名单 */
    private static final String[] WHITELIST = {
            "/api/v1/auth/login",
            "/api/v1/auth/captcha",
            "/api/app/v1/auth/**",
            "/api/app/v1/stores/nearby",
            "/api/app/v1/stores/*/products",
            "/api/app/v1/orders/**",
            "/api/app/v1/payments/**",
            "/api/app/v1/members/**",
            "/api/app/v1/coupons/**",
            "/api/notify/**",
            "/api/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    // 登录校验：自定义 auth 函数会替换 SaInterceptor 默认的 checkLogin，
                    // 必须显式调用，否则 B 端接口未登录也能进（曾致裸奔，冒烟发现）
                    StpUtil.checkLogin();
                    // 每次请求把会话中的 tenantId 注入 TenantContext（多租户主线）
                    Long tenantId = SecurityUtils.getTenantIdFromSession();
                    TenantContext.setTenantId(tenantId);
                }))
                .addPathPatterns("/**")
                .excludePathPatterns(WHITELIST);

        // C 端会员会话（Review §2 会话化）：Bearer token → MemberContext + TenantContext；
        // 排除 wx-login 与门店公开接口（它们仍各自读 X-Tenant-Id 头）
        registry.addInterceptor(new AppMemberInterceptor(memberTokenService))
                .addPathPatterns("/api/app/**")
                .excludePathPatterns(
                        "/api/app/v1/auth/wx-login",
                        "/api/app/v1/stores/nearby",
                        "/api/app/v1/stores/*/products");

        // ThreadLocal 清理（防止线程复用串租户——多租户红线）
        registry.addInterceptor(new TenantCleanupInterceptor())
                .addPathPatterns("/**");
    }
}
