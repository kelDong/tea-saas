<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { packageApi } from '@/api/platform'
import type { SysPackageVO } from '@/types/platform'
import { fen2yuan } from '@/utils/format'

const loading = ref(false)
const rows = ref<SysPackageVO[]>([])

async function load() {
  loading.value = true
  try {
    rows.value = await packageApi.list()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="packages-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>套餐列表（含停用；套餐 CRUD 二期开放）</span>
          <el-button link type="primary" @click="load">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="套餐编码" min-width="120">
          <template #default="{ row }">
            <span class="mono">{{ row.packageCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="packageName" label="套餐名称" min-width="150" />
        <el-table-column label="单价(店/月)" width="120" align="right">
          <template #default="{ row }">
            <span class="mono">{{ fen2yuan(row.pricePerStore) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="门店上限" width="100" align="right">
          <template #default="{ row }">
            <span class="mono">{{ row.storeLimit }}</span>
          </template>
        </el-table-column>
        <el-table-column label="功能开关" min-width="220">
          <template #default="{ row }">
            <code v-if="row.featureJson" class="feature">{{ row.featureJson }}</code>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="page-note">对应 PRD：F-SYS-01</div>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.feature {
  font-size: 12px;
  word-break: break-all;
  color: var(--el-text-color-secondary);
}
</style>
