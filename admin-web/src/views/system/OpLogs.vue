<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { opLogApi } from '@/api/system'
import type { SysOperationLog } from '@/types/system'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const rows = ref<SysOperationLog[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, module: '' })

async function load() {
  loading.value = true
  try {
    const result = await opLogApi.page({ ...query, module: query.module || undefined })
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function prettyParams(params: string | null): string {
  if (!params) return '—'
  try {
    return JSON.stringify(JSON.parse(params), null, 2)
  } catch {
    return params
  }
}

onMounted(load)
</script>

<template>
  <div class="oplogs-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.module"
          placeholder="模块（如 system/order/product）"
          clearable
          class="toolbar-input"
          @keyup.enter="search"
          @clear="search"
        />
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <pre class="params">{{ prettyParams(row.params) }}</pre>
          </template>
        </el-table-column>
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="操作" min-width="120" />
        <el-table-column prop="method" label="方法" min-width="240" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130">
          <template #default="{ row }">{{ row.ip || '—' }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">{{ row.costMs != null ? `${row.costMs}ms` : '—' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        class="pager"
        @current-change="load"
        @size-change="search"
      />
    </el-card>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-input {
  width: 260px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.params {
  margin: 0;
  padding: 8px 16px;
  max-height: 240px;
  overflow: auto;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 12px;
}
</style>
