package com.teasaas.framework.security.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
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

    /** 登录/回调/文档白名单 */
    private static final String[] WHITELIST = {
            "/api/v1/auth/login",
            "/api/v1/auth/captcha",
            "/api/app/v1/auth/**",
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
                    // 未登录会被 SaInterceptor 直接拦截（NotLoginException -> 全局处理器）
                    if (StpUtil.isLogin()) {
                        Long tenantId = SecurityUtils.getTenantIdFromSession();
                        TenantContext.setTenantId(tenantId);
                    }
                }))
                .addPathPatterns("/**")
                .excludePathPatterns(WHITELIST);

        // ThreadLocal 清理（防止线程复用串租户——多租户红线）
        registry.addInterceptor(new TenantCleanupInterceptor())
                .addPathPatterns("/**");
    }
}
