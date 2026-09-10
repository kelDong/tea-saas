package com.teasaas.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.product.entity.PrdProductSpecOption;
import com.teasaas.product.mapper.PrdProductSpecOptionMapper;
import com.teasaas.product.service.PrdProductSpecOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 规格选项 服务实现（生成器骨架：业务规则由人工补齐）
 */
@Service
@RequiredArgsConstructor
public class PrdProductSpecOptionServiceImpl implements PrdProductSpecOptionService {

    private final PrdProductSpecOptionMapper prdProductSpecOptionMapper;

    @Override
    public PageResult<PrdProductSpecOption> page(int page, int size, String keyword) {
        LambdaQueryWrapper<PrdProductSpecOption> wrapper = new LambdaQueryWrapper<PrdProductSpecOption>()
                .like(keyword != null && !keyword.isBlank(), PrdProductSpecOption::getOptionName, keyword)
                .orderByDesc(PrdProductSpecOption::getId);
        Page<PrdProductSpecOption> result = prdProductSpecOptionMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PrdProductSpecOption getById(Long id) {
        PrdProductSpecOption entity = prdProductSpecOptionMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "规格选项不存在");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PrdProductSpecOption entity) {
        entity.setTenantId(SecurityUtils.getTenantIdFromSession());
        prdProductSpecOptionMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PrdProductSpecOption entity) {
        entity.setId(id);
        prdProductSpecOptionMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 物理删除（表无 del_flag）；租户插件自动拼 tenant_id 防越权
        prdProductSpecOptionMapper.deleteById(id);
    }
}
