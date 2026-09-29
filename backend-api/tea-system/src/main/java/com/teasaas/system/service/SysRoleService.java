package com.teasaas.system.service;

import com.teasaas.system.entity.SysRole;

import java.util.List;

/**
 * 角色管理（F-SYS-02；一期：列表 + 菜单授权）
 */
public interface SysRoleService {

    /** 全量列表（预置 R1-R8 + 自建角色）：R2 */
    List<SysRole> list();

    /** 角色已绑菜单 ID（授权回显）：R2 */
    List<Long> listMenuIds(Long roleId);

    /** 更新角色的菜单授权（全量重绑）：R2 */
    void grantMenus(Long roleId, List<Long> menuIds);
}
