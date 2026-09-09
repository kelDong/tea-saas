package com.teasaas.franchise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.franchise.dto.FranchiseeAccountReq;
import com.teasaas.franchise.dto.FranchiseeCreateReq;
import com.teasaas.franchise.dto.FranchiseeLevelReq;
import com.teasaas.franchise.dto.FranchiseeStatusReq;
import com.teasaas.franchise.dto.FranchiseeUpdateReq;
import com.teasaas.franchise.entity.FraFranchisee;
import com.teasaas.franchise.entity.FraStore;
import com.teasaas.franchise.mapper.FraFranchiseeMapper;
import com.teasaas.franchise.mapper.FraStoreMapper;
import com.teasaas.franchise.service.FraFranchiseeService;
import com.teasaas.system.dto.SysUserCreateReq;
import com.teasaas.system.entity.SysRole;
import com.teasaas.system.mapper.SysRoleMapper;
import com.teasaas.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 加盟商服务实现（F-FRA-01）
 * 查询均走 MyBatis-Plus 租户插件自动拼 tenant_id（多租户红线）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraFranchiseeServiceImpl implements FraFranchiseeService {

    /** 开通账号未传密码时的初始默认密码（密码加密策略复用 SysUserService） */
    private static final String DEFAULT_INIT_PASSWORD = "tea@123456";

    /** 加盟商角色编码（禁止硬编码 role id，统一按 role_code 查询） */
    private static final String ROLE_CODE_FRANCHISEE = "R6";

    private final FraFranchiseeMapper fraFranchiseeMapper;
    private final FraStoreMapper fraStoreMapper;
    private final SysUserService sysUserService;
    private final SysRoleMapper sysRoleMapper;

    @Override
    public PageResult<FraFranchisee> page(int page, int size, String keyword) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        LambdaQueryWrapper<FraFranchisee> wrapper = new LambdaQueryWrapper<FraFranchisee>()
                .and(hasKeyword, w -> w.like(FraFranchisee::getFranchiseeName, keyword)
                        .or()
                        .like(FraFranchisee::getFranchiseeCode, keyword))
                .orderByDesc(FraFranchisee::getId);
        Page<FraFranchisee> result = fraFranchiseeMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public FraFranchisee getById(Long id) {
        FraFranchisee entity = fraFranchiseeMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.FRANCHISEE_NOT_FOUND);
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FranchiseeCreateReq req) {
        checkCodeDuplicate(req.getFranchiseeCode(), null);
        FraFranchisee entity = new FraFranchisee();
        entity.setFranchiseeCode(req.getFranchiseeCode());
        entity.setFranchiseeName(req.getFranchiseeName());
        entity.setContactName(req.getContactName());
        entity.setContactPhone(req.getContactPhone());
        entity.setLevelCode(req.getLevelCode() != null ? req.getLevelCode() : "C");
        entity.setJoinDate(req.getJoinDate());
        entity.setStatus(1);
        entity.setTenantId(SecurityUtils.getTenantIdFromSession());
        fraFranchiseeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FranchiseeUpdateReq req) {
        getById(id);
        checkCodeDuplicate(req.getFranchiseeCode(), id);
        FraFranchisee entity = new FraFranchisee();
        entity.setId(id);
        entity.setFranchiseeCode(req.getFranchiseeCode());
        entity.setFranchiseeName(req.getFranchiseeName());
        entity.setContactName(req.getContactName());
        entity.setContactPhone(req.getContactPhone());
        entity.setJoinDate(req.getJoinDate());
        fraFranchiseeMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLevel(Long id, FranchiseeLevelReq req) {
        getById(id);
        FraFranchisee entity = new FraFranchisee();
        entity.setId(id);
        entity.setLevelCode(req.getLevelCode());
        fraFranchiseeMapper.updateById(entity);
        // fra_franchisee 无 remark 列，评定备注仅在此留痕（是否加列待 Owner 决策）
        log.info("franchisee {} level -> {} (remark: {})", id, req.getLevelCode(), req.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long openAccount(Long id, FranchiseeAccountReq req) {
        FraFranchisee franchisee = getById(id);

        // R6 角色按 role_code 动态查询（禁止硬编码 role id）
        SysRole franchiseeRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, ROLE_CODE_FRANCHISEE).last("LIMIT 1"));
        if (franchiseeRole == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "预置角色 R6 缺失，请先初始化角色数据");
        }

        // 名下门店作为账号的门店绑定（数据权限"名下门店"依据）
        List<Long> storeIds = fraStoreMapper.selectList(
                        new LambdaQueryWrapper<FraStore>().eq(FraStore::getFranchiseeId, id))
                .stream().map(FraStore::getId).toList();

        // 复用 tea-system 的创建逻辑：密码加密/用户名查重/角色绑定/门店绑定
        // SysUserCreateReq 无 franchisee 关联字段 -> 重复开通以 username 唯一性兜底
        SysUserCreateReq createReq = new SysUserCreateReq();
        createReq.setUsername(req.getUsername());
        createReq.setPassword(req.getPassword() != null ? req.getPassword() : DEFAULT_INIT_PASSWORD);
        createReq.setNickname(req.getNickname() != null ? req.getNickname() : franchisee.getFranchiseeName());
        createReq.setPhone(franchisee.getContactPhone());
        createReq.setRoleIds(List.of(franchiseeRole.getId()));
        createReq.setStoreIds(storeIds);
        return sysUserService.create(createReq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void switchStatus(Long id, FranchiseeStatusReq req) {
        if (req.getStatus() != 0 && req.getStatus() != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "status 仅支持 0/1");
        }
        getById(id);
        FraFranchisee entity = new FraFranchisee();
        entity.setId(id);
        entity.setStatus(req.getStatus());
        fraFranchiseeMapper.updateById(entity);
        if (req.getStatus() == 0) {
            // 已拍板：不做门店级联停业，是否有级联策略待 Owner 决策
            log.info("franchisee {} cooperation terminated (cascade NOT applied by design)", id);
        }
    }

    // ---------------- 内部 ----------------

    /** franchiseeCode 租户内唯一（租户插件自动限定本租户）；excludeId 用于编辑时排除自身 */
    private void checkCodeDuplicate(String franchiseeCode, Long excludeId) {
        Long count = fraFranchiseeMapper.selectCount(new LambdaQueryWrapper<FraFranchisee>()
                .eq(FraFranchisee::getFranchiseeCode, franchiseeCode)
                .ne(excludeId != null, FraFranchisee::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "加盟商编码已存在");
        }
    }
}
