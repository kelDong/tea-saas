package com.teasaas.franchise.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.franchise.dto.FranchiseeAccountReq;
import com.teasaas.franchise.dto.FranchiseeCreateReq;
import com.teasaas.franchise.dto.FranchiseeLevelReq;
import com.teasaas.franchise.dto.FranchiseeStatusReq;
import com.teasaas.franchise.dto.FranchiseeUpdateReq;
import com.teasaas.franchise.entity.FraFranchisee;

/**
 * 加盟商服务（F-FRA-01，接口需求 §5）
 */
public interface FraFranchiseeService {

    /** 分页列表（keyword 模糊匹配名称/编码） */
    PageResult<FraFranchisee> page(int page, int size, String keyword);

    FraFranchisee getById(Long id);

    Long create(FranchiseeCreateReq req);

    void update(Long id, FranchiseeUpdateReq req);

    /** 等级评定：直接更新 level_code（采购分级价联动 F-SUP-02 不在本模块） */
    void updateLevel(Long id, FranchiseeLevelReq req);

    /** 开通账号：复用 SysUserService.create，绑定 R6 角色 + 名下门店；返回 userId */
    Long openAccount(Long id, FranchiseeAccountReq req);

    /** 启停合作（1合作中/0已终止）；不做门店级联，由 Owner 后续决策 */
    void switchStatus(Long id, FranchiseeStatusReq req);
}
