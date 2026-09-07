package com.teasaas.bom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.bom.entity.BomFormulaItem;
import com.teasaas.bom.mapper.BomFormulaItemMapper;
import com.teasaas.bom.service.BomFormulaItemService;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 配方明细 服务实现（生成器骨架：业务规则由人工补齐）
 */
@Service
@RequiredArgsConstructor
public class BomFormulaItemServiceImpl implements BomFormulaItemService {

    private final BomFormulaItemMapper bomFormulaItemMapper;

    @Override
    public PageResult<BomFormulaItem> page(int page, int size) {
        LambdaQueryWrapper<BomFormulaItem> wrapper = new LambdaQueryWrapper<BomFormulaItem>()
                .orderByDesc(BomFormulaItem::getId);
        Page<BomFormulaItem> result = bomFormulaItemMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public BomFormulaItem getById(Long id) {
        BomFormulaItem entity = bomFormulaItemMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "配方明细不存在");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(BomFormulaItem entity) {
        entity.setTenantId(SecurityUtils.getTenantIdFromSession());
        entity.setCreateTime(LocalDateTime.now());
        bomFormulaItemMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BomFormulaItem entity) {
        entity.setId(id);
        bomFormulaItemMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 物理删除（表无 del_flag）；租户插件自动拼 tenant_id 防越权
        bomFormulaItemMapper.deleteById(id);
    }
}
