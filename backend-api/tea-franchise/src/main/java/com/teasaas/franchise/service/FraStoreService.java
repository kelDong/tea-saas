package com.teasaas.franchise.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.franchise.dto.StoreBusinessStatusReq;
import com.teasaas.franchise.dto.StoreCreateReq;
import com.teasaas.franchise.dto.StoreUpdateReq;
import com.teasaas.franchise.entity.FraStore;
import com.teasaas.franchise.vo.StoreNearbyVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 门店服务（F-FRA-02，接口需求 §5 + §13）
 */
public interface FraStoreService {

    /** 分页列表（R2/R5 看全部；R6/R7 只看 sys_user_store 绑定的名下门店） */
    PageResult<FraStore> page(int page, int size, String keyword);

    /** 详情（R6/R7 操作绑定外门店抛 FORBIDDEN） */
    FraStore getById(Long id);

    Long create(StoreCreateReq req);

    void update(Long id, StoreUpdateReq req);

    /** 营业状态切换：R2 任意本租户门店；R7 仅本店；R6 无此权限 */
    void switchBusinessStatus(Long id, StoreBusinessStatusReq req);

    /** C 端公开附近门店：X-Tenant-Id 手动注入租户，仅 status=1 且 business_status=1，最近 20 家 */
    List<StoreNearbyVO> nearby(BigDecimal lng, BigDecimal lat);
}
