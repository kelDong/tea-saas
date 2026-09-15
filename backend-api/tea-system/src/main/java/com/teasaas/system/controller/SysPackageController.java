package com.teasaas.system.controller;

import com.teasaas.common.result.Result;
import com.teasaas.system.service.SysPackageService;
import com.teasaas.system.vo.SysPackageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 套餐查询接口（F-SYS-01 后半；接口需求 2.2；套餐 CRUD 二期细化）
 * 权限：R1 平台管理员（Service 内校验）
 */
@RestController
@RequestMapping("/api/admin/v1/packages")
@RequiredArgsConstructor
@Tag(name = "套餐管理", description = "平台侧套餐列表（R1 专属）")
public class SysPackageController {

    private final SysPackageService sysPackageService;

    @Operation(summary = "套餐列表（含停用，含功能开关 JSON）")
    @GetMapping
    public Result<List<SysPackageVO>> list() {
        return Result.success(sysPackageService.list());
    }
}
