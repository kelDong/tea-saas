package com.teasaas.franchise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 加盟商等级评定入参（F-FRA-01；等级决定采购分级价 F-SUP-02，联动不在本模块）
 */
@Data
public class FranchiseeLevelReq {

    @NotBlank(message = "等级不能为空")
    @Pattern(regexp = "^[ABC]$", message = "等级仅支持 A/B/C")
    private String levelCode;

    /** 评定备注（fra_franchisee 无 remark 列，暂不落库，仅操作日志留痕） */
    @Size(max = 255, message = "备注最长 255 字符")
    private String remark;
}
