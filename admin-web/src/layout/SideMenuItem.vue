<script setup lang="ts">
import { computed } from 'vue'

import type { MenuNode } from '@/types/api'
import SideMenuItem from './SideMenuItem.vue'

/** parentPath：祖先目录累积的绝对路径（顶层不传） */
const props = defineProps<{ menu: MenuNode; parentPath?: string }>()

/** 子级中参与导航的部分（按钮权限 menuType=3 不进菜单） */
const navChildren = computed(() =>
  [...(props.menu.children ?? [])]
    .filter((child) => child.menuType !== 3)
    .sort((a, b) => a.sort - b.sort),
)

const dirPath = computed(() => props.menu.path ?? `/${props.menu.id}`)

/** el-menu router 模式下 index 即跳转路径；与 dynamic.ts 的路由拼接规则保持一致 */
function joinPath(childPath: string): string {
  if (childPath.startsWith('/')) {
    return childPath
  }
  const base = props.parentPath?.replace(/\/+$/, '') ?? ''
  return `${base}/${childPath}`
}
</script>

<template>
  <!-- 目录：可展开子菜单 -->
  <el-sub-menu v-if="menu.menuType === 1" :index="joinPath(dirPath)">
    <template #title>{{ menu.menuName }}</template>
    <SideMenuItem
      v-for="child in navChildren"
      :key="child.id"
      :menu="child"
      :parent-path="joinPath(dirPath)"
    />
  </el-sub-menu>

  <!-- 菜单叶子：点击跳转 -->
  <el-menu-item v-else :index="joinPath(menu.path ?? String(menu.id))">
    {{ menu.menuName }}
  </el-menu-item>
</template>
