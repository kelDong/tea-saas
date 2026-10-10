package com.teasaas.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.framework.ws.WsStoreBindingResolver;
import com.teasaas.system.entity.SysUserStore;
import com.teasaas.system.mapper.SysUserStoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 门店绑定解析实现（订单 WS 订阅过滤，SPI 见 WsStoreBindingResolver）
 *
 * <p>查询 sys_user_store 走租户插件——调用方（WS 握手拦截器）
 * 已显式注入登录会话的租户上下文。</p>
 */
@Component
@RequiredArgsConstructor
public class WsStoreBindingResolverImpl implements WsStoreBindingResolver {

    private final SysUserStoreMapper sysUserStoreMapper;

    @Override
    public List<Long> listBoundStoreIds(Long userId) {
        return sysUserStoreMapper.selectList(
                        new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, userId))
                .stream().map(SysUserStore::getStoreId).distinct().toList();
    }
}
