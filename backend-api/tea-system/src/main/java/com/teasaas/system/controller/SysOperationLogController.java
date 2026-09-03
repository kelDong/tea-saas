package com.teasaas.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.system.entity.SysOperationLog;
import com.teasaas.system.mapper.SysOperationLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询（F-SYS-04；权限：R2 全量——租户插件自动过滤本租户）
 */
@RestController
@RequestMapping("/api/v1/operation-logs")
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "关键操作留痕查询")
public class SysOperationLogController {

    private final SysOperationLogMapper sysOperationLogMapper;

    @Operation(summary = "日志列表（分页，按时间/模块筛选）")
    @GetMapping
    public Result<PageResult<SysOperationLog>> page(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(required = false) String module) {
        Page<SysOperationLog> result = sysOperationLogMapper.selectPage(
                Page.of(page, size),
                new LambdaQueryWrapper<SysOperationLog>()
                        .eq(module != null && !module.isBlank(), SysOperationLog::getModule, module)
                        .orderByDesc(SysOperationLog::getCreateTime));
        return Result.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }
}
