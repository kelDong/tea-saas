package com.teasaas.bom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * 原料档案 服务实现（生成器骨架：业务规则由人工补齐）
 */
@Service
@RequiredArgsConstructor
public class BomMaterialServiceImpl implements BomMaterialService {

    private final BomMaterialMapper bomMaterialMapper;

    @Override
    public PageResult<BomMaterial> page(int page, int size, String keyword) {
        LambdaQueryWrapper<BomMaterial> wrapper = new LambdaQueryWrapper<BomMaterial>()
                .like(keyword != null && !keyword.isBlank(), BomMaterial::getMaterialName, keyword)
                .orderByDesc(BomMaterial::getId);
        Page<BomMaterial> result = bomMaterialMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public BomMaterial getById(Long id) {
        BomMaterial entity = bomMaterialMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "原料档案不存在");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(BomMaterial entity) {
        entity.setTenantId(SecurityUtils.getTenantIdFromSession());
        bomMaterialMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BomMaterial entity) {
        entity.setId(id);
        bomMaterialMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 逻辑删除（@TableLogic）；租户插件自动拼 tenant_id 防越权
        bomMaterialMapper.deleteById(id);
    }
}
