package com.teasaas.system.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.system.dto.SysUserCreateReq;
import com.teasaas.system.dto.SysUserUpdateReq;
import com.teasaas.system.vo.SysUserVO;

/**
 * 用户管理（F-SYS-02 后半）
 */
public interface SysUserService {

    /** 分页列表（按当前用户数据权限过滤：本租户全量 / 名下门店 / 本店） */
    PageResult<SysUserVO> page(int page, int size, String keyword);

    Long create(SysUserCreateReq req);

    void update(Long id, SysUserUpdateReq req);

    void resetPassword(Long id, String newPassword);
}
