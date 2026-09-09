package com.teasaas.bom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.bom.dto.MaterialSaveReq;
import com.teasaas.bom.entity.BomMaterial;
import com.teasaas.bom.mapper.BomMaterialMapper;
import com.teasaas.bom.service.BomMaterialService;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 原料档案服务实现（F-BOM-01）
 */
@Service
@RequiredArgsConstructor
public class BomMaterialServiceImpl implements BomMaterialService {

    /** status：停用 */
    private static final int STATUS_DISABLED = 0;
    /** status：正常 */
    private static final int STATUS_ENABLED = 1;

    private final BomMaterialMapper bomMaterialMapper;

    @Override
    public PageResult<BomMaterial> page(int page, int size, String keyword) {
        // 租户插件自动拼 tenant_id；keyword 同时模糊匹配名称/编码
        LambdaQueryWrapper<BomMaterial> wrapper = new LambdaQueryWrapper<BomMaterial>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(BomMaterial::getMaterialName, keyword)
                        .or()
                        .like(BomMaterial::getMaterialCode, keyword))
                .orderByDesc(BomMaterial::getId);
        Page<BomMaterial> result = bomMaterialMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public BomMaterial getById(Long id) {
        BomMaterial entity = bomMaterialMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.MATERIAL_NOT_FOUND);
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MaterialSaveReq req) {
        checkCodeUnique(req.getMaterialCode(), null);
        BomMaterial entity = new BomMaterial();
        entity.setTenantId(SecurityUtils.getTenantIdFromSession());
        entity.setMaterialCode(req.getMaterialCode());
        entity.setMaterialName(req.getMaterialName());
        entity.setCategory(req.getCategory());
        entity.setUnit(req.getUnit());
        entity.setWarnThreshold(req.getWarnThreshold());
        entity.setCostPrice(req.getCostPrice());
        entity.setStatus(STATUS_ENABLED);
        bomMaterialMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MaterialSaveReq req) {
        getById(id);
        checkCodeUnique(req.getMaterialCode(), id);
        BomMaterial entity = new BomMaterial();
        entity.setId(id);
        entity.setMaterialCode(req.getMaterialCode());
        entity.setMaterialName(req.getMaterialName());
        entity.setCategory(req.getCategory());
        entity.setUnit(req.getUnit());
        entity.setWarnThreshold(req.getWarnThreshold());
        entity.setCostPrice(req.getCostPrice());
        bomMaterialMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_DISABLED && status != STATUS_ENABLED)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "status 仅允许 0(停用)/1(启用)");
        }
        getById(id);
        BomMaterial entity = new BomMaterial();
        entity.setId(id);
        entity.setStatus(status);
        bomMaterialMapper.updateById(entity);
    }

    /**
     * materialCode 租户内唯一校验（租户插件自动过滤 tenant_id）
     *
     * @param excludeId 编辑时排除自身，新增传 null
     */
    private void checkCodeUnique(String materialCode, Long excludeId) {
        LambdaQueryWrapper<BomMaterial> wrapper = new LambdaQueryWrapper<BomMaterial>()
                .eq(BomMaterial::getMaterialCode, materialCode)
                .ne(excludeId != null, BomMaterial::getId, excludeId);
        if (bomMaterialMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "原料编码已存在");
        }
    }
}
