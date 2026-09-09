package com.teasaas.franchise.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 门店营业状态切换入参（F-FRA-02）
 */
@Data
public class StoreBusinessStatusReq {

    /** 1营业/0打烊 */
    @NotNull(message = "营业状态不能为空")
    private Integer businessStatus;
}
