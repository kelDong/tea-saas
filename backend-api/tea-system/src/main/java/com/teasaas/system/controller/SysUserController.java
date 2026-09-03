package com.teasaas.system.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.system.annotation.OpLog;
import com.teasaas.system.dto.SysUserCreateReq;
import com.teasaas.system.dto.SysUserUpdateReq;
import com.teasaas.system.service.SysUserService;
import com.teasaas.system.vo.SysUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口（F-SYS-02 后半；接口需求 2.3）
 * 权限：R2 全量；R6/R7 本店（Service 内按数据权限过滤）
 */
@Validated
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "员工账号/角色绑定/门店绑定")
public class SysUserController {

    private final SysUserService sysUserService;

    @Operation(summary = "用户列表（分页，按数据权限过滤）")
    @GetMapping
    public Result<PageResult<SysUserVO>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword) {
        return Result.success(sysUserService.page(page, size, keyword));
    }

    @OpLog(module = "system", action = "创建用户")
    @Operation(summary = "创建用户（初始密码 BCrypt 加密）")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysUserCreateReq req) {
        return Result.success(sysUserService.create(req));
    }

    @OpLog(module = "system", action = "编辑用户")
    @Operation(summary = "编辑用户（角色/门店重绑）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUserUpdateReq req) {
        sysUserService.update(id, req);
        return Result.success();
    }

    @OpLog(module = "system", action = "重置密码")
    @Operation(summary = "重置密码（重置后强制下线）")
    @PatchMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @RequestBody @Valid PasswordResetBody body) {
        sysUserService.resetPassword(id, body.newPassword());
        return Result.success();
    }

    /** 重置密码入参 */
    public record PasswordResetBody(
            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度 6-32 位")
            String newPassword) {
    }
}
