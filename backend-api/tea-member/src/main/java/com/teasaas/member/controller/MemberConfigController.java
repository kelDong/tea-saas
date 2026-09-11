package com.teasaas.member.controller;

import com.teasaas.common.result.Result;
import com.teasaas.member.dto.PointsConfigDTO;
import com.teasaas.member.service.MemMemberService;
import com.teasaas.member.vo.PointsConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 积分/储值规则配置接口（F-MEM-02/03，接口需求 10.2/10.3；会话鉴权 R2/R5）
 */
@Validated
@RestController
@RequestMapping("/api/v1/member-configs")
@RequiredArgsConstructor
@Tag(name = "MEM-规则配置", description = "积分/储值规则配置")
public class MemberConfigController {

    private final MemMemberService memMemberService;

    @Operation(summary = "积分/储值规则查询（配置页回显）")
    @GetMapping("/points")
    public Result<PointsConfigVO> get() {
        return Result.success(memMemberService.getPointsConfig());
    }

    @Operation(summary = "积分/储值规则配置")
    @PutMapping("/points")
    public Result<Void> update(@RequestBody @Validated PointsConfigDTO dto) {
        memMemberService.updatePointsConfig(dto);
        return Result.success();
    }
}
