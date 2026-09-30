package com.teasaas.job.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务开关（tea-job，随 tea-server 装配）
 * 任务本体不持会话：各服务内部自行遍历租户（TenantContext set/clear）
 * 且不依赖登录用户（userId 判空兜底），见各 Job 注释。
 *
 * <p>tea.job.enabled=false 可整体停用调度（集成测试默认关闭，
 * 防止超时取消/回调重试任务干扰并发用例的订单与流水状态）。</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "tea.job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JobConfig {
}
