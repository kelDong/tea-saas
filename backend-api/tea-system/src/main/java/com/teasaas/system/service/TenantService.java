package com.teasaas.system.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.system.dto.TenantCreateReq;
import com.teasaas.system.dto.TenantUpdateReq;
import com.teasaas.system.vo.TenantVO;

/**
 * 品牌租户管理（F-SYS-01，接口需求 2.2；仅 R1 平台管理员）
 */
public interface TenantService {

    /** 品牌列表（分页，keyword 匹配编码/名称/联系人电话） */
    PageResult<TenantVO> page(int page, int size, String keyword);

    /** 创建品牌 */
    Long create(TenantCreateReq req);

    /** 编辑品牌（tenantCode 不可改） */
    void update(Long id, TenantUpdateReq req);

    /** 启停品牌（1启用/0停用） */
    void updateStatus(Long id, Integer status);
}
