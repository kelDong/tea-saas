package com.teasaas.framework.ws;

import java.util.List;

/**
 * 门店绑定解析 SPI（WS 订阅过滤用）
 *
 * <p>tea-framework 不依赖 tea-system（依赖方向：system → framework），
 * 名下门店（sys_user_store）的解析由 tea-system 提供实现注册为 bean，
 * framework 侧经 ObjectProvider 可空注入。调用方需已注入正确租户上下文。</p>
 */
public interface WsStoreBindingResolver {

    /** 用户名下绑定的门店 ID 列表（去重） */
    List<Long> listBoundStoreIds(Long userId);
}
