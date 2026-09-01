package com.teasaas.framework.tenant;

import lombok.Data;

/**
 * 租户上下文（数据库设计 14.1）
 * 登录后由拦截器写入；MyBatis-Plus 租户插件从 ThreadLocal 取 tenant_id 自动拼接 SQL。
 * 平台超级租户 tenantId = 0。
 */
@Data
public class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    /** 平台超级租户 */
    public static final long SUPER_TENANT = 0L;

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static boolean isSuperTenant() {
        Long id = TENANT_ID.get();
        return id != null && id == SUPER_TENANT;
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
