package com.teasaas.franchise.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加盟商启停合作入参（F-FRA-01）
 */
@Data
public class FranchiseeStatusReq {

    /** 1合作中/0已终止 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
