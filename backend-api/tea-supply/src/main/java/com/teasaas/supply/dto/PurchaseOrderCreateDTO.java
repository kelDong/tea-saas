package com.teasaas.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发起采购单入参（F-SUP-03，仅 R6）
 */
@Data
@Schema(description = "发起采购单入参")
public class PurchaseOrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店ID（须为当前用户名下加盟门店）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "storeId不能为空")
    private Long storeId;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最长255字符")
    private String remark;

    @Schema(description = "采购明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "采购明细不能为空")
    @Valid
    private List<PurchaseOrderItemDTO> items;
}
