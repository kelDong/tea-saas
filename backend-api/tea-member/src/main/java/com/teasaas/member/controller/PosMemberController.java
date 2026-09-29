package com.teasaas.member.controller;

import com.teasaas.common.result.PageResult;
import com.teasaas.common.result.Result;
import com.teasaas.member.service.MemMemberService;
import com.teasaas.member.vo.PosMemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * POS 会员搜索接口（F-ORD-04 储值收款二期，接口需求 8.4；会话鉴权 R7/R8）
 *
 * <p>最小字段集（昵称/手机号/储值余额），不透出 openid 等敏感字段；
 * 供收银台储值收款选会员，角色校验同 posPay 口径。</p>
 */
@RestController
@RequestMapping("/api/v1/pos/members")
@RequiredArgsConstructor
@Tag(name = "ORD-POS会员", description = "收银台会员搜索")
public class PosMemberController {

    private final MemMemberService memMemberService;

    @Operation(summary = "POS会员搜索（昵称/手机号模糊，带储值余额）")
    @GetMapping
    public Result<PageResult<PosMemberVO>> search(
            @Parameter(description = "昵称/手机号关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return Result.success(memMemberService.searchForPos(keyword, page, size));
    }
}
