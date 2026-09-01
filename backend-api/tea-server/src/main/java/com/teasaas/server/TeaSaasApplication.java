package com.teasaas.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 茶饮连锁加盟 SaaS 管理系统 - 启动入口
 * 扫描范围：com.teasaas（全部业务模块）
 */
@SpringBootApplication(scanBasePackages = "com.teasaas")
public class TeaSaasApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeaSaasApplication.class, args);
    }
}
