package com.teasaas.supply.service;

import com.teasaas.supply.entity.SupSupplierMaterial;

import java.util.List;

/**
 * 供应商可供应原料服务（F-SUP-01 内部复用，无独立契约端点）
 */
public interface SupSupplierMaterialService {

    /**
     * 查询供应商关联的可供应原料列表
     */
    List<SupSupplierMaterial> listBySupplierId(Long supplierId);
}
