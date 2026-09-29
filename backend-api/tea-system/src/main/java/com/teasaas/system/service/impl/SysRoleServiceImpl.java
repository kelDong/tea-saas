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
 * 角色管理实现（接口需求 2.3：R2）
 * sys_role/sys_role_menu 为平台共享表（租户插件 IGNORE_TABLES，见 MybatisPlusConfig）：
 * 角色 R1-R8 平台预置、授权跨品牌生效，tenant_id 统一写 0 对齐种子（02_data.sql）；
 * 二期若开放租户自定义角色，需重新评估隔离方案。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private static final String ROLE_R2 = "R2";
    /** 平台共享表语义：与种子一致写 0（角色授权跨品牌生效） */
    private static final Long PLATFORM_TENANT_ID = 0L;

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<SysRole> list() {
        requireR2();
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getId));
    }

    @Override
    public List<Long> listMenuIds(Long roleId) {
        requireR2();
        validateRoleExists(roleId);
        return sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).distinct().toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantMenus(Long roleId, List<Long> menuIds) {
        requireR2();
        validateRoleExists(roleId);
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                rm.setTenantId(PLATFORM_TENANT_ID);
                sysRoleMenuMapper.insert(rm);
            }
        }
        log.info("role {} menus granted, count={}", roleId, menuIds == null ? 0 : menuIds.size());
    }

    private void requireR2() {
        if (!SecurityUtils.getRolesFromSession().contains(ROLE_R2)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部管理员可操作角色授权");
        }
    }

    private void validateRoleExists(Long roleId) {
        if (sysRoleMapper.selectById(roleId) == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色不存在");
        }
    }
}
