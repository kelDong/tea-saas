package com.teasaas.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.system.entity.SysMenu;
import com.teasaas.system.mapper.SysMenuMapper;
import com.teasaas.system.service.SysMenuService;
import com.teasaas.system.vo.MenuTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统菜单实现
 * 授权树口径与登录菜单树（AuthServiceImpl.MenusAssembler：按钮归权限码、不进树）不同：
 * 角色授权需精确勾到按钮级（PRD 5.3 按钮权限矩阵），故本类独立组装全量树，
 * 不复用/不改动 MenusAssembler（避免登录菜单回归风险）。
 */
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private static final String ROLE_R2 = "R2";

    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<MenuTreeVO> tree() {
        requireR2();
        List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort));
        return buildTree(menus);
    }

    private void requireR2() {
        if (!SecurityUtils.getRolesFromSession().contains(ROLE_R2)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部管理员可操作");
        }
    }

    /** 全量树组装：含按钮节点（menuType=3 作叶子）；不过滤 status/visible（授权视角） */
    private List<MenuTreeVO> buildTree(List<SysMenu> menus) {
        Map<Long, MenuTreeVO> idMap = new LinkedHashMap<>();
        for (SysMenu m : menus) {
            MenuTreeVO node = new MenuTreeVO();
            node.setId(m.getId());
            node.setParentId(m.getParentId());
            node.setMenuName(m.getMenuName());
            node.setMenuType(m.getMenuType());
            node.setPath(m.getPath());
            node.setComponent(m.getComponent());
            node.setPerm(m.getPerm());
            node.setSort(m.getSort());
            idMap.put(m.getId(), node);
        }
        List<MenuTreeVO> roots = new ArrayList<>();
        for (MenuTreeVO n : idMap.values()) {
            MenuTreeVO parent = idMap.get(n.getParentId());
            if (parent == null) {
                roots.add(n);
            } else {
                parent.getChildren().add(n);
            }
        }
        roots.sort(Comparator.comparing(MenuTreeVO::getSort, Comparator.nullsLast(Comparator.naturalOrder())));
        return roots;
    }
}
