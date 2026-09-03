package com.teasaas.system.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.system.annotation.OpLog;
import com.teasaas.system.entity.SysOperationLog;
import com.teasaas.system.mapper.SysOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面（F-SYS-04：关键操作全留痕）
 * 注意：日志写入失败不影响业务（吞异常记 WARN——规范 9.2 允许的注释豁免场景）
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OpLogAspect {

    private final SysOperationLogMapper sysOperationLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        Throwable error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                saveLog(joinPoint, opLog, System.currentTimeMillis() - start, error == null ? 1 : 0);
            } catch (Exception ex) {
                // 日志落库失败不影响业务（关键操作已有业务流水兜底）
                log.warn("operation log save failed: {}", ex.getMessage());
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, OpLog opLog, long costMs, int status) {
        SysOperationLog entity = new SysOperationLog();
        entity.setModule(opLog.module());
        entity.setAction(opLog.action());
        entity.setMethod(joinPoint.getSignature().toShortString());
        entity.setCostMs((int) costMs);
        entity.setStatus(status);
        try {
            entity.setUserId(StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null);
        } catch (Exception ignore) {
            entity.setUserId(null);
        }
        Long tenantId = TenantContext.getTenantId();
        entity.setTenantId(tenantId == null ? 0L : tenantId);
        try {
            entity.setParams(truncate(objectMapper.writeValueAsString(joinPoint.getArgs()), 2000));
        } catch (Exception ignore) {
            entity.setParams("serialize-failed");
        }
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            entity.setIp(request.getRemoteAddr());
        }
        sysOperationLogMapper.insert(entity);
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
