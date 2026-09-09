package com.teasaas.supply.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.supply.dto.PurchaseOrderCreateDTO;
import com.teasaas.supply.entity.SupPurchaseOrder;
import com.teasaas.supply.vo.PurchaseOrderDetailVO;

/**
 * 采购单服务（F-SUP-03/04，接口需求 4.3）
 * 状态机：1待确认 → 2已确认 → 3已发货 → 4已入库；5已取消
 */
public interface SupPurchaseOrderService {

    /**
     * 采购单分页列表（R4 查全租户 / R6 名下门店；status/storeId 可选筛选）
     */
    PageResult<SupPurchaseOrder> page(int page, int size, Integer status, Long storeId);

    /**
     * 采购单详情（主表 + 明细；R6/R7 访问非名下门店抛 FORBIDDEN）
     */
    PurchaseOrderDetailVO detail(Long id);

    /**
     * 发起采购单（仅 R6；门店须为名下加盟门店，按加盟商等级自动计价快照）
     */
    Long create(PurchaseOrderCreateDTO dto);

    /**
     * 确认计价（R4，1→2）
     */
    void confirm(Long id);

    /**
     * 发货（R4，2→3）
     */
    void ship(Long id);

    /**
     * 确认入库（R6/R7 名下门店，3→4；库存联动 F-SUP-04 批次 2）
     */
    void receive(Long id);

    /**
     * 取消（R4：1/2/3 可取消；R6：仅 1）
     */
    void cancel(Long id);
}
