package com.teasaas.supply.service;

import com.teasaas.supply.entity.SupPurchaseOrderItem;

import java.util.List;

/**
 * 采购单明细服务（F-SUP-03 内部复用，无独立契约端点）
 */
public interface SupPurchaseOrderItemService {

    /**
     * 按采购单 ID 查询明细列表（含计价快照）
     */
    List<SupPurchaseOrderItem> listByOrderId(Long orderId);
}
