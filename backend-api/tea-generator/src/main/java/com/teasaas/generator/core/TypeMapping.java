package com.teasaas.generator.core;

import java.util.Map;

/** JDBC 类型 -&gt; Java 类型映射（HANDOFF 定稿；金额一律 Integer 存分，由 DDL 保证） */
public final class TypeMapping {

    private static final Map<String, String> MAPPING = Map.ofEntries(
            Map.entry("bigint", "Long"),
            Map.entry("varchar", "String"),
            Map.entry("char", "String"),
            Map.entry("text", "String"),
            Map.entry("int", "Integer"),
            Map.entry("integer", "Integer"),
            Map.entry("smallint", "Integer"),
            Map.entry("tinyint", "Integer"),
            Map.entry("decimal", "BigDecimal"),
            Map.entry("numeric", "BigDecimal"),
            Map.entry("datetime", "LocalDateTime"),
            Map.entry("timestamp", "LocalDateTime"),
            Map.entry("date", "LocalDate"),
            Map.entry("time", "LocalTime"),
            Map.entry("json", "String"));

    private TypeMapping() {
    }

    /** 未映射类型直接失败（fail fast，防止生成错误实体） */
    public static String toJavaType(String baseType) {
        String javaType = MAPPING.get(baseType.toLowerCase());
        if (javaType == null) {
            throw new IllegalArgumentException("未映射的 JDBC 类型：" + baseType + "（请在 TypeMapping 补充映射）");
        }
        return javaType;
    }
}
