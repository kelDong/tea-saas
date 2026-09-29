package com.teasaas.system.controller;

import com.teasaas.common.result.Result;
import com.teasaas.system.service.SysMenuService;
import com.teasaas.system.vo.MenuTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统菜单接口（F-SYS-02，接口需求 2.3；权限：R2）
 * sys_menu 为平台共享表（租户插件 IGNORE_TABLES），本接口返回全量树（含按钮节点）供角色授权勾选。
 */
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@Tag(name = "系统菜单", description = "全量菜单树（角色授权勾选用）")
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @Operation(summary = "全量菜单树（目录/菜单/按钮；不过滤停用/隐藏——授权视角）")
    @GetMapping("/tree")
    public Result<List<MenuTreeVO>> tree() {
        return Result.success(sysMenuService.tree());
    }
}
