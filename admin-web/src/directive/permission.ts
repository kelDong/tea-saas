import type { Directive, DirectiveBinding } from 'vue'

import { useAuthStore } from '@/stores/auth'

/**
 * v-permission 按钮权限指令（红线：权限码以 /auth/menus 下发的 permissions 为准）
 * 用法：v-permission="'bom:add'" 或 v-permission="['bom:add', 'bom:edit']"（任一命中即显示）
 * 无权限时直接移除元素（而非隐藏，避免绕过 disabled 交互）
 */
function check(binding: DirectiveBinding<string | string[] | undefined>): boolean {
  const value = binding.value
  if (!value) {
    return true
  }
  const perms = Array.isArray(value) ? value : [value]
  if (perms.length === 0) {
    return true
  }
  const auth = useAuthStore()
  return perms.some((perm) => auth.hasPerm(perm))
}

export const permission: Directive<HTMLElement, string | string[] | undefined> = {
  mounted(el, binding) {
    if (!check(binding)) {
      el.parentNode?.removeChild(el)
    }
  },
}
