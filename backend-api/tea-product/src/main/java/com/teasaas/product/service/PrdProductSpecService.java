package com.teasaas.product.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.product.entity.PrdProductSpec;

/**
 * 商品规格组 服务（生成器骨架：业务规则由人工补齐）
 */
public interface PrdProductSpecService {

    PageResult<PrdProductSpec> page(int page, int size, String keyword);

    PrdProductSpec getById(Long id);

    Long create(PrdProductSpec entity);

    void update(Long id, PrdProductSpec entity);

    void delete(Long id);
}
