package com.teasaas.supply.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.supply.dto.SupplierMaterialBindDTO;
import com.teasaas.supply.dto.SupplierSaveDTO;
import com.teasaas.supply.entity.SupSupplier;
import com.teasaas.supply.entity.SupSupplierMaterial;

import java.util.List;

/**
 * 供应商服务（F-SUP-01）
 */
public interface SupSupplierService {

    /**
     * 分页查询（keyword 匹配编码/名称）
     */
    PageResult<SupSupplier> page(int page, int size, String keyword);

    /**
     * 新增供应商（编码租户内唯一，冲突抛 PARAM_ERROR）
     */
    Long create(SupplierSaveDTO dto);

    /**
     * 编辑供应商（编码租户内唯一，冲突抛 PARAM_ERROR；供应商不存在抛 PARAM_ERROR）
     */
    void update(Long id, SupplierSaveDTO dto);

    /**
     * 关联可供应原料（事务内全量重绑：先删后插，幂等）
     */
    void bindMaterials(Long id, SupplierMaterialBindDTO dto);

    /**
     * 启用/停用供应商（status 仅 0/1）
     */
    void changeStatus(Long id, Integer status);

    /**
     * 查询供应商当前关联的可供应原料列表
     */
    List<SupSupplierMaterial> listMaterials(Long supplierId);
}
