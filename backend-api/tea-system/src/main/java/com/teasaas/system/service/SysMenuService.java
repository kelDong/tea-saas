package com.teasaas.system.service;

import com.teasaas.system.vo.MenuTreeVO;

import java.util.List;

/**
 * 系统菜单（F-SYS-02；二期补缺：全量树供角色授权勾选）
 */
public interface SysMenuService {

    /** 全量菜单树（含按钮节点、含停用/隐藏——授权勾选视角）：R2 */
    List<MenuTreeVO> tree();
}
