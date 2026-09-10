package com.teasaas.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.product.entity.PrdProductSpec;
import com.teasaas.product.mapper.PrdProductSpecMapper;
import com.teasaas.product.service.PrdProductSpecService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品规格组 服务实现（生成器骨架：业务规则由人工补齐）
 */
@Service
@RequiredArgsConstructor
public class PrdProductSpecServiceImpl implements PrdProductSpecService {

    private final PrdProductSpecMapper prdProductSpecMapper;

    @Override
    public PageResult<PrdProductSpec> page(int page, int size, String keyword) {
        LambdaQueryWrapper<PrdProductSpec> wrapper = new LambdaQueryWrapper<PrdProductSpec>()
                .like(keyword != null && !keyword.isBlank(), PrdProductSpec::getSpecName, keyword)
                .orderByDesc(PrdProductSpec::getId);
        Page<PrdProductSpec> result = prdProductSpecMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PrdProductSpec getById(Long id) {
        PrdProductSpec entity = prdProductSpecMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "商品规格组不存在");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PrdProductSpec entity) {
        entity.setTenantId(SecurityUtils.getTenantIdFromSession());
        prdProductSpecMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PrdProductSpec entity) {
        entity.setId(id);
        prdProductSpecMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 物理删除（表无 del_flag）；租户插件自动拼 tenant_id 防越权
        prdProductSpecMapper.deleteById(id);
    }
}
