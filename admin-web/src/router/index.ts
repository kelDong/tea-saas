import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { buildRoutesFromMenus, firstMenuFullPath } from './dynamic'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    name: 'layout',
    component: () => import('@/layout/AdminLayout.vue'),
    children: [],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '404' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.name === 'login') {
    return auth.token ? { path: '/' } : true
  }
  if (!auth.token) {
    return {
      path: '/login',
      query: to.fullPath !== '/' ? { redirect: to.fullPath } : undefined,
    }
  }

  // 首次进入受保护页面：拉菜单树 -> 注册动态路由 -> 重试当前导航
  let routesAdded = false
  if (!auth.menusLoaded) {
    try {
      await auth.loadMenus()
    } catch {
      auth.reset()
      return { path: '/login' }
    }
    for (const route of buildRoutesFromMenus(auth.menus)) {
      router.addRoute('layout', route)
    }
    routesAdded = true
  }

  if (to.path === '/') {
    const first = firstMenuFullPath(auth.menus)
    if (first) {
      return first
    }
  }
  // to 是按旧路由表解析的（可能落在 404），重定向到 fullPath 让新路由生效
  if (routesAdded) {
    return to.fullPath
  }
  return true
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : ''
  document.title = title ? `${title} · 茶饮 SaaS` : '茶饮 SaaS 管理后台'
})

export default router
