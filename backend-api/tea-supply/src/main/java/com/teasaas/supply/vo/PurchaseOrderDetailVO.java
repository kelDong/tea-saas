package com.teasaas.supply.vo;

import com.teasaas.supply.entity.SupPurchaseOrder;
import com.teasaas.supply.entity.SupPurchaseOrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 采购单详情 VO（主表 + 明细列表，F-SUP-03）
 */
@Data
@Schema(description = "采购单详情")
public class PurchaseOrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "采购单主表信息")
    private SupPurchaseOrder order;

    @Schema(description = "采购明细列表（含计价快照）")
    private List<SupPurchaseOrderItem> items;

    public static PurchaseOrderDetailVO of(SupPurchaseOrder order, List<SupPurchaseOrderItem> items) {
        PurchaseOrderDetailVO vo = new PurchaseOrderDetailVO();
        vo.setOrder(order);
        vo.setItems(items);
        return vo;
    }
}
