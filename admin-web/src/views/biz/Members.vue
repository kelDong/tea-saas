<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { memberApi, memberConfigApi } from '@/api/member'
import type { MemMember } from '@/types/member'
import { formatDateTime } from '@/utils/format'

// ---------- 会员列表 ----------
const loading = ref(false)
const rows = ref<MemMember[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

async function load() {
  loading.value = true
  try {
    const result = await memberApi.page({ ...query, keyword: query.keyword || undefined })
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

// ---------- 积分/储值规则 ----------
const configLoading = ref(false)
const configSaving = ref(false)
const config = reactive({
  pointsPerYuan: 1,
  pointValue: 1,
  rechargeBonusPermille: 0,
})

async function loadConfig() {
  configLoading.value = true
  try {
    const result = await memberConfigApi.points()
    Object.assign(config, result)
  } finally {
    configLoading.value = false
  }
}

async function saveConfig() {
  configSaving.value = true
  try {
    await memberConfigApi.savePoints({ ...config })
    ElMessage.success('积分/储值规则已保存（下单积分、积分抵现、充值赠送即时生效）')
  } finally {
    configSaving.value = false
  }
}

onMounted(() => {
  load()
  loadConfig()
})
</script>

<template>
  <div class="members-page">
    <el-tabs>
      <el-tab-pane label="会员列表">
        <el-card shadow="never">
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="储值余额/可用积分/积分与储值流水在 C 端「我的」接口，后台一期提供列表与规则配置；等级规则（mem_member_level）接口二期开放"
            class="hint"
          />
          <div class="toolbar">
            <el-input
              v-model="query.keyword"
              placeholder="昵称/手机号"
              clearable
              class="toolbar-input"
              @keyup.enter="search"
              @clear="search"
            />
            <el-button type="primary" @click="search">查询</el-button>
          </div>

          <el-table v-loading="loading" :data="rows" stripe>
            <el-table-column prop="id" label="会员ID" width="190" show-overflow-tooltip />
            <el-table-column label="昵称" min-width="140">
              <template #default="{ row }">{{ row.nickname || '—' }}</template>
            </el-table-column>
            <el-table-column label="手机号" width="130">
              <template #default="{ row }">{{ row.phone || '—' }}</template>
            </el-table-column>
            <el-table-column prop="totalPoints" label="累计积分" width="100" align="right" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" min-width="160">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
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
      </el-tab-pane>

      <el-tab-pane label="积分/储值规则" lazy>
        <el-card v-loading="configLoading" shadow="never">
          <el-form label-width="150px" class="config-form">
            <el-form-item label="消费积分比例">
              <el-input-number v-model="config.pointsPerYuan" :min="0" :max="1000" />
              <span class="tip">消费 1 元得 {{ config.pointsPerYuan }} 积分</span>
            </el-form-item>
            <el-form-item label="积分抵现价值">
              <el-input-number v-model="config.pointValue" :min="1" :max="1000" />
              <span class="tip">1 积分抵 {{ config.pointValue }} 分（{{ (config.pointValue / 100).toFixed(2) }} 元）</span>
            </el-form-item>
            <el-form-item label="充值赠送比例">
              <el-input-number v-model="config.rechargeBonusPermille" :min="0" :max="1000" :step="10" />
              <span class="tip">千分比 {{ config.rechargeBonusPermille }}‰（100‰=充 1000 送 100）</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="configSaving" @click="saveConfig">保存规则</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.hint {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-input {
  width: 220px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.config-form {
  max-width: 560px;
}

.tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
