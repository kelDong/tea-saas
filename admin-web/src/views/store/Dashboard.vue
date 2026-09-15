<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import { franchiseApi } from '@/api/franchise'
import { inventoryApi } from '@/api/inventory'
import { orderApi } from '@/api/order'
import { reportApi } from '@/api/report'
import type { StockWarningVO } from '@/types/inventory'
import type { OrdOrder } from '@/types/order'
import type { DailySettleVO } from '@/types/report'
import { fen2yuan } from '@/utils/format'

const stores = ref<Array<{ id: number; storeName: string }>>([])
const storeId = ref<number | undefined>(undefined)

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

// ---------- 今日经营 ----------
const settleLoading = ref(false)
const settle = ref<DailySettleVO | null>(null)

async function loadSettle() {
  if (!storeId.value) return
  settleLoading.value = true
  try {
    settle.value = await reportApi.storeDaily(storeId.value, today())
  } finally {
    settleLoading.value = false
  }
}

// ---------- 待处理订单（2待接单/3制作中/4待取餐） ----------
const pendingLoading = ref(false)
const pending = ref<OrdOrder[]>([])
const flowLoading = ref<number | null>(null)

async function loadPending() {
  if (!storeId.value) return
  pendingLoading.value = true
  try {
    const result = await orderApi.page({ page: 1, size: 20, storeId: storeId.value })
    pending.value = result.list.filter((row) => row.status === 2 || row.status === 3 || row.status === 4)
  } finally {
    pendingLoading.value = false
  }
}

async function flow(row: OrdOrder, action: 'accept' | 'make' | 'finish') {
  flowLoading.value = row.id
  try {
    if (action === 'accept') await orderApi.accept(row.id)
    else if (action === 'make') await orderApi.make(row.id)
    else await orderApi.finish(row.id)
    ElMessage.success('已流转')
    loadPending()
    loadSettle()
  } finally {
    flowLoading.value = null
  }
}

// ---------- 库存预警 ----------
const warnLoading = ref(false)
const warnings = ref<StockWarningVO[]>([])

async function loadWarnings() {
  if (!storeId.value) return
  warnLoading.value = true
  try {
    warnings.value = await inventoryApi.warnings(storeId.value)
  } finally {
    warnLoading.value = false
  }
}

function refresh() {
  loadSettle()
  loadPending()
  loadWarnings()
}

watch(storeId, refresh)

onMounted(async () => {
  stores.value = (await franchiseApi.storePage({ page: 1, size: 100 })).list
  if (stores.value.length) {
    storeId.value = stores.value[0].id
  }
})
</script>

<template>
  <div class="store-dashboard">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="storeId" placeholder="选择门店" filterable class="toolbar-select">
          <el-option v-for="store in stores" :key="store.id" :value="store.id" :label="store.storeName" />
        </el-select>
        <el-button type="primary" @click="refresh">刷新</el-button>
      </div>

      <el-empty v-if="!storeId" description="当前租户暂无门店" />
      <el-row v-else v-loading="settleLoading" :gutter="16">
        <el-col :span="4">
          <div class="stat">
            <div class="label">今日订单</div>
            <div class="value">{{ settle?.orderCount ?? 0 }}</div>
          </div>
        </el-col>
        <el-col :span="5">
          <div class="stat">
            <div class="label">今日营业额</div>
            <div class="value">{{ fen2yuan(settle?.salesAmount ?? 0) }}</div>
          </div>
        </el-col>
        <el-col :span="5">
          <div class="stat">
            <div class="label">客单价</div>
            <div class="value">{{ fen2yuan(settle?.avgOrderAmount ?? 0) }}</div>
          </div>
        </el-col>
        <el-col :span="5">
          <div class="stat">
            <div class="label">微信实收</div>
            <div class="value">{{ fen2yuan(settle?.wxAmount ?? 0) }}</div>
          </div>
        </el-col>
        <el-col :span="5">
          <div class="stat">
            <div class="label">储值实收</div>
            <div class="value">{{ fen2yuan(settle?.walletAmount ?? 0) }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row v-if="storeId" :gutter="16" class="bottom-row">
      <!-- 待处理订单 -->
      <el-col :span="14">
        <el-card shadow="never" class="full-height">
          <template #header>待处理订单（待接单 / 制作中 / 待取餐）</template>
          <el-table v-loading="pendingLoading" :data="pending" stripe size="small">
            <el-table-column prop="orderNo" label="单号" min-width="180" show-overflow-tooltip />
            <el-table-column prop="takeCode" label="取餐码" width="80" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 2 ? 'warning' : row.status === 3 ? 'primary' : 'success'" size="small">
                  {{ row.status === 2 ? '待接单' : row.status === 3 ? '制作中' : '待取餐' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="实付" width="100" align="right">
              <template #default="{ row }">{{ fen2yuan(row.payAmount) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 2"
                  v-permission="'order:accept'"
                  link
                  type="warning"
                  size="small"
                  :loading="flowLoading === row.id"
                  @click="flow(row, 'accept')"
                >接单</el-button>
                <el-button
                  v-if="row.status === 3"
                  v-permission="'order:make'"
                  link
                  type="primary"
                  size="small"
                  :loading="flowLoading === row.id"
                  @click="flow(row, 'make')"
                >制作完成</el-button>
                <el-button
                  v-if="row.status === 4"
                  v-permission="'order:finish'"
                  link
                  type="success"
                  size="small"
                  :loading="flowLoading === row.id"
                  @click="flow(row, 'finish')"
                >核销取餐</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!pendingLoading && !pending.length" description="暂无待处理订单" :image-size="60" />
        </el-card>
      </el-col>

      <!-- 库存预警 -->
      <el-col :span="10">
        <el-card shadow="never" class="full-height">
          <template #header>库存预警（{{ warnings.length }} 条）</template>
          <el-table v-loading="warnLoading" :data="warnings" stripe size="small" max-height="360">
            <el-table-column prop="materialName" label="原料" min-width="110" />
            <el-table-column prop="stockQty" label="库存" width="80" align="right" />
            <el-table-column prop="warnThreshold" label="阈值" width="80" align="right" />
            <el-table-column label="缺口" width="80" align="right">
              <template #default="{ row }">
                <span class="gap">{{ row.gap }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!warnLoading && !warnings.length" description="库存充足" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-select {
  width: 220px;
}

.stat .label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.stat .value {
  font-size: 22px;
  font-weight: 600;
  margin-top: 6px;
}

.bottom-row {
  margin-top: 16px;
}

.full-height {
  height: 100%;
}

.gap {
  color: var(--el-color-danger);
  font-weight: 600;
}
</style>
