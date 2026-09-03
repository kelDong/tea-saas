package com.teasaas.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.PasswordUtil;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.system.dto.SysUserCreateReq;
import com.teasaas.system.dto.SysUserUpdateReq;
import com.teasaas.system.entity.SysRole;
import com.teasaas.system.entity.SysUser;
import com.teasaas.system.entity.SysUserStore;
import com.teasaas.system.entity.SysUserRole;
import com.teasaas.system.mapper.SysRoleMapper;
import com.teasaas.system.mapper.SysUserMapper;
import com.teasaas.system.mapper.SysUserStoreMapper;
import com.teasaas.system.mapper.SysUserRoleMapper;
import com.teasaas.system.service.SysUserService;
import com.teasaas.system.vo.SysUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理实现
 * 数据权限（PRD 5.4）：当前用户角色 data_scope 决定可见范围——
 * 总部（R2-R5）本租户全量；加盟商（R6）名下门店员工；店长/店员（R7/R8）本店。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysUserStoreMapper sysUserStoreMapper;
    private final SysRoleMapper sysRoleMapper;

    @Override
    public PageResult<SysUserVO> page(int page, int size, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(keyword != null && !keyword.isBlank(), SysUser::getUsername, keyword)
                .or(keyword != null && !keyword.isBlank(),
                        w -> w.like(SysUser::getNickname, keyword))
                .orderByDesc(SysUser::getCreateTime);

        // 数据权限：非总部角色 -> 只看绑定了相同门店的用户
        List<Long> scopeStoreIds = currentScopeStoreIds();
        if (scopeStoreIds != null) {
            List<SysUserStore> bindings = sysUserStoreMapper.selectList(
                    new LambdaQueryWrapper<SysUserStore>().in(SysUserStore::getStoreId, scopeStoreIds));
            Set<Long> userIds = bindings.stream().map(SysUserStore::getUserId).collect(Collectors.toSet());
            if (userIds.isEmpty()) {
                return PageResult.empty(page, size);
            }
            wrapper.in(SysUser::getId, userIds);
        }

        Page<SysUser> result = sysUserMapper.selectPage(Page.of(page, size), wrapper);
        List<SysUserVO> vos = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysUserCreateReq req) {
        Long tenantId = SecurityUtils.getTenantIdFromSession();
        // 用户名租户内唯一
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(PasswordUtil.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setPhone(req.getPhone());
        user.setUserType(resolveUserType(req.getRoleIds()));
        user.setStatus(1);
        user.setTenantId(tenantId);
        sysUserMapper.insert(user);

        rebindRoles(user.getId(), req.getRoleIds());
        rebindStores(user.getId(), req.getStoreIds());
        log.info("user {} created by {}", user.getUsername(), StpUtil.getLoginIdAsLong());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SysUserUpdateReq req) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户不存在");
        }
        user.setNickname(req.getNickname());
        user.setPhone(req.getPhone());
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }
        sysUserMapper.updateById(user);
        if (req.getRoleIds() != null && !req.getRoleIds().isEmpty()) {
            rebindRoles(id, req.getRoleIds());
        }
        if (req.getStoreIds() != null) {
            rebindStores(id, req.getStoreIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(PasswordUtil.encode(newPassword));
        sysUserMapper.updateById(update);
        // 强制下线（密码已变）
        StpUtil.logout(id);
    }

    // ---------------- 内部 ----------------

    /** null=总部视角（本租户全量）；非 null=限定门店范围 */
    private List<Long> currentScopeStoreIds() {
        List<String> roleCodes = SecurityUtils.getRolesFromSession();
        boolean hq = roleCodes.stream().anyMatch(c -> c.equals("R1") || c.equals("R2")
                || c.equals("R3") || c.equals("R4") || c.equals("R5"));
        if (hq) {
            return null;
        }
        long userId = StpUtil.getLoginIdAsLong();
        return sysUserStoreMapper.selectList(
                        new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, userId))
                .stream().map(SysUserStore::getStoreId).distinct().toList();
    }

    private Integer resolveUserType(List<Long> roleIds) {
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        return roles.stream()
                .map(SysRole::getRoleCode)
                .filter(code -> code.startsWith("R"))
                .map(code -> switch (code) {
                    case "R1" -> 0;
                    case "R6" -> 2;
                    case "R7", "R8" -> 3;
                    default -> 1;
                })
                .min(Integer::compareTo)
                .orElse(1);
    }

    private void rebindRoles(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            ur.setTenantId(SecurityUtils.getTenantIdFromSession());
            sysUserRoleMapper.insert(ur);
        }
    }

    private void rebindStores(Long userId, List<Long> storeIds) {
        sysUserStoreMapper.delete(new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, userId));
        if (storeIds == null) {
            return;
        }
        for (Long storeId : storeIds) {
            SysUserStore us = new SysUserStore();
            us.setUserId(userId);
            us.setStoreId(storeId);
            us.setTenantId(SecurityUtils.getTenantIdFromSession());
            sysUserStoreMapper.insert(us);
        }
    }

    private SysUserVO toVO(SysUser user) {
        SysUserVO vo = new SysUserVO();
        vo.setId(user.getId());
        vo.setTenantId(user.getTenantId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setUserType(user.getUserType());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        if (!userRoles.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectBatchIds(
                    userRoles.stream().map(SysUserRole::getRoleId).toList());
            vo.setRoleIds(roles.stream().map(SysRole::getId).toList());
            vo.setRoleNames(roles.stream().map(SysRole::getRoleName).toList());
        }
        vo.setStoreIds(sysUserStoreMapper.selectList(
                        new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, user.getId()))
                .stream().map(SysUserStore::getStoreId).distinct().toList());
        return vo;
    }
}
