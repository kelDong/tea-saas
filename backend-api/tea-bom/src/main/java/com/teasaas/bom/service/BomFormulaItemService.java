package com.teasaas.bom.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.bom.entity.BomFormulaItem;

/**
 * 配方明细 服务（生成器骨架：业务规则由人工补齐）
 */
public interface BomFormulaItemService {

    PageResult<BomFormulaItem> page(int page, int size);

    BomFormulaItem getById(Long id);

    Long create(BomFormulaItem entity);

    void update(Long id, BomFormulaItem entity);

    void delete(Long id);
}
