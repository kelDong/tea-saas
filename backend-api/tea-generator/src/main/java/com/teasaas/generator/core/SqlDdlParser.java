package com.teasaas.generator.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 离线解析 01_schema.sql 的 CREATE TABLE DDL（无数据库连接依赖）
 *
 * <p>约定：列行以反引号开头；PRIMARY KEY / UNIQUE KEY / KEY 等约束行以关键字开头，跳过。
 */
public final class SqlDdlParser {

    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "CREATE\\s+TABLE\\s+`(?<table>[a-zA-Z0-9_]+)`\\s*\\((?<body>.*?)\\)\\s*ENGINE(?<tail>[^;]*)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** 兼容列级 "COMMENT 'x'" 与表级 "COMMENT='x'" 两种写法 */
    private static final Pattern COMMENT_PATTERN =
            Pattern.compile("COMMENT\\s*=?\\s*'(?<comment>(?:[^']|'')*)'");

    private SqlDdlParser() {
    }

    /** 解析全部建表语句 */
    public static List<TableMeta> parse(String ddl) {
        List<TableMeta> tables = new ArrayList<>();
        Matcher m = TABLE_PATTERN.matcher(ddl);
        while (m.find()) {
            String table = m.group("table").toLowerCase();
            String tableComment = extractComment(m.group("tail"));
            tables.add(new TableMeta(table, tableComment, parseColumns(m.group("body"))));
        }
        return tables;
    }

    private static List<ColumnMeta> parseColumns(String body) {
        List<ColumnMeta> cols = new ArrayList<>();
        for (String line : body.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("`")) {
                continue;
            }
            int end = trimmed.indexOf('`', 1);
            String name = trimmed.substring(1, end).toLowerCase();
            String rest = trimmed.substring(end + 1).trim();
            String rawType = rest.split("\\s+")[0];
            cols.add(new ColumnMeta(name, rawType, extractComment(rest)));
        }
        return cols;
    }

    private static String extractComment(String text) {
        Matcher c = COMMENT_PATTERN.matcher(text);
        return c.find() ? c.group("comment").replace("''", "'") : "";
    }
}
