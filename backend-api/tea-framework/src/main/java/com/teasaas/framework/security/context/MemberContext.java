package com.teasaas.framework.security.context;

/**
 * C 端会员上下文（Review §2 会话化）
 *
 * <p>由 {@link com.teasaas.framework.security.config.AppMemberInterceptor} 在请求进入时
 * 从 Bearer token（或兼容期 X-Member-Id 头）解析注入，C 端 Controller 统一从此处取
 * 租户/会员身份，禁止再各自解析请求头。请求结束由拦截器 afterCompletion 清理。</p>
 */
public final class MemberContext {

    /** 会员身份（tenantId + memberId） */
    public record MemberInfo(Long tenantId, Long memberId) {
    }

    private static final ThreadLocal<MemberInfo> HOLDER = new ThreadLocal<>();

    private MemberContext() {
    }

    public static void set(Long tenantId, Long memberId) {
        HOLDER.set(new MemberInfo(tenantId, memberId));
    }

    public static MemberInfo get() {
        return HOLDER.get();
    }

    /** 当前会员 ID（未注入时为 null——匿名场景如订单试算） */
    public static Long getMemberId() {
        MemberInfo info = HOLDER.get();
        return info == null ? null : info.memberId();
    }

    /** 当前租户 ID（未注入时为 null） */
    public static Long getTenantId() {
        MemberInfo info = HOLDER.get();
        return info == null ? null : info.tenantId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
