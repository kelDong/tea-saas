package com.teasaas.supply.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.supply.dto.TierPriceBatchDTO;
import com.teasaas.supply.dto.TierPriceItemDTO;
import com.teasaas.supply.entity.SupTierPrice;
import com.teasaas.supply.mapper.SupTierPriceMapper;
import com.teasaas.supply.service.SupTierPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 分级价格服务实现（F-SUP-02，接口需求 4.2）
 * 维度 = 原料 × 加盟商等级（A/B/C），uk_tier_price 兜底唯一
 */
@Service
@RequiredArgsConstructor
public class SupTierPriceServiceImpl implements SupTierPriceService {

    private static final Set<String> VALID_LEVELS = Set.of("A", "B", "C");

    private final SupTierPriceMapper supTierPriceMapper;

    @Override
    public PageResult<SupTierPrice> page(int page, int size, Long materialId, String level) {
        LambdaQueryWrapper<SupTierPrice> wrapper = new LambdaQueryWrapper<SupTierPrice>()
                .eq(materialId != null, SupTierPrice::getMaterialId, materialId)
                .eq(level != null && !level.isBlank(), SupTierPrice::getLevelCode, level)
                .orderByAsc(SupTierPrice::getMaterialId)
                .orderByAsc(SupTierPrice::getLevelCode);
        Page<SupTierPrice> result = supTierPriceMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSet(TierPriceBatchDTO dto) {
        // levelCode 合法性二次校验（除 DTO 正则外的兜底）
        for (TierPriceItemDTO item : dto.getItems()) {
            if (!VALID_LEVELS.contains(item.getLevelCode())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "levelCode必须为A/B/C");
            }
        }
        Long tenantId = SecurityUtils.getTenantIdFromSession();
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        // 按 (tenant, materialId, levelCode) 逐条 upsert；uk_tier_price 唯一约束兜底并发
        for (TierPriceItemDTO item : dto.getItems()) {
            SupTierPrice exist = supTierPriceMapper.selectOne(new LambdaQueryWrapper<SupTierPrice>()
                    .eq(SupTierPrice::getMaterialId, item.getMaterialId())
                    .eq(SupTierPrice::getLevelCode, item.getLevelCode()));
            if (exist != null) {
                exist.setPrice(item.getPrice());
                exist.setUpdateBy(userId);
                exist.setUpdateTime(now);
                supTierPriceMapper.updateById(exist);
            } else {
                SupTierPrice entity = new SupTierPrice();
                entity.setTenantId(tenantId);
                entity.setMaterialId(item.getMaterialId());
                entity.setLevelCode(item.getLevelCode());
                entity.setPrice(item.getPrice());
                entity.setUpdateBy(userId);
                entity.setUpdateTime(now);
                supTierPriceMapper.insert(entity);
            }
        }
    }

    @Override
    public List<SupTierPrice> listByMaterialsAndLevel(List<Long> materialIds, String levelCode) {
        return supTierPriceMapper.selectList(new LambdaQueryWrapper<SupTierPrice>()
                .in(SupTierPrice::getMaterialId, materialIds)
                .eq(SupTierPrice::getLevelCode, levelCode));
    }
}
