package com.teasaas.job.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务开关（tea-job，随 tea-server 装配）
 * 任务本体不持会话：各服务内部自行遍历租户（TenantContext set/clear）
 * 且不依赖登录用户（userId 判空兜底），见各 Job 注释。
 */
@Configuration
@EnableScheduling
public class JobConfig {
}
