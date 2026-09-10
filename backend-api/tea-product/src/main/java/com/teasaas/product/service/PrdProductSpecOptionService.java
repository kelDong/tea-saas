package com.teasaas.product.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.product.entity.PrdProductSpecOption;

/**
 * 规格选项 服务（生成器骨架：业务规则由人工补齐）
 */
public interface PrdProductSpecOptionService {

    PageResult<PrdProductSpecOption> page(int page, int size, String keyword);

    PrdProductSpecOption getById(Long id);

    Long create(PrdProductSpecOption entity);

    void update(Long id, PrdProductSpecOption entity);

    void delete(Long id);
}
