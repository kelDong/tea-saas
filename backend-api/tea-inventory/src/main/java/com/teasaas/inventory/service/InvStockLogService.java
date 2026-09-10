package com.teasaas.inventory.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.inventory.entity.InvStockLog;

/**
 * 库存变动流水 服务（生成器骨架：业务规则由人工补齐）
 */
public interface InvStockLogService {

    PageResult<InvStockLog> page(int page, int size);

    InvStockLog getById(Long id);

    Long create(InvStockLog entity);

    void update(Long id, InvStockLog entity);

    void delete(Long id);
}
