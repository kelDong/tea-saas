package com.teasaas.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teasaas.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * SysUser Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 登录专用查询。
     * 【多租户绕过声明（工程规范 6.1）】登录认证时租户上下文未知（tenant_id 尚未解析），
     * 用户名必须全局唯一查询；登录成功后才把该用户的 tenant_id 注入 TenantContext。
     * 此处显式绕过租户插件，已过评审；业务查询禁止模仿本方法。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id, tenant_id, username, password, nickname, phone, user_type, status, del_flag "
            + "FROM sys_user WHERE username = #{username} AND del_flag = 0 LIMIT 1")
    SysUser selectByUsernameForLogin(@Param("username") String username);
}
