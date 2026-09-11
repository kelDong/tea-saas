package com.teasaas.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 下单明细项（C 端/POS 共用，接口需求 8.1/8.4）
 */
@Data
@Schema(description = "下单明细项")
public class OrderItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店上架ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "listingId不能为空")
    private Long listingId;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "qty不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer qty;

    @Schema(description = "规格选项ID列表（可空）")
    private List<Long> specOptionIds;
}
