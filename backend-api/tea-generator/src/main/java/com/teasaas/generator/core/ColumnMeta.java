package com.teasaas.generator.core;

/**
 * 列元数据（DDL 解析结果）
 *
 * @param name    列名（snake_case，如 material_code）
 * @param rawType JDBC 类型（含精度，如 VARCHAR(32) / DECIMAL(10,3)）
 * @param comment 列注释（可为空串）
 */
public record ColumnMeta(String name, String rawType, String comment) {

    /** 基础类型（去掉精度）：DECIMAL(10,3) -&gt; decimal */
    public String baseType() {
        int p = rawType.indexOf('(');
        return (p > 0 ? rawType.substring(0, p) : rawType).trim().toLowerCase();
    }

    /** Java 字段名：material_code -&gt; materialCode */
    public String fieldName() {
        return NameUtil.toCamel(name());
    }

    /** Java 类型：Long/String/Integer/BigDecimal/LocalDateTime/... */
    public String javaType() {
        return TypeMapping.toJavaType(baseType());
    }

    /** Getter 方法名引用：material_name -&gt; getMaterialName */
    public String getterName() {
        return "get" + NameUtil.toPascal(name());
    }
}
