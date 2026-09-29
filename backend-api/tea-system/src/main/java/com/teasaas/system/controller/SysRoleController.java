package com.teasaas.system.controller;

import com.teasaas.common.result.Result;
import com.teasaas.system.annotation.OpLog;
import com.teasaas.system.entity.SysRole;
import com.teasaas.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理接口（接口需求 2.3；权限：R2）
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "角色列表/菜单授权")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @Operation(summary = "角色列表（R1-R8 预置）")
    @GetMapping
    public Result<List<SysRole>> list() {
        return Result.success(sysRoleService.list());
    }

    @Operation(summary = "角色已绑菜单 ID（授权回显）")
    @GetMapping("/{id}/menus")
    public Result<List<Long>> menuIds(@PathVariable("id") Long roleId) {
        return Result.success(sysRoleService.listMenuIds(roleId));
    }

    @OpLog(module = "system", action = "角色菜单授权")
    @Operation(summary = "更新角色菜单授权（全量重绑）")
    @PutMapping("/{id}/menus")
    public Result<Void> grantMenus(@PathVariable("id") Long roleId,
                                   @RequestBody List<Long> menuIds) {
        sysRoleService.grantMenus(roleId, menuIds);
        return Result.success();
    }
}
