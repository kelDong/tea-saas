package com.teasaas.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.core.util.IdUtil;
import com.teasaas.common.constant.RedisConstants;
import com.teasaas.common.result.Result;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.system.dto.LoginReq;
import com.teasaas.system.service.AuthService;
import com.teasaas.system.vo.LoginVO;
import com.teasaas.system.vo.MenuTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口（F-SYS-03；接口需求 2.1）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "登录/登出/验证码/菜单权限")
public class AuthController {

    private final AuthService authService;
    private final StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "登录（账号+密码，验证码一期可选）")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginReq req) {
        return Result.success(authService.login(req));
    }

    @Operation(summary = "获取图形验证码（返回 key + base64 图）")
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        var captcha = CaptchaUtil.createCircleCaptcha(120, 40, 4, 10);
        String key = IdUtil.fastSimpleUUID();
        // 5 分钟有效，一次性使用
        stringRedisTemplate.opsForValue().set(RedisConstants.CAPTCHA + key,
                captcha.getCode(), Duration.ofMinutes(5));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        captcha.write(bos);
        Map<String, String> data = new HashMap<>();
        data.put("key", key);
        data.put("image", "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray()));
        return Result.success(data);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", StpUtil.getLoginIdAsLong());
        data.put("tenantId", SecurityUtils.getTenantIdFromSession());
        data.put("roles", SecurityUtils.getRolesFromSession());
        data.put("permissions", SecurityUtils.getPermsFromSession());
        return Result.success(data);
    }

    @Operation(summary = "菜单树+按钮权限码（前端动态路由输入）")
    @GetMapping("/menus")
    public Result<MenuTreeVO.MenusPayload> menus() {
        return Result.success(authService.menus());
    }
}
