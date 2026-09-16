import { ref } from 'vue'
import { defineStore } from 'pinia'

import { authApi } from '@/api/auth'
import type { MenuNode } from '@/types/api'
import { clearToken, getNickname, getToken, setToken } from '@/utils/token'

/** 认证/权限状态：token + 用户信息 + 菜单树 + 按钮权限码 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const nickname = ref(getNickname())
  const roles = ref<string[]>([])
  const menus = ref<MenuNode[]>([])
  const permissions = ref<string[]>([])
  const menusLoaded = ref(false)

  async function login(data: { username: string; password: string; captchaKey?: string; captchaCode?: string }) {
    const vo = await authApi.login(data)
    setToken(vo.token, vo.tokenName, vo.nickname)
    token.value = vo.token
    nickname.value = vo.nickname
    roles.value = vo.roles ?? []
  }

  async function loadMenus() {
    const payload = await authApi.menus()
    menus.value = payload.menuTree ?? []
    permissions.value = payload.permissions ?? []
    menusLoaded.value = true
  }

  /** 按钮权限判断（v-permission 指令与模板共用）；空权限码视为公开 */
  function hasPerm(perm?: string | null): boolean {
    if (!perm) return true
    return permissions.value.includes(perm)
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      // 后端登出失败也照常清本地态，避免卡死在已失效会话
      reset()
    }
  }

  function reset() {
    clearToken()
    token.value = null
    nickname.value = ''
    roles.value = []
    menus.value = []
    permissions.value = []
    menusLoaded.value = false
  }

  return { token, nickname, roles, menus, permissions, menusLoaded, login, loadMenus, hasPerm, logout, reset }
})
