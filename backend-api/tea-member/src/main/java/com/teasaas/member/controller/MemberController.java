package com.teasaas.member.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.member.entity.MemMember;
import com.teasaas.member.service.MemMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台会员接口（F-MEM-01，接口需求 10.1；会话鉴权 R2/R5/R6）
 *
 * <p>租户经会话注入；R6 一期见全租户会员（会员表无门店归属字段，
 * "名下"过滤依赖订单关联，二期细化）。</p>
 */
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "MEM-后台会员", description = "会员列表")
public class MemberController {

    private final MemMemberService memMemberService;

    @Operation(summary = "会员列表（昵称/手机号模糊）")
    @GetMapping
    public Result<PageResult<MemMember>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "昵称/手机号关键字") @RequestParam(required = false) String keyword) {
        return Result.success(memMemberService.page(page, size, keyword));
    }
}
