package com.teasaas.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 供应商新增/编辑入参（F-SUP-01）
 */
@Data
@Schema(description = "供应商新增/编辑入参")
public class SupplierSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "供应商编码（租户内唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "供应商编码不能为空")
    @Size(max = 32, message = "供应商编码最长32字符")
    private String supplierCode;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 64, message = "供应商名称最长64字符")
    private String supplierName;

    @Schema(description = "联系人")
    @Size(max = 32, message = "联系人最长32字符")
    private String contactName;

    @Schema(description = "联系电话")
    @Size(max = 20, message = "联系电话最长20字符")
    private String contactPhone;

    @Schema(description = "账期")
    @Size(max = 64, message = "账期最长64字符")
    private String paymentTerms;
}
