package com.teasaas.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量设置分级价格入参（F-SUP-02）
 */
@Data
@Schema(description = "批量设置分级价格入参")
public class TierPriceBatchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "价格项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "价格项列表不能为空")
    @Valid
    private List<TierPriceItemDTO> items;
}
