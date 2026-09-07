<script setup lang="ts">
import { computed } from 'vue'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const roleText = computed(() => auth.roles.join(' / ') || '未分配角色')
</script>

<template>
  <div class="dashboard">
    <el-card>
      <template #header>平台工作台</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="当前用户">{{ auth.nickname || '—' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ roleText }}</el-descriptions-item>
        <el-descriptions-item label="可见菜单数">{{ auth.menus.length }}</el-descriptions-item>
        <el-descriptions-item label="按钮权限码数">{{ auth.permissions.length }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card>
      <template #header>按钮权限指令演示（v-permission）</template>
      <el-space wrap>
        <el-button v-permission="'bom:add'" type="primary">新增配方（bom:add）</el-button>
        <el-button v-permission="'product:publish'" type="success">发布商品（product:publish）</el-button>
        <el-button v-permission="'purchase:submit'">提交采购单（purchase:submit）</el-button>
        <el-tag type="info">无权限的按钮会被直接移除，可对照上方权限码数验证</el-tag>
      </el-space>
    </el-card>

    <el-card>
      <template #header>下一步（M3）</template>
      <el-empty description="业务页面按 docs/茶饮SaaS-接口需求.md 逐模块补齐：原料档案 / 配方 BOM / 门店库存 …" />
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
