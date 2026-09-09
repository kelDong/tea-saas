package com.teasaas.supply.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.supply.dto.TierPriceBatchDTO;
import com.teasaas.supply.entity.SupTierPrice;

import java.util.List;

/**
 * 分级价格服务（F-SUP-02）
 */
public interface SupTierPriceService {

    /**
     * 价格表分页查询（materialId/level 均可空 = 查全量）
     */
    PageResult<SupTierPrice> page(int page, int size, Long materialId, String level);

    /**
     * 批量设置价格：按 (tenant, materialId, levelCode) upsert，levelCode 必须 A/B/C
     */
    void batchSet(TierPriceBatchDTO dto);

    /**
     * 按原料 ID 集合 + 等级查询当前租户价格（采购计价用）
     */
    List<SupTierPrice> listByMaterialsAndLevel(List<Long> materialIds, String levelCode);
}
