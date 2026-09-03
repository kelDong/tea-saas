package com.teasaas.server.runner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.framework.security.context.PasswordUtil;
import com.teasaas.system.entity.SysUser;
import com.teasaas.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 平台管理员密码初始化（02_data.sql 的 BCrypt 为占位哈希）
 * 用法：首次部署时设置环境变量 TEA_ADMIN_PASSWORD=你的密码 再启动，自动替换占位值；
 * 不设置则仅告警提示。运行一次后即可移除环境变量。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminPasswordInitRunner implements CommandLineRunner {

    private static final String PLACEHOLDER = "{bcrypt}REPLACE_WITH_BCRYPT_HASH_ON_FIRST_BOOT";

    private final SysUserMapper sysUserMapper;

    @Override
    public void run(String... args) {
        String envPassword = System.getenv("TEA_ADMIN_PASSWORD");
        try {
            SysUser admin = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, "admin").last("LIMIT 1"));
            if (admin == null || !PLACEHOLDER.equals(admin.getPassword())) {
                return; // 已初始化或账号不存在
            }
            if (envPassword == null || envPassword.isBlank()) {
                log.warn("==============================================");
                log.warn("⚠️ 平台管理员(admin)密码仍为占位值，无法登录！");
                log.warn("   设置环境变量 TEA_ADMIN_PASSWORD 后重启即可自动初始化");
                log.warn("==============================================");
                return;
            }
            SysUser update = new SysUser();
            update.setId(admin.getId());
            update.setPassword(PasswordUtil.encode(envPassword));
            sysUserMapper.updateById(update);
            log.info("✅ 平台管理员(admin)初始密码已由 TEA_ADMIN_PASSWORD 环境变量设置");
        } catch (Exception ex) {
            // 数据库未初始化等场景不阻断启动（可能是首次启动前检查）
            log.warn("admin 密码初始化检查跳过：{}", ex.getMessage());
        }
    }
}
