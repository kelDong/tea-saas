package com.teasaas.framework.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.teasaas.framework.tenant.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * MyBatis-Plus 配置（多租户核心，数据库设计第 14 章）
 * 1. TenantLineInnerInterceptor：所有 SQL 自动追加 tenant_id = ?
 * 2. PaginationInnerInterceptor：分页插件（规范 5.2：禁止内存分页）
 * 3. @MapperScan：主类在 com.teasaas.server 包，@Mapper 自动扫描覆盖不到其他模块的
 *    mapper 包（M2 遗留缺陷，首次启动冒烟暴露）；限定 annotationClass=@Mapper 只注册
 *    mapper 接口，业务接口不受影响，后续新增模块无需改此配置
 */
@Configuration
@MapperScan(basePackages = "com.teasaas", annotationClass = Mapper.class)
public class MybatisPlusConfig {

    /**
     * 不参与租户隔离的表（平台表，数据库设计 14.2 / 15.2）
     * sys_role/sys_role_menu 存平台预置数据（R1-R8 + 默认菜单授权，tenant_id=0 全租户共用），
     * 租户隔离锚点是 sys_user_role（仍按 tenant_id 过滤）——用户可达的角色只来自本租户绑定行。
     * 注意：二期若开放租户自定义角色/授权，这两表需重新评估（回测发现 brand 租户登录角色为空的问题）。
     */
    private static final Set<String> IGNORE_TABLES = Set.of(
            "tenant", "sys_package", "sys_menu", "sys_role", "sys_role_menu"
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 租户插件必须放在分页插件之前
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContext.getTenantId();
                if (tenantId == null) {
                    // 未登录上下文（如定时任务）默认平台租户，业务代码需显式设置
                    return new LongValue(TenantContext.SUPER_TENANT);
                }
                return new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return IGNORE_TABLES.contains(tableName.toLowerCase());
            }
        }));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 审计字段自动填充（BaseEntity：create_by/create_time/update_by/update_time）
     */
    @Bean
    public MetaObjectHandler auditMetaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
