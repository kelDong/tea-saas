package com.teasaas.bom.service;

import com.teasaas.bom.dto.MaterialSaveReq;
import com.teasaas.bom.entity.BomMaterial;
import com.teasaas.common.result.PageResult;

/**
 * 原料档案服务（F-BOM-01）
 */
public interface BomMaterialService {

    /**
     * 分页列表，keyword 模糊匹配名称/编码
     */
    PageResult<BomMaterial> page(int page, int size, String keyword);

    /**
     * 详情（不存在抛 MATERIAL_NOT_FOUND）
     */
    BomMaterial getById(Long id);

    /**
     * 新增（materialCode 租户内唯一，重复抛 PARAM_ERROR）
     */
    Long create(MaterialSaveReq req);

    /**
     * 编辑（不存在抛 MATERIAL_NOT_FOUND；编码查重排除自身）
     */
    void update(Long id, MaterialSaveReq req);

    /**
     * 停用/启用（status：0 停用 / 1 启用）
     */
    void updateStatus(Long id, Integer status);
}
