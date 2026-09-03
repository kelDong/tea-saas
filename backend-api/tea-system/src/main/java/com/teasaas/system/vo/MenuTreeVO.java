package com.teasaas.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点（前端动态路由输入）+ 按钮权限码集合
 */
@Data
public class MenuTreeVO {

    private Long id;
    private Long parentId;
    private String menuName;
    /** 1目录/2菜单/3按钮 */
    private Integer menuType;
    private String path;
    private String component;
    private String perm;
    private Integer sort;

    private List<MenuTreeVO> children = new ArrayList<>();

    /**
     * 登录响应的完整载荷：菜单树（目录/菜单）+ 按钮权限码集合
     */
    @Data
    public static class MenusPayload {
        private List<MenuTreeVO> menuTree;
        private List<String> permissions;
    }
}
