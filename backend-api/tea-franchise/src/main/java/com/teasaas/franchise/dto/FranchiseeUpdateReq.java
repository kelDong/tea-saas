package com.teasaas.franchise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 编辑加盟商入参（F-FRA-01；编码可改但同样走租户内查重）
 */
@Data
public class FranchiseeUpdateReq {

    @NotBlank(message = "加盟商编码不能为空")
    @Size(max = 32, message = "加盟商编码最长 32 字符")
    private String franchiseeCode;

    @NotBlank(message = "加盟商名称不能为空")
    @Size(max = 64, message = "加盟商名称最长 64 字符")
    private String franchiseeName;

    @Size(max = 32, message = "联系人最长 32 字符")
    private String contactName;

    @Size(max = 20, message = "联系电话最长 20 字符")
    private String contactPhone;

    private LocalDate joinDate;
}
