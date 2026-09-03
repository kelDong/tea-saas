package com.teasaas.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.common.constant.RedisConstants;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.system.dto.LoginReq;
import com.teasaas.system.entity.SysMenu;
import com.teasaas.system.entity.SysRole;
import com.teasaas.system.entity.SysRoleMenu;
import com.teasaas.system.entity.SysUser;
import com.teasaas.system.entity.SysUserRole;
import com.teasaas.system.mapper.SysMenuMapper;
import com.teasaas.system.mapper.SysRoleMapper;
import com.teasaas.system.mapper.SysRoleMenuMapper;
import com.teasaas.system.mapper.SysUserMapper;
import com.teasaas.system.mapper.SysUserRoleMapper;
import com.teasaas.system.service.AuthService;
import com.teasaas.system.vo.LoginVO;
import com.teasaas.system.vo.MenuTreeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 认证服务实现
 * RBAC 聚合逻辑：user -> roles -> role_menu -> menus（树） + perms（按钮码）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO login(LoginReq req) {
        // 1. 验证码（一期可选：传了 key 才校验）
        if (req.getCaptchaKey() != null && !req.getCaptchaKey().isBlank()) {
            String cacheKey = RedisConstants.CAPTCHA + req.getCaptchaKey();
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached == null || !cached.equalsIgnoreCase(req.getCaptchaCode())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "验证码错误或已过期");
            }
            stringRedisTemplate.delete(cacheKey); // 一次性
        }

        // 2. 用户校验（登录时租户未知：selectByUsernameForLogin 显式绕过租户插件，见 Mapper 声明）
        SysUser user = sysUserMapper.selectByUsernameForLogin(req.getUsername());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已停用");
        }

        // 3. Sa-Token 登录 + 会话写入租户/角色（多租户主线）
        StpUtil.login(user.getId());
        List<SysRole> roles = loadRoles(user.getId());
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).toList();
        List<String> perms = loadPerms(roles);

        StpUtil.getSession().set(SecurityUtils.SESSION_KEY_TENANT, user.getTenantId());
        StpUtil.getSession().set(SecurityUtils.SESSION_KEY_ROLES, roleCodes);
        StpUtil.getSession().set(SecurityUtils.SESSION_KEY_PERMS, perms);

        log.info("user {} (tenant {}) logged in, roles={}", user.getUsername(), user.getTenantId(), roleCodes);

        return LoginVO.builder()
                .token(StpUtil.getTokenValue())
                .tokenName(StpUtil.getTokenName())
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .nickname(user.getNickname())
                .userType(user.getUserType())
                .roles(roleCodes)
                .build();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public MenuTreeVO.MenusPayload menus() {
        long userId = StpUtil.getLoginIdAsLong();
        List<SysRole> roles = loadRoles(userId);
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();

        MenusAssembler assembler = new MenusAssembler();
        if (roleIds.isEmpty()) {
            return assembler.assemble(List.of());
        }
        // 角色 -> 菜单 ID（distinct）
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().toList();
        return assembler.assemble(loadMenusByIds(menuIds));
    }

    // ---------------- 内部 ----------------

    private List<SysRole> loadRoles(long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return List.of();
        }
        return sysRoleMapper.selectBatchIds(userRoles.stream().map(SysUserRole::getRoleId).toList());
    }

    private List<String> loadPerms(List<SysRole> roles) {
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        if (roleMenus.isEmpty()) {
            return List.of();
        }
        List<SysMenu> menus = loadMenusByIds(
                roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().toList());
        return menus.stream()
                .map(SysMenu::getPerm)
                .filter(Objects::nonNull)
                .filter(p -> !p.isBlank())
                .distinct()
                .toList();
    }

    private List<SysMenu> loadMenusByIds(List<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getVisible, 1));
    }

    /**
     * 菜单聚合器：目录/菜单构建树；按钮归权限码
     */
    private static class MenusAssembler {

        MenuTreeVO.MenusPayload assemble(List<SysMenu> menus) {
            MenuTreeVO.MenusPayload result = new MenuTreeVO.MenusPayload();
            List<MenuTreeVO> treeNodes = new ArrayList<>();
            List<String> permissions = new ArrayList<>();
            for (SysMenu m : menus) {
                if (m.getMenuType() == 3) {
                    if (m.getPerm() != null && !m.getPerm().isBlank()) {
                        permissions.add(m.getPerm());
                    }
                    continue;
                }
                MenuTreeVO node = new MenuTreeVO();
                node.setId(m.getId());
                node.setParentId(m.getParentId());
                node.setMenuName(m.getMenuName());
                node.setMenuType(m.getMenuType());
                node.setPath(m.getPath());
                node.setComponent(m.getComponent());
                node.setPerm(m.getPerm());
                node.setSort(m.getSort());
                treeNodes.add(node);
            }
            result.setMenuTree(buildTree(treeNodes));
            result.setPermissions(permissions.stream().distinct().sorted().toList());
            return result;
        }

        private List<MenuTreeVO> buildTree(List<MenuTreeVO> nodes) {
            Map<Long, MenuTreeVO> idMap = new LinkedHashMap<>();
            nodes.forEach(n -> idMap.put(n.getId(), n));
            List<MenuTreeVO> roots = new ArrayList<>();
            for (MenuTreeVO n : nodes) {
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
}
