package com.teasaas.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.system.entity.SysRole;
import com.teasaas.system.entity.SysRoleMenu;
import com.teasaas.system.mapper.SysRoleMapper;
import com.teasaas.system.mapper.SysRoleMenuMapper;
import com.teasaas.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色管理实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<SysRole> list() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantMenus(Long roleId, List<Long> menuIds) {
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds != null) {
            Long tenantId = SecurityUtils.getTenantIdFromSession();
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                rm.setTenantId(tenantId);
                sysRoleMenuMapper.insert(rm);
            }
        }
        log.info("role {} menus granted, count={}", roleId, menuIds == null ? 0 : menuIds.size());
    }

    @SuppressWarnings("unused")
    private void validateRoleExists(Long roleId) {
        if (sysRoleMapper.selectById(roleId) == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色不存在");
        }
    }
}
