package com.teasaas.system.service;

import com.teasaas.system.dto.LoginReq;
import com.teasaas.system.vo.LoginVO;
import com.teasaas.system.vo.MenuTreeVO;

/**
 * 认证服务（F-SYS-03 登录认证）
 */
public interface AuthService {

    /** 账号密码登录（验证码一期可选） */
    LoginVO login(LoginReq req);

    /** 退出登录 */
    void logout();

    /** 当前用户菜单树 + 按钮权限码（前端动态路由/v-permission 输入） */
    MenuTreeVO.MenusPayload menus();
}
