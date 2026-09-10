package com.teasaas.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.inventory.entity.InvStockLog;
import com.teasaas.inventory.mapper.InvStockLogMapper;
import com.teasaas.inventory.service.InvStockLogService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存变动流水 服务实现（生成器骨架：业务规则由人工补齐）
 */
@Service
@RequiredArgsConstructor
public class InvStockLogServiceImpl implements InvStockLogService {

    private final InvStockLogMapper invStockLogMapper;

    @Override
    public PageResult<InvStockLog> page(int page, int size) {
        LambdaQueryWrapper<InvStockLog> wrapper = new LambdaQueryWrapper<InvStockLog>()
                .orderByDesc(InvStockLog::getId);
        Page<InvStockLog> result = invStockLogMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public InvStockLog getById(Long id) {
        InvStockLog entity = invStockLogMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "库存变动流水不存在");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(InvStockLog entity) {
        entity.setTenantId(SecurityUtils.getTenantIdFromSession());
        entity.setCreateTime(LocalDateTime.now());
        invStockLogMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, InvStockLog entity) {
        entity.setId(id);
        invStockLogMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 物理删除（表无 del_flag）；租户插件自动拼 tenant_id 防越权
        invStockLogMapper.deleteById(id);
    }
}
