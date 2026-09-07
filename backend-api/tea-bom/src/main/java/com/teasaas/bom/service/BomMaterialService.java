package com.teasaas.bom.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.bom.entity.BomMaterial;

/**
 * 原料档案 服务（生成器骨架：业务规则由人工补齐）
 */
public interface BomMaterialService {

    PageResult<BomMaterial> page(int page, int size, String keyword);

    BomMaterial getById(Long id);

    Long create(BomMaterial entity);

    void update(Long id, BomMaterial entity);

    void delete(Long id);
}
