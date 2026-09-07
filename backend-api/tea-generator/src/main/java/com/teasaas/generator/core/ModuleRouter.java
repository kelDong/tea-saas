package com.teasaas.generator.core;

import java.util.Map;

/** 表前缀 -&gt; Maven 模块路由（HANDOFF §3 定稿） */
public final class ModuleRouter {

    private static final Map<String, String> PREFIX_TO_MODULE = Map.of(
            "sys", "tea-system",
            "fra", "tea-franchise",
            "sup", "tea-supply",
            "bom", "tea-bom",
            "prd", "tea-product",
            "inv", "tea-inventory",
            "ord", "tea-order",
            "pay", "tea-payment",
            "mem", "tea-member",
            "mkt", "tea-marketing");

    private ModuleRouter() {
    }

    /** 表名 -&gt; 模块 artifactId（tenant/sys_* -&gt; tea-system；其余按首段前缀路由） */
    public static String artifact(String tableName) {
        if (tableName.equals("tenant") || tableName.startsWith("sys_")) {
            return "tea-system";
        }
        int idx = tableName.indexOf('_');
        if (idx <= 0) {
            throw new IllegalArgumentException("无法路由表（无识别前缀）：" + tableName);
        }
        String module = PREFIX_TO_MODULE.get(tableName.substring(0, idx));
        if (module == null) {
            throw new IllegalArgumentException("无法路由表（前缀未登记）：" + tableName);
        }
        return module;
    }

    /** 模块 artifactId -&gt; 基础包：tea-bom -&gt; com.teasaas.bom */
    public static String basePackage(String artifactId) {
        return "com.teasaas." + artifactId.substring("tea-".length());
    }
}
