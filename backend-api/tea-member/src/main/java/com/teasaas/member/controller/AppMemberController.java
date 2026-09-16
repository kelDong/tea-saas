package com.teasaas.member.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.framework.security.context.MemberContext;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端会员接口（F-MEM-01/02/03，接口需求 10.1–10.3）
 *
 * <p>身份与会话（Review §2 会话化）：AppMemberInterceptor 解析
 * Authorization: Bearer token（兼容期 X-Member-Id 双头 WARN 放行）注入
 * MemberContext + TenantContext，本类统一从 MemberContext 取身份，
 * 不再手动解析请求头；请求结束由拦截器统一清理 ThreadLocal。</p>
 */
@Validated
@RestController
@RequestMapping("/api/app/v1/members")
@RequiredArgsConstructor
@Tag(name = "MEM-C端会员", description = "会员信息/积分明细/储值")
public class AppMemberController {

    private final MemMemberService memMemberService;
    private final MemPointsService memPointsService;
    private final MemWalletService memWalletService;

    @Operation(summary = "会员信息（档案+等级+积分+余额）")
    @GetMapping("/me")
    public Result<MemberVO> me() {
        return Result.success(memMemberService.me(MemberContext.getMemberId()));
    }

    @Operation(summary = "绑定手机号")
    @PutMapping("/me/phone")
    public Result<Void> bindPhone(@RequestBody @Validated PhoneBindDTO dto) {
        memMemberService.bindPhone(MemberContext.getMemberId(), dto);
        return Result.success();
    }

    @Operation(summary = "积分明细")
    @GetMapping("/me/points")
    public Result<PageResult<MemPointsLog>> points(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(memPointsService.pageLogs(MemberContext.getMemberId(), page, size));
    }

    @Operation(summary = "充值下单（一期 mock 即时到账）")
    @PostMapping("/me/wallet/recharge")
    public Result<WalletVO> recharge(@RequestBody @Validated RechargeDTO dto) {
        return Result.success(memWalletService.recharge(MemberContext.getMemberId(), dto));
    }

    @Operation(summary = "储值账户")
    @GetMapping("/me/wallet")
    public Result<WalletVO> wallet() {
        return Result.success(memWalletService.getWallet(MemberContext.getMemberId()));
    }

    @Operation(summary = "储值流水（一期=充值记录）")
    @GetMapping("/me/wallet/logs")
    public Result<PageResult<MemRechargeRecord>> walletLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(memWalletService.pageLogs(MemberContext.getMemberId(), page, size));
    }
}
