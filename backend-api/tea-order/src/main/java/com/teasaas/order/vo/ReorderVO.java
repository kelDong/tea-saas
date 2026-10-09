package com.teasaas.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 再来一单出参（接口需求 8.2 F-APP-03）：按历史明细重建购物车，
 * 已下架/缺货项拆入 skipped 由前端提示跳过。
 *
 * <p>注意：ord_order_item 仅存 specDesc 快照（specOptionIds 不持久化），
 * 可加购项由前端跳规格弹层重选规格后入车。</p>
 */
@Data
@Schema(description = "再来一单")
public class ReorderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原订单门店ID")
    private Long storeId;

    @Schema(description = "可加购项")
    private List<ReorderItem> items;

    @Schema(description = "跳过项（已下架/缺货）")
    private List<ReorderItem> skipped;

    @Data
    @Schema(description = "再来一单明细项")
    public static class ReorderItem implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "商品ID")
        private Long productId;

        @Schema(description = "门店上架ID")
        private Long listingId;

        @Schema(description = "商品名快照")
        private String productName;

        @Schema(description = "规格快照（入车前需重选规格）")
        private String specDesc;

        @Schema(description = "数量")
        private Integer qty;

        @Schema(description = "当前本店售价(分)，仅可加购项返回")
        private Integer salePrice;

        @Schema(description = "跳过原因：已下架/缺货，仅跳过项返回")
        private String skipReason;
    }
}
