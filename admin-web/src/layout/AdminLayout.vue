<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'
import SideMenuItem from './SideMenuItem.vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

/** 侧边栏菜单：只渲染目录/菜单，按钮（menuType=3）不进导航 */
const navMenus = computed(() =>
  [...auth.menus].filter((menu) => menu.menuType !== 3).sort((a, b) => a.sort - b.sort),
)

const activeMenu = computed(() => route.path)

const breadcrumbs = computed(() =>
  route.matched
    .filter((item) => item.meta?.title)
    .map((item) => ({ title: item.meta.title as string })),
)

/** 头像占位字：昵称首字（无昵称时用"客"） */
const avatarText = computed(() => auth.nickname?.trim()?.charAt(0) || '客')

/** 过渡兜底：被遮挡/节流的窗口里 requestAnimationFrame 可能永不触发，
 *  Vue Transition 的 class 帧推进依赖 rAF，会令 out-in 的 leave 永不结束（表现为点菜单主区不换页）。
 *  用定时器 done 兜底（setTimeout 不受 rAF 节流影响），可见窗口下 CSS 与定时器先到者生效 */
function onLeave(_el: Element, done: () => void) {
  setTimeout(done, 150)
}

function onEnter(_el: Element, done: () => void) {
  setTimeout(done, 200)
}

async function handleLogout() {
  await auth.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="230px" class="admin-aside">
      <div class="admin-brand">
        <div class="brand-logo">🍵</div>
        <div class="brand-text">
          <div class="brand-name">茶饮 SaaS</div>
          <div class="brand-sub">连锁管理平台</div>
        </div>
      </div>
      <el-menu class="admin-menu" :default-active="activeMenu" router>
        <SideMenuItem v-for="menu in navMenus" :key="menu.id" :menu="menu" />
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header" height="58px">
        <el-breadcrumb separator="/" class="admin-crumb">
          <el-breadcrumb-item v-for="(crumb, index) in breadcrumbs" :key="index">
            {{ crumb.title }}
          </el-breadcrumb-item>
        </el-breadcrumb>
        <el-dropdown>
          <span class="admin-user">
            <span class="user-avatar">{{ avatarText }}</span>
            <span class="user-name">{{ auth.nickname || '未命名用户' }}</span>
            <el-icon class="user-arrow"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="admin-main">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in" @leave="onLeave" @enter="onEnter">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  height: 100%;
}

/* ---- 侧栏:深茶棕底,品牌块对齐顶栏高度 ---- */
.admin-aside {
  display: flex;
  flex-direction: column;
  background-color: var(--ts-side-bg);
  border-right: 1px solid var(--ts-side-border);
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 58px;
  padding: 0 16px;
  border-bottom: 1px solid var(--ts-side-border);
  flex-shrink: 0;
}

.brand-logo {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--ts-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.brand-name {
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.2;
}

.brand-sub {
  font-size: 10px;
  color: var(--ts-side-text-dim);
}

/* ---- 菜单:分组标签(一级目录) + 导航项(琥珀激活轨) ---- */
.admin-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
  padding-bottom: 20px;
  /* el-menu 主题变量(内联颜色 props 已删除,CSS 才能生效) */
  --el-menu-bg-color: transparent;
  --el-menu-text-color: var(--ts-side-text);
  --el-menu-active-color: var(--ts-primary-bright);
  --el-menu-hover-bg-color: transparent;
  --el-menu-item-height: 44px;
  --el-menu-sub-item-height: 44px;
  --el-menu-item-font-size: 13.5px;
}

.admin-menu::-webkit-scrollbar {
  width: 4px;
}
.admin-menu::-webkit-scrollbar-thumb {
  background: #4a3d2e;
  border-radius: 4px;
}

/* 一级目录做成分组标签样式(小号/字距/暗金；常规字重——层级感由字号+颜色承担，加粗会与小号冲突) */
.admin-menu :deep(.el-sub-menu__title) {
  height: 36px;
  padding-top: 10px;
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--ts-side-text-dim);
}
.admin-menu :deep(.el-sub-menu__title:hover) {
  background: var(--ts-side-hover);
}
.admin-menu :deep(.el-sub-menu__title .el-sub-menu__icon-arrow) {
  font-size: 11px;
  color: var(--ts-side-text-dim);
}

/* 导航项:透明左轨,激活时琥珀点亮(inset 不占布局) */
.admin-menu :deep(.el-menu-item) {
  border-left: 3px solid transparent;
  color: var(--ts-side-text);
  transition:
    background-color 0.12s,
    color 0.12s;
}
.admin-menu :deep(.el-menu-item:hover) {
  background: var(--ts-side-hover);
}
.admin-menu :deep(.el-menu-item.is-active) {
  background: var(--ts-side-active-bg);
  border-left-color: var(--ts-primary);
  color: var(--ts-primary-bright);
  font-weight: 600;
}

/* ---- 顶栏:纸白 + 暖边线,末级面包屑即页标题 ---- */
.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: var(--ts-paper);
  border-bottom: 1px solid var(--ts-border);
}

.admin-crumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  font-size: 16px;
  font-weight: 700;
  color: var(--ts-ink);
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--ts-ink);
  outline: none;
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--ts-primary-soft);
  color: var(--ts-primary-deep);
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-name {
  font-size: 13px;
}

.user-arrow {
  font-size: 12px;
  color: var(--ts-ink-2);
}

/* ---- 主区:奶油画布 + 原型边距 ---- */
.admin-main {
  background-color: var(--ts-canvas);
  padding: 20px 22px 40px;
  overflow-y: auto;
}
</style>
