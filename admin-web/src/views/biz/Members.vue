<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { memberApi, memberConfigApi } from '@/api/member'
import type { MemberAssets, MemMember, PointsLog, RechargeRecord } from '@/types/member'
import { POINTS_BIZ_TYPE_MAP } from '@/types/member'
import { fen2yuan, formatDateTime } from '@/utils/format'

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

// ---------- 资产/流水（二期A，drawer 复刻 store/Inventory 流水模式） ----------
const assetsVisible = ref(false)
const assetsLoading = ref(false)
const assets = ref<MemberAssets | null>(null)
const assetsTarget = ref<MemMember | null>(null)

const pointsLoading = ref(false)
const pointsLogs = ref<PointsLog[]>([])
const pointsTotal = ref(0)
const pointsQuery = reactive({ page: 1, size: 10 })

const walletLoading = ref(false)
const walletLogs = ref<RechargeRecord[]>([])
const walletTotal = ref(0)
const walletQuery = reactive({ page: 1, size: 10 })

async function openAssets(row: MemMember) {
  assetsTarget.value = row
  assetsVisible.value = true
  assetsLoading.value = true
  pointsQuery.page = 1
  walletQuery.page = 1
  try {
    const [assetResult, pointsResult, walletResult] = await Promise.all([
      memberApi.assets(row.id),
      memberApi.logs(row.id, { type: 'points', ...pointsQuery }),
      memberApi.logs(row.id, { type: 'wallet', ...walletQuery }),
    ])
    assets.value = assetResult
    pointsLogs.value = pointsResult.list as PointsLog[]
    pointsTotal.value = pointsResult.total
    walletLogs.value = walletResult.list as RechargeRecord[]
    walletTotal.value = walletResult.total
  } finally {
    assetsLoading.value = false
  }
}

async function loadPointsLogs() {
  if (!assetsTarget.value) return
  pointsLoading.value = true
  try {
    const result = await memberApi.logs(assetsTarget.value.id, { type: 'points', ...pointsQuery })
    pointsLogs.value = result.list as PointsLog[]
    pointsTotal.value = result.total
  } finally {
    pointsLoading.value = false
  }
}

async function loadWalletLogs() {
  if (!assetsTarget.value) return
  walletLoading.value = true
  try {
    const result = await memberApi.logs(assetsTarget.value.id, { type: 'wallet', ...walletQuery })
    walletLogs.value = result.list as RechargeRecord[]
    walletTotal.value = result.total
  } finally {
    walletLoading.value = false
  }
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
          <div class="page-toolbar">
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
            <el-table-column label="会员ID" width="190" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="mono">{{ row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column label="昵称" min-width="140">
              <template #default="{ row }">{{ row.nickname || '—' }}</template>
            </el-table-column>
            <el-table-column label="手机号" width="130">
              <template #default="{ row }">{{ row.phone || '—' }}</template>
            </el-table-column>
            <el-table-column label="累计积分" width="100" align="right">
              <template #default="{ row }">
                <span class="mono">{{ row.totalPoints }}</span>
              </template>
            </el-table-column>
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
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openAssets(row)">资产/流水</el-button>
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

    <div class="page-note">对应 PRD：F-MEM-01/F-MEM-02/F-MEM-03</div>

    <!-- 资产/流水 drawer（二期A） -->
    <el-drawer
      v-model="assetsVisible"
      :title="`资产/流水：${assetsTarget?.nickname || assetsTarget?.id || ''}`"
      size="720px"
    >
      <el-descriptions v-loading="assetsLoading" :column="3" border size="small" class="assets-card">
        <el-descriptions-item label="储值余额">{{ fen2yuan(assets?.balance) }}</el-descriptions-item>
        <el-descriptions-item label="累计充值">{{ fen2yuan(assets?.totalRecharge) }}</el-descriptions-item>
        <el-descriptions-item label="累计消费">{{ fen2yuan(assets?.totalConsume) }}</el-descriptions-item>
        <el-descriptions-item label="可用积分">
          <span class="mono">{{ assets?.availablePoints ?? '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="累计积分">
          <span class="mono">{{ assets?.totalPoints ?? '—' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-tabs class="logs-tabs">
        <el-tab-pane :label="`积分明细（${pointsTotal}）`">
          <el-table v-loading="pointsLoading" :data="pointsLogs" stripe size="small">
            <el-table-column label="变动" width="80" align="right">
              <template #default="{ row }">
                <span class="mono">{{ row.changePoints }}</span>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="100">
              <template #default="{ row }">{{ POINTS_BIZ_TYPE_MAP[row.bizType] ?? row.bizType }}</template>
            </el-table-column>
            <el-table-column label="业务单号" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="mono">{{ row.bizNo || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="变动前" width="80" align="right">
              <template #default="{ row }">
                <span class="mono">{{ row.beforePoints }}</span>
              </template>
            </el-table-column>
            <el-table-column label="变动后" width="80" align="right">
              <template #default="{ row }">
                <span class="mono">{{ row.afterPoints }}</span>
              </template>
            </el-table-column>
            <el-table-column label="时间" min-width="150">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="pointsQuery.page"
            v-model:page-size="pointsQuery.size"
            :total="pointsTotal"
            layout="total, prev, pager, next"
            class="pager"
            @current-change="loadPointsLogs"
          />
        </el-tab-pane>

        <el-tab-pane :label="`充值记录（${walletTotal}）`" lazy>
          <el-table v-loading="walletLoading" :data="walletLogs" stripe size="small">
            <el-table-column label="充值金额" width="100" align="right">
              <template #default="{ row }">{{ fen2yuan(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="赠送" width="100" align="right">
              <template #default="{ row }">{{ fen2yuan(row.bonus) }}</template>
            </el-table-column>
            <el-table-column label="充值后余额" width="110" align="right">
              <template #default="{ row }">{{ fen2yuan(row.balanceAfter) }}</template>
            </el-table-column>
            <el-table-column label="业务单号" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="mono">{{ row.bizNo || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="时间" min-width="150">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="walletQuery.page"
            v-model:page-size="walletQuery.size"
            :total="walletTotal"
            layout="total, prev, pager, next"
            class="pager"
            @current-change="loadWalletLogs"
          />
        </el-tab-pane>
      </el-tabs>
    </el-drawer>
  </div>
</template>

<style scoped>
.config-form {
  max-width: 560px;
}

.assets-card {
  margin-bottom: 16px;
}
</style>
