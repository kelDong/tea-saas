package com.teasaas.member.controller;

import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.member.dto.PhoneBindDTO;
import com.teasaas.member.dto.RechargeDTO;
import com.teasaas.member.entity.MemPointsLog;
import com.teasaas.member.entity.MemRechargeRecord;
import com.teasaas.member.service.MemMemberService;
import com.teasaas.member.service.MemPointsService;
import com.teasaas.member.service.MemWalletService;
import com.teasaas.member.vo.MemberVO;
import com.teasaas.member.vo.WalletVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端会员接口（F-MEM-01/02/03，接口需求 10.1–10.3；白名单公开）
 *
 * <p>【多租户红线唯一例外声明】公开路径不经过会话租户注入，从此处读取
 * X-Tenant-Id 请求头手动设置 TenantContext，查询仍由租户插件自动拼
 * tenant_id 隔离；请求结束由 TenantCleanupInterceptor 统一清理 ThreadLocal。</p>
 *
 * <p>会员身份：wx-login 返回 memberId 后，前端经 X-Member-Id 请求头携带
 * （开发桥接，正式 C 端会话二期替换）。</p>
 */
@Validated
@RestController
@RequestMapping("/api/app/v1/members")
@RequiredArgsConstructor
@Tag(name = "MEM-C端会员", description = "会员信息/积分明细/储值")
public class AppMemberController {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String MEMBER_HEADER = "X-Member-Id";

    private final MemMemberService memMemberService;
    private final MemPointsService memPointsService;
    private final MemWalletService memWalletService;

    @Operation(summary = "会员信息（档案+等级+积分+余额）")
    @GetMapping("/me")
    public Result<MemberVO> me(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestHeader(value = MEMBER_HEADER, required = false)
            @Parameter(description = "会员ID，必填") Long memberId) {
        require(tenantId, memberId);
        return Result.success(memMemberService.me(memberId));
    }

    @Operation(summary = "绑定手机号")
    @PutMapping("/me/phone")
    public Result<Void> bindPhone(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestHeader(value = MEMBER_HEADER, required = false)
            @Parameter(description = "会员ID，必填") Long memberId,
            @RequestBody @Validated PhoneBindDTO dto) {
        require(tenantId, memberId);
        memMemberService.bindPhone(memberId, dto);
        return Result.success();
    }

    @Operation(summary = "积分明细")
    @GetMapping("/me/points")
    public Result<PageResult<MemPointsLog>> points(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestHeader(value = MEMBER_HEADER, required = false)
            @Parameter(description = "会员ID，必填") Long memberId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        require(tenantId, memberId);
        return Result.success(memPointsService.pageLogs(memberId, page, size));
    }

    @Operation(summary = "充值下单（一期 mock 即时到账）")
    @PostMapping("/me/wallet/recharge")
    public Result<WalletVO> recharge(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestHeader(value = MEMBER_HEADER, required = false)
            @Parameter(description = "会员ID，必填") Long memberId,
            @RequestBody @Validated RechargeDTO dto) {
        require(tenantId, memberId);
        return Result.success(memWalletService.recharge(memberId, dto));
    }

    @Operation(summary = "储值账户")
    @GetMapping("/me/wallet")
    public Result<WalletVO> wallet(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestHeader(value = MEMBER_HEADER, required = false)
            @Parameter(description = "会员ID，必填") Long memberId) {
        require(tenantId, memberId);
        return Result.success(memWalletService.getWallet(memberId));
    }

    @Operation(summary = "储值流水（一期=充值记录）")
    @GetMapping("/me/wallet/logs")
    public Result<PageResult<MemRechargeRecord>> walletLogs(
            @RequestHeader(value = TENANT_HEADER, required = false)
            @Parameter(description = "品牌标识（租户 ID），必填") Long tenantId,
            @RequestHeader(value = MEMBER_HEADER, required = false)
            @Parameter(description = "会员ID，必填") Long memberId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        require(tenantId, memberId);
        return Result.success(memWalletService.pageLogs(memberId, page, size));
    }

    private void require(Long tenantId, Long memberId) {
        if (tenantId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少品牌标识");
        }
        if (memberId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少会员标识");
        }
        TenantContext.setTenantId(tenantId);
    }
}
