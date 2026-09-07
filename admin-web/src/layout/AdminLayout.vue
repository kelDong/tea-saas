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

async function handleLogout() {
  await auth.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="admin-logo">茶饮 SaaS</div>
      <el-menu
        class="admin-menu"
        :default-active="activeMenu"
        router
        background-color="#001529"
        text-color="rgba(255, 255, 255, 0.65)"
        active-text-color="#ffffff"
      >
        <SideMenuItem v-for="menu in navMenus" :key="menu.id" :menu="menu" />
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="(crumb, index) in breadcrumbs" :key="index">
            {{ crumb.title }}
          </el-breadcrumb-item>
        </el-breadcrumb>
        <el-dropdown>
          <span class="admin-user">
            {{ auth.nickname || '未命名用户' }}
            <el-icon><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  height: 100%;
}

.admin-aside {
  display: flex;
  flex-direction: column;
  background-color: #001529;
}

.admin-logo {
  height: 56px;
  line-height: 56px;
  color: #ffffff;
  font-size: 18px;
  font-weight: 600;
  text-align: center;
  letter-spacing: 2px;
}

.admin-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border-bottom: 1px solid #e8e8e8;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #303133;
  outline: none;
}

.admin-main {
  background-color: #f5f7fa;
  overflow-y: auto;
}
</style>
