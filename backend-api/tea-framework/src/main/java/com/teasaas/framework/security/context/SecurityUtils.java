package com.teasaas.framework.security.context;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;

import java.util.List;

/**
 * 当前登录用户上下文（基于 Sa-Token 会话）
 * 登录时写入：userId / tenantId / roles / perms
 */
public final class SecurityUtils {

    public static final String SESSION_KEY_TENANT = "tenantId";
    public static final String SESSION_KEY_ROLES = "roles";
    public static final String SESSION_KEY_PERMS = "perms";

    private SecurityUtils() {
    }

    public static Long getUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    public static Long getTenantIdFromSession() {
        SaSession session = StpUtil.getSession();
        Object tenantId = session.get(SESSION_KEY_TENANT);
        return tenantId == null ? null : Long.parseLong(tenantId.toString());
    }

    @SuppressWarnings("unchecked")
    public static List<String> getRolesFromSession() {
        SaSession session = StpUtil.getSession();
        Object roles = session.get(SESSION_KEY_ROLES);
        return roles == null ? List.of() : (List<String>) roles;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getPermsFromSession() {
        SaSession session = StpUtil.getSession();
        Object perms = session.get(SESSION_KEY_PERMS);
        return perms == null ? List.of() : (List<String>) perms;
    }
}
