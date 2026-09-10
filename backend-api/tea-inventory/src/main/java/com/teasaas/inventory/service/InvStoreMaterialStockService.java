package com.teasaas.inventory.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.inventory.dto.StockAdjustDTO;
import com.teasaas.inventory.entity.InvStoreMaterialStock;
import com.teasaas.inventory.entity.InvStockLog;
import com.teasaas.inventory.vo.InventorySummaryVO;
import com.teasaas.inventory.vo.StockWarningVO;

import java.util.List;

/**
 * 库存台账服务（F-INV-01 台账/盘点 / F-INV-03 预警，接口需求 7.1/7.2）
 */
public interface InvStoreMaterialStockService {

    /** 门店库存列表（R6/R7 名下门店，R2 读全部；storeId/materialId 可选筛选） */
    PageResult<InvStoreMaterialStock> page(int page, int size, Long storeId, Long materialId);

    /** 库存变动流水（R6/R7 名下门店） */
    PageResult<InvStockLog> logs(int page, int size, Long storeId, Long materialId);

    /** 盘点修正（仅 R6/R7，名下门店；以实盘为准，差异写流水 bizType=5） */
    void adjust(StockAdjustDTO dto);

    /** 低库存列表（R6/R7 名下门店，R2 全部） */
    List<StockWarningVO> warnings(Long storeId);

    /** 总部库存总览（R2/R5 读，按原料聚合） */
    List<InventorySummaryVO> summary();
}
