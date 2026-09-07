package com.teasaas.generator;

import com.teasaas.generator.core.ModuleRouter;
import com.teasaas.generator.core.SqlDdlParser;
import com.teasaas.generator.core.TableMeta;
import com.teasaas.generator.render.Templates;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 离线代码生成器（M2，HANDOFF §3）：解析 sql/init/01_schema.sql，按表前缀路由模块，
 * 生成 Entity / Mapper / Mapper.xml / Service / ServiceImpl / Controller 六件。
 *
 * <p>用法（backend-api 目录下执行）：
 * <pre>
 * java -cp tea-generator/target/classes com.teasaas.generator.CodeGenerator ^
 *      --ddl=sql/init/01_schema.sql --out=. --tables=bom_material,bom_formula
 * </pre>
 * --tables 支持前缀通配（bom_*）；缺省生成全部表。
 *
 * <p>规则：
 * <ul>
 *   <li>已存在的输出文件一律跳过（保护手写代码，如 SysUser 系列）</li>
 *   <li>通用字段全含的表实体继承 BaseEntity；关联/流水/平台表字段平铺</li>
 *   <li>生成产物须人工 review 后方可合入（工程规范 14：禁止"生成即合入"）</li>
 * </ul>
 */
public final class CodeGenerator {

    private static final Logger log = Logger.getLogger(CodeGenerator.class.getName());

    private CodeGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path ddl = null;
        Path out = null;
        Set<String> filter = Set.of();
        for (String arg : args) {
            if (arg.startsWith("--ddl=")) {
                ddl = Path.of(arg.substring("--ddl=".length()));
            } else if (arg.startsWith("--out=")) {
                out = Path.of(arg.substring("--out=".length()));
            } else if (arg.startsWith("--tables=")) {
                filter = Set.of(arg.substring("--tables=".length()).split(","));
            } else {
                throw new IllegalArgumentException("无法识别的参数：" + arg);
            }
        }
        if (ddl == null || out == null) {
            throw new IllegalArgumentException(
                    "必填参数：--ddl=<schema.sql> --out=<backend-api 根目录>（可选 --tables=t1,t2 / 前缀*）");
        }

        List<TableMeta> all = SqlDdlParser.parse(Files.readString(ddl, StandardCharsets.UTF_8));
        final Set<String> tableFilter = filter;
        List<TableMeta> targets = all.stream()
                .filter(t -> tableFilter.isEmpty() || matches(t.tableName(), tableFilter))
                .toList();

        int attempts = 0;
        int created = 0;
        for (TableMeta t : targets) {
            String module = ModuleRouter.artifact(t.tableName());
            String pkgPath = ModuleRouter.basePackage(module).replace('.', '/');
            Path javaRoot = out.resolve(module).resolve("src/main/java").resolve(pkgPath);
            Path xmlRoot = out.resolve(module).resolve("src/main/resources/mapper");

            attempts += 6;
            created += writeIfAbsent(javaRoot.resolve("entity/" + t.entityName() + ".java"),
                    Templates.entity(t));
            created += writeIfAbsent(javaRoot.resolve("mapper/" + t.entityName() + "Mapper.java"),
                    Templates.mapper(t));
            created += writeIfAbsent(xmlRoot.resolve(t.entityName() + "Mapper.xml"),
                    Templates.mapperXml(t));
            created += writeIfAbsent(javaRoot.resolve("service/" + t.entityName() + "Service.java"),
                    Templates.service(t));
            created += writeIfAbsent(javaRoot.resolve("service/impl/" + t.entityName() + "ServiceImpl.java"),
                    Templates.serviceImpl(t));
            created += writeIfAbsent(javaRoot.resolve("controller/" + t.entityName() + "Controller.java"),
                    Templates.controller(t));
        }

        int skipped = attempts - created;
        log.info("DDL 解析 " + all.size() + " 张表，命中 " + targets.size() + " 张；"
                + "生成 " + created + " 个文件，跳过已存在 " + skipped + " 个");
    }

    /** 已存在则跳过（保护手写代码）；返回 1=已生成 / 0=已跳过 */
    private static int writeIfAbsent(Path file, String content) throws IOException {
        if (Files.exists(file)) {
            log.info("跳过（已存在，保护手写代码）: " + file);
            return 0;
        }
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        log.info("生成: " + file);
        return 1;
    }

    /** 精确匹配或前缀通配（bom_*） */
    private static boolean matches(String tableName, Set<String> filter) {
        for (String f : filter) {
            String p = f.trim();
            if (p.isEmpty()) {
                continue;
            }
            if (p.endsWith("*") ? tableName.startsWith(p.substring(0, p.length() - 1))
                    : tableName.equals(p)) {
                return true;
            }
        }
        return false;
    }
}
