package com.teasaas.generator.core;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 表元数据（DDL 解析结果 + 派生命名）
 *
 * @param tableName    表名（snake_case）
 * @param tableComment 表注释（业务名，如"原料档案"）
 * @param columns      列元数据（按 DDL 顺序）
 */
public record TableMeta(String tableName, String tableComment, List<ColumnMeta> columns) {

    /** 通用字段全集：六字段全含的表实体才继承 BaseEntity（数据库设计 1.1） */
    private static final Set<String> COMMON_FIELDS = Set.of(
            "tenant_id", "create_by", "create_time", "update_by", "update_time", "del_flag");

    /** 实体类名：bom_material -&gt; BomMaterial（与手写 SysUser 命名一致，保留全表名） */
    public String entityName() {
        return NameUtil.toPascal(tableName());
    }

    /** 是否继承 BaseEntity */
    public boolean extendsBase() {
        return columnNames().containsAll(COMMON_FIELDS);
    }

    /** 待生成字段（剔除 id——统一显式声明；继承 BaseEntity 时再剔除通用字段） */
    public List<ColumnMeta> businessColumns() {
        return columns.stream()
                .filter(c -> !"id".equals(c.name()))
                .filter(c -> !(extendsBase() && COMMON_FIELDS.contains(c.name())))
                .toList();
    }

    public Set<String> columnNames() {
        return columns.stream().map(ColumnMeta::name).collect(Collectors.toSet());
    }

    public boolean hasTenantId() {
        return columnNames().contains("tenant_id");
    }

    public boolean hasCreateTime() {
        return columnNames().contains("create_time");
    }

    public boolean hasDelFlag() {
        return columnNames().contains("del_flag");
    }

    /** keyword 模糊匹配列：第一个以 name 结尾的 varchar 业务列（无则 null） */
    public ColumnMeta keywordColumn() {
        return columns.stream()
                .filter(c -> c.name().endsWith("name") && "String".equals(c.javaType()))
                .filter(c -> !COMMON_FIELDS.contains(c.name()) && !"id".equals(c.name()))
                .findFirst()
                .orElse(null);
    }

    /** 模块 key：bom_material -&gt; bom（用于 REST 前缀 / @OpLog module） */
    public String moduleKey() {
        return ModuleRouter.artifact(tableName()).substring("tea-".length());
    }

    /** REST 资源名（去模块前缀）：bom_formula_item -&gt; formula_item */
    public String resourceName() {
        String prefix = moduleKey() + "_";
        return tableName().startsWith(prefix) ? tableName().substring(prefix.length()) : tableName();
    }
}
