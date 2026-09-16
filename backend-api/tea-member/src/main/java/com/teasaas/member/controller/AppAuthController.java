package com.teasaas.member.controller;

import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.Result;
import com.teasaas.framework.security.context.MemberTokenService;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.member.dto.WxLoginDTO;
import com.teasaas.member.service.MemMemberService;
import com.teasaas.member.vo.WxLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端登录接口（F-MEM-01，接口需求 10.1；白名单公开）
 *
 * <p>【多租户红线唯一例外声明】公开路径不经过会话租户注入，从此处读取
 * X-Tenant-Id 请求头手动设置 TenantContext，建档仍由租户插件自动拼
 * tenant_id 隔离；请求结束由 TenantCleanupInterceptor 统一清理 ThreadLocal。</p>
 *
 * <p>一期 mock：code 即模拟 openid；登录成功签发 C 端会话 token
 * （Review §2 会话化），前端后续经 Authorization: Bearer 携带；
 * 兼容期一个迭代内仍可走 X-Member-Id 头（拦截器 WARN 放行）。</p>
 */
@Validated
@RestController
@RequestMapping("/api/app/v1/auth")
@RequiredArgsConstructor
@Tag(name = "MEM-C端登录", description = "微信登录建档")
public class AppAuthController {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final MemMemberService memMemberService;
    private final MemberTokenService memberTokenService;

    @Operation(summary = "微信登录建档（一期 mock：code 即 openid）")
    @PostMapping("/wx-login")
    public Result<WxLoginVO> wxLogin(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestBody @Validated WxLoginDTO dto) {
        if (tenantId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少品牌标识");
        }
        TenantContext.setTenantId(tenantId);
        WxLoginVO vo = memMemberService.wxLogin(dto);
        // 事务提交后签发（避免建档回滚却留下指向不存在会员的 token）
        vo.setToken(memberTokenService.issue(tenantId, vo.getMemberId()));
        return Result.success(vo);
    }
}
