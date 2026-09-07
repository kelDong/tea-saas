package com.teasaas.bom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.bom.entity.BomFormula;
import com.teasaas.bom.mapper.BomFormulaMapper;
import com.teasaas.bom.service.BomFormulaService;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 配方 服务实现（生成器骨架：业务规则由人工补齐）
 */
@Service
@RequiredArgsConstructor
public class BomFormulaServiceImpl implements BomFormulaService {

    private final BomFormulaMapper bomFormulaMapper;

    @Override
    public PageResult<BomFormula> page(int page, int size, String keyword) {
        LambdaQueryWrapper<BomFormula> wrapper = new LambdaQueryWrapper<BomFormula>()
                .like(keyword != null && !keyword.isBlank(), BomFormula::getFormulaName, keyword)
                .orderByDesc(BomFormula::getId);
        Page<BomFormula> result = bomFormulaMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public BomFormula getById(Long id) {
        BomFormula entity = bomFormulaMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "配方不存在");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(BomFormula entity) {
        entity.setTenantId(SecurityUtils.getTenantIdFromSession());
        bomFormulaMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BomFormula entity) {
        entity.setId(id);
        bomFormulaMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 逻辑删除（@TableLogic）；租户插件自动拼 tenant_id 防越权
        bomFormulaMapper.deleteById(id);
    }
}
