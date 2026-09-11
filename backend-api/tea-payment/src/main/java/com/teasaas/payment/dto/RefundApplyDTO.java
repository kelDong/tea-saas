package com.teasaas.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 申请退款入参（接口需求 9.2，P0-5：仅整单退）
 */
@Data
@Schema(description = "申请退款入参")
public class RefundApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "退款原因")
    @Size(max = 128, message = "退款原因最长128字")
    private String reason;
}
