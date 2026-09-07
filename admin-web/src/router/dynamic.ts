import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'

import type { MenuNode } from '@/types/api'

/**
 * 菜单树 -> 动态路由（/auth/menus 返回的 menuTree 是唯一路由来源，权限由后端裁剪）
 * 约定（与 sql/init/02_data.sql 种子一致）：
 * - 目录 menuType=1：path 为 / 开头绝对路径，component 为空，自身不渲染页面
 * - 菜单 menuType=2：path 为相对路径，component 如 platform/Dashboard -> src/views/platform/Dashboard.vue
 * - 按钮 menuType=3：只有 perm，不生成路由
 */
type ViewLoader = () => Promise<{ default: Component }>

const viewModules = import.meta.glob('../views/**/*.vue') as Record<string, ViewLoader>

const PLACEHOLDER_SUFFIX = '/error/Placeholder.vue'

/** component 字段解析到 views 下的实际文件；未实现的菜单回落到占位页（M3 按接口文档逐个补齐） */
function resolveComponent(component: string | null | undefined): ViewLoader {
  const suffix = component ? `/${component}.vue` : PLACEHOLDER_SUFFIX
  const key =
    Object.keys(viewModules).find((k) => k.endsWith(suffix)) ??
    Object.keys(viewModules).find((k) => k.endsWith(PLACEHOLDER_SUFFIX))
  if (!key) {
    // 占位页随骨架一起提交，此分支仅防御误删
    throw new Error('动态路由解析失败：src/views/error/Placeholder.vue 缺失')
  }
  return viewModules[key]
}

function joinPath(base: string, child: string): string {
  if (child.startsWith('/')) {
    return child
  }
  return `${base.replace(/\/+$/, '')}/${child}`
}

function toRouteRecord(menu: MenuNode, root: boolean): RouteRecordRaw | null {
  if (menu.menuType === 3) {
    return null
  }
  if (menu.menuType === 1) {
    // 目录：无自身组件，子路由渲染在最近祖先 router-view（即主布局内）
    const children = (menu.children ?? [])
      .map((child) => toRouteRecord(child, false))
      .filter((route): route is RouteRecordRaw => route !== null)
    if (children.length === 0) {
      return null
    }
    const dirPath = menu.path?.startsWith('/') ? menu.path : `/${menu.path ?? menu.id}`
    return {
      path: dirPath,
      name: `menu-${menu.id}`,
      children,
      meta: { title: menu.menuName },
    }
  }
  // 菜单叶子
  const rawPath = menu.path ?? String(menu.id)
  const pagePath = root && !rawPath.startsWith('/') ? `/${rawPath}` : rawPath
  return {
    path: pagePath,
    name: `menu-${menu.id}`,
    component: resolveComponent(menu.component),
    meta: { title: menu.menuName, perm: menu.perm ?? undefined },
  }
}

/** 顶层数组直接挂到 layout 路由下（router.addRoute('layout', route)） */
export function buildRoutesFromMenus(menus: MenuNode[]): RouteRecordRaw[] {
  return menus
    .filter((menu) => menu.menuType !== 3)
    .sort((a, b) => a.sort - b.sort)
    .map((menu) => toRouteRecord(menu, true))
    .filter((route): route is RouteRecordRaw => route !== null)
}

/** 第一个可跳转菜单的完整路径（登录后 '/' 的默认落点） */
export function firstMenuFullPath(menus: MenuNode[]): string | null {
  const directories = [...menus]
    .filter((menu) => menu.menuType === 1)
    .sort((a, b) => a.sort - b.sort)
  for (const dir of directories) {
    if (!dir.path) {
      continue
    }
    const leaf = (dir.children ?? [])
      .filter((child) => child.menuType === 2)
      .sort((a, b) => a.sort - b.sort)
      .find((child) => child.path)
    if (leaf?.path) {
      return joinPath(dir.path, leaf.path)
    }
  }
  return null
}
