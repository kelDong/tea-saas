package com.teasaas.supply.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.supply.entity.SupPurchaseOrderItem;
import com.teasaas.supply.mapper.SupPurchaseOrderItemMapper;
import com.teasaas.supply.service.SupPurchaseOrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 采购单明细服务实现（F-SUP-03 内部复用：明细随主表在事务内写入）
 */
@Service
@RequiredArgsConstructor
public class SupPurchaseOrderItemServiceImpl implements SupPurchaseOrderItemService {

    private final SupPurchaseOrderItemMapper supPurchaseOrderItemMapper;

    @Override
    public List<SupPurchaseOrderItem> listByOrderId(Long orderId) {
        return supPurchaseOrderItemMapper.selectList(new LambdaQueryWrapper<SupPurchaseOrderItem>()
                .eq(SupPurchaseOrderItem::getOrderId, orderId)
                .orderByAsc(SupPurchaseOrderItem::getId));
    }
}
