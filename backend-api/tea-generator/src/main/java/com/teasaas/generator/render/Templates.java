package com.teasaas.generator.render;

import com.teasaas.generator.core.ColumnMeta;
import com.teasaas.generator.core.ModuleRouter;
import com.teasaas.generator.core.NameUtil;
import com.teasaas.generator.core.TableMeta;

import java.util.TreeSet;

/**
 * 四件套代码模板（规范 14：禁 TODO 占位；Controller 走 Result&lt;T&gt;/分页/@Validated/OpenAPI/@OpLog；
 * Service 写方法带 rollbackFor = Exception.class；构造器注入）
 */
public final class Templates {

    private Templates() {
    }

    // ==================== Entity ====================

    public static String entity(TableMeta t) {
        String pkg = basePackage(t);
        String entity = t.entityName();
        boolean ext = t.extendsBase();

        TreeSet<String> imports = new TreeSet<>();
        imports.add("com.baomidou.mybatisplus.annotation.IdType");
        imports.add("com.baomidou.mybatisplus.annotation.TableId");
        imports.add("com.baomidou.mybatisplus.annotation.TableName");
        imports.add("lombok.Data");
        if (ext) {
            imports.add("com.teasaas.framework.mybatis.entity.BaseEntity");
            imports.add("lombok.EqualsAndHashCode");
        } else {
            imports.add("java.io.Serializable");
            if (t.hasDelFlag()) {
                imports.add("com.baomidou.mybatisplus.annotation.TableLogic");
            }
        }
        for (ColumnMeta c : t.businessColumns()) {
            switch (c.javaType()) {
                case "BigDecimal" -> imports.add("java.math.BigDecimal");
                case "LocalDateTime" -> imports.add("java.time.LocalDateTime");
                case "LocalDate" -> imports.add("java.time.LocalDate");
                case "LocalTime" -> imports.add("java.time.LocalTime");
                default -> { }
            }
        }

        StringBuilder fields = new StringBuilder();
        for (ColumnMeta c : t.businessColumns()) {
            if (!c.comment().isBlank()) {
                fields.append("    /** ").append(c.comment()).append(" */\n");
            }
            if (!ext && "del_flag".equals(c.name())) {
                fields.append("    @TableLogic\n");
            }
            fields.append("    private ").append(c.javaType()).append(' ')
                    .append(c.fieldName()).append(";\n\n");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(".entity;\n\n");
        for (String imp : imports) {
            sb.append("import ").append(imp).append(";\n");
        }
        sb.append("\n/**\n * ").append(displayName(t))
                .append("（生成器骨架：业务行为由人工补齐）\n */\n");
        sb.append("@Data\n");
        if (ext) {
            sb.append("@EqualsAndHashCode(callSuper = true)\n");
        }
        sb.append("@TableName(\"").append(t.tableName()).append("\")\n");
        sb.append("public class ").append(entity)
                .append(ext ? " extends BaseEntity {\n" : " implements Serializable {\n");
        sb.append("\n    private static final long serialVersionUID = 1L;\n\n");
        sb.append("    @TableId(type = IdType.ASSIGN_ID)\n    private Long id;\n\n");
        sb.append(fields.toString().stripTrailing()).append("\n}\n");
        return sb.toString();
    }

    // ==================== Mapper ====================

    public static String mapper(TableMeta t) {
        String pkg = basePackage(t);
        String entity = t.entityName();
        return """
                package %s.mapper;

                import com.baomidou.mybatisplus.core.mapper.BaseMapper;
                import %s.entity.%s;
                import org.apache.ibatis.annotations.Mapper;

                /**
                 * %s Mapper（生成器骨架）
                 */
                @Mapper
                public interface %sMapper extends BaseMapper<%s> {
                }
                """.formatted(pkg, pkg, entity, displayName(t), entity, entity);
    }

    public static String mapperXml(TableMeta t) {
        String pkg = basePackage(t);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="%s.mapper.%sMapper">
                    <!-- 生成器骨架：扩展 SQL 手写于此（查询须显式列名，规范 5.2） -->
                </mapper>
                """.formatted(pkg, t.entityName()).stripLeading();
    }

    // ==================== Service ====================

    public static String service(TableMeta t) {
        String pkg = basePackage(t);
        String entity = t.entityName();
        String pageSig = t.keywordColumn() != null
                ? "PageResult<%s> page(int page, int size, String keyword);".formatted(entity)
                : "PageResult<%s> page(int page, int size);".formatted(entity);
        return """
                package %s.service;

                import com.teasaas.common.result.PageResult;
                import %s.entity.%s;

                /**
                 * %s 服务（生成器骨架：业务规则由人工补齐）
                 */
                public interface %sService {

                    %s

                    %s getById(Long id);

                    Long create(%s entity);

                    void update(Long id, %s entity);

                    void delete(Long id);
                }
                """.formatted(pkg, pkg, entity, displayName(t), entity,
                pageSig, entity, entity, entity);
    }

    // ==================== ServiceImpl ====================

    public static String serviceImpl(TableMeta t) {
        String pkg = basePackage(t);
        String entity = t.entityName();
        String mapperField = NameUtil.decapitalize(entity + "Mapper");
        ColumnMeta keyword = t.keywordColumn();
        boolean hasKeyword = keyword != null;

        TreeSet<String> imports = new TreeSet<>();
        imports.add("com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper");
        imports.add("com.baomidou.mybatisplus.extension.plugins.pagination.Page");
        imports.add("com.teasaas.common.exception.BizException");
        imports.add("com.teasaas.common.result.ErrorCode");
        imports.add("com.teasaas.common.result.PageResult");
        imports.add(pkg + ".entity." + entity);
        imports.add(pkg + ".mapper." + entity + "Mapper");
        imports.add(pkg + ".service." + entity + "Service");
        imports.add("lombok.RequiredArgsConstructor");
        imports.add("org.springframework.stereotype.Service");
        imports.add("org.springframework.transaction.annotation.Transactional");
        if (t.hasTenantId()) {
            imports.add("com.teasaas.framework.security.context.SecurityUtils");
        }
        if (!t.extendsBase() && t.hasCreateTime()) {
            imports.add("java.time.LocalDateTime");
        }

        StringBuilder pageBody = new StringBuilder();
        pageBody.append("        LambdaQueryWrapper<").append(entity).append("> wrapper = new LambdaQueryWrapper<")
                .append(entity).append(">()\n");
        if (hasKeyword) {
            pageBody.append("                .like(keyword != null && !keyword.isBlank(), ")
                    .append(entity).append("::").append(keyword.getterName()).append(", keyword)\n");
        }
        pageBody.append("                .orderByDesc(").append(entity).append("::getId);\n");
        pageBody.append("        Page<").append(entity).append("> result = ").append(mapperField)
                .append(".selectPage(Page.of(page, size), wrapper);\n");
        pageBody.append("        return PageResult.of(result.getRecords(), result.getTotal(), page, size);");

        String pageMethod = "    @Override\n"
                + "    public PageResult<" + entity + "> page(int page, int size"
                + (hasKeyword ? ", String keyword" : "") + ") {\n"
                + pageBody + "\n"
                + "    }";

        StringBuilder createLines = new StringBuilder();
        if (t.hasTenantId()) {
            createLines.append("        entity.setTenantId(SecurityUtils.getTenantIdFromSession());\n");
        }
        if (!t.extendsBase() && t.hasCreateTime()) {
            createLines.append("        entity.setCreateTime(LocalDateTime.now());\n");
        }

        String deleteNote = t.extendsBase() || t.hasDelFlag()
                ? "        // 逻辑删除（@TableLogic）；租户插件自动拼 tenant_id 防越权\n"
                : "        // 物理删除（表无 del_flag）；租户插件自动拼 tenant_id 防越权\n";

        return """
                package %s.service.impl;

                %s

                /**
                 * %s 服务实现（生成器骨架：业务规则由人工补齐）
                 */
                @Service
                @RequiredArgsConstructor
                public class %sServiceImpl implements %sService {

                    private final %sMapper %s;

                %s

                    @Override
                    public %s getById(Long id) {
                        %s entity = %s.selectById(id);
                        if (entity == null) {
                            throw new BizException(ErrorCode.PARAM_ERROR, "%s不存在");
                        }
                        return entity;
                    }

                    @Override
                    @Transactional(rollbackFor = Exception.class)
                    public Long create(%s entity) {
                %s        %s.insert(entity);
                        return entity.getId();
                    }

                    @Override
                    @Transactional(rollbackFor = Exception.class)
                    public void update(Long id, %s entity) {
                        entity.setId(id);
                        %s.updateById(entity);
                    }

                    @Override
                    @Transactional(rollbackFor = Exception.class)
                    public void delete(Long id) {
                %s        %s.deleteById(id);
                    }
                }
                """.formatted(pkg, importLines(imports), displayName(t), entity, entity,
                entity, mapperField, pageMethod, entity, entity, mapperField, displayName(t),
                entity, createLines, mapperField, entity, mapperField, deleteNote, mapperField);
    }

    // ==================== Controller ====================

    public static String controller(TableMeta t) {
        String pkg = basePackage(t);
        String entity = t.entityName();
        String serviceField = NameUtil.decapitalize(entity + "Service");
        String url = "/api/v1/" + t.moduleKey() + "/" + t.resourceName();
        boolean hasKeyword = t.keywordColumn() != null;

        String pageMethod = "    @Operation(summary = \"分页列表\")\n"
                + "    @GetMapping\n"
                + "    public Result<PageResult<" + entity + ">> page(@RequestParam(defaultValue = \"1\") int page,\n"
                + "                                      @RequestParam(defaultValue = \"10\") int size"
                + (hasKeyword
                ? ",\n                                      @RequestParam(required = false) String keyword"
                : "")
                + ") {\n"
                + "        return Result.success(" + serviceField + ".page(page, size"
                + (hasKeyword ? ", keyword" : "") + "));\n"
                + "    }";

        return """
                package %s.controller;

                import %s.entity.%s;
                import %s.service.%sService;
                import com.teasaas.common.result.PageResult;
                import com.teasaas.common.result.Result;
                import com.teasaas.system.annotation.OpLog;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import lombok.RequiredArgsConstructor;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.DeleteMapping;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.PutMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RestController;

                /**
                 * %s接口（生成器骨架：路径/入参 DTO/权限码以接口需求文档为准，人工 review 后校正）
                 */
                @Validated
                @RestController
                @RequestMapping("%s")
                @RequiredArgsConstructor
                @Tag(name = "%s", description = "生成器骨架：接口契约以 docs/茶饮SaaS-接口需求.md 为准")
                public class %sController {

                    private final %sService %s;

                %s

                    @Operation(summary = "详情")
                    @GetMapping("/{id}")
                    public Result<%s> getById(@PathVariable Long id) {
                        return Result.success(%s.getById(id));
                    }

                    @OpLog(module = "%s", action = "新增%s")
                    @Operation(summary = "新增")
                    @PostMapping
                    public Result<Long> create(@RequestBody %s entity) {
                        return Result.success(%s.create(entity));
                    }

                    @OpLog(module = "%s", action = "修改%s")
                    @Operation(summary = "修改")
                    @PutMapping("/{id}")
                    public Result<Void> update(@PathVariable Long id, @RequestBody %s entity) {
                        %s.update(id, entity);
                        return Result.success();
                    }

                    @OpLog(module = "%s", action = "删除%s")
                    @Operation(summary = "删除")
                    @DeleteMapping("/{id}")
                    public Result<Void> delete(@PathVariable Long id) {
                        %s.delete(id);
                        return Result.success();
                    }
                }
                """.formatted(pkg, pkg, entity, pkg, entity, displayName(t), url,
                tag(t), entity, entity, serviceField, pageMethod,
                entity, serviceField,
                t.moduleKey(), displayName(t), entity, serviceField,
                t.moduleKey(), displayName(t), entity, serviceField,
                t.moduleKey(), displayName(t), serviceField);
    }

    // ==================== 公共 ====================

    private static String basePackage(TableMeta t) {
        return ModuleRouter.basePackage(ModuleRouter.artifact(t.tableName()));
    }

    /** 类注释显示名：优先表注释，兜底表名 */
    private static String displayName(TableMeta t) {
        return t.tableComment().isBlank() ? t.tableName() : t.tableComment();
    }

    /** @Tag 名称：模块 key - 表注释 */
    private static String tag(TableMeta t) {
        return t.moduleKey().toUpperCase() + "-" + displayName(t);
    }

    private static String importLines(TreeSet<String> imports) {
        StringBuilder sb = new StringBuilder();
        for (String imp : imports) {
            sb.append("import ").append(imp).append(";\n");
        }
        return sb.toString().stripTrailing();
    }
}
