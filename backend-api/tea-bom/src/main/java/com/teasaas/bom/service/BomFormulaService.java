package com.teasaas.bom.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.bom.entity.BomFormula;

/**
 * 配方 服务（生成器骨架：业务规则由人工补齐）
 */
public interface BomFormulaService {

    PageResult<BomFormula> page(int page, int size, String keyword);

    BomFormula getById(Long id);

    Long create(BomFormula entity);

    void update(Long id, BomFormula entity);

    void delete(Long id);
}
