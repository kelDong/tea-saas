<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { franchiseApi } from '@/api/franchise'
import { reportApi } from '@/api/report'
import type { DailySettleVO, StoreReportVO, TrendNode } from '@/types/report'
import { fen2yuan } from '@/utils/format'

// ---------- 门店下拉 ----------
const stores = ref<Array<{ id: number; storeName: string }>>([])

// ---------- 经营趋势（F-RPT-01） ----------
const GRANULARITY = [
  { value: 'day', label: '按日' },
  { value: 'week', label: '按周' },
  { value: 'month', label: '按月' },
]

const trendLoading = ref(false)
const trend = ref<TrendNode[]>([])
const trendQuery = reactive({
  storeId: undefined as number | undefined,
  granularity: 'day',
})

async function loadTrend() {
  trendLoading.value = true
  try {
    const result: StoreReportVO = await reportApi.store({
      storeId: trendQuery.storeId,
      granularity: trendQuery.granularity,
    })
    trend.value = result.items
  } finally {
    trendLoading.value = false
  }
}

// ---------- 门店日结（F-RPT-02） ----------
function yesterday(): string {
  return new Date(Date.now() - 86400000).toISOString().slice(0, 10)
}

const settleLoading = ref(false)
const settle = ref<DailySettleVO | null>(null)
const settleQuery = reactive({
  storeId: undefined as number | undefined,
  date: yesterday(),
})

async function loadSettle() {
  if (!settleQuery.storeId) {
    settle.value = null
    return
  }
  settleLoading.value = true
  try {
    settle.value = await reportApi.storeDaily(settleQuery.storeId, settleQuery.date)
  } finally {
    settleLoading.value = false
  }
}

onMounted(async () => {
  stores.value = (await franchiseApi.storePage({ page: 1, size: 100 })).list
  loadTrend()
})
</script>

<template>
  <div class="store-report-page">
    <!-- 经营趋势 -->
    <el-card shadow="never">
      <template #header>经营趋势（口径：支付时间，退款/取消订单不计）</template>
      <div class="page-toolbar">
        <el-select
          v-model="trendQuery.storeId"
          placeholder="全部门店（品牌汇总）"
          clearable
          filterable
          class="toolbar-select"
          @change="loadTrend"
        >
          <el-option v-for="store in stores" :key="store.id" :value="store.id" :label="store.storeName" />
        </el-select>
        <el-radio-group v-model="trendQuery.granularity" @change="loadTrend">
          <el-radio-button v-for="item in GRANULARITY" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button>
        </el-radio-group>
      </div>

      <el-table v-loading="trendLoading" :data="trend" stripe>
        <el-table-column prop="period" label="时段" min-width="130" />
        <el-table-column label="订单数" width="120" align="right">
          <template #default="{ row }">
            <span class="mono">{{ row.orderCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="销售额" width="140" align="right">
          <template #default="{ row }"><span class="mono">{{ fen2yuan(row.salesAmount) }}</span></template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 门店日结 -->
    <el-card shadow="never" class="settle-card">
      <template #header>门店日结对账（已支付未退款口径）</template>
      <div class="page-toolbar">
        <el-select
          v-model="settleQuery.storeId"
          placeholder="选择门店"
          clearable
          filterable
          class="toolbar-select"
          @change="loadSettle"
        >
          <el-option v-for="store in stores" :key="store.id" :value="store.id" :label="store.storeName" />
        </el-select>
        <el-date-picker v-model="settleQuery.date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="loadSettle" />
        <el-button type="primary" @click="loadSettle">查询</el-button>
      </div>

      <el-empty v-if="!settleQuery.storeId" description="请先选择门店" />
      <el-descriptions v-else v-loading="settleLoading" :column="4" border>
        <el-descriptions-item label="日期">{{ settle?.date ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="订单数">{{ settle?.orderCount ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="实收金额">{{ settle ? fen2yuan(settle.salesAmount) : '—' }}</el-descriptions-item>
        <el-descriptions-item label="客单价">{{ settle ? fen2yuan(settle.avgOrderAmount) : '—' }}</el-descriptions-item>
        <el-descriptions-item label="退款笔数">{{ settle?.refundCount ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="退款金额">{{ settle ? fen2yuan(settle.refundAmount) : '—' }}</el-descriptions-item>
        <el-descriptions-item label="微信实收">{{ settle ? fen2yuan(settle.wxAmount) : '—' }}</el-descriptions-item>
        <el-descriptions-item label="储值实收">{{ settle ? fen2yuan(settle.walletAmount) : '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <div class="page-note">对应 PRD：F-RPT-01</div>
  </div>
</template>

<style scoped>
/* 门店下拉占位文案长（全部门店（品牌汇总）），全局 140px 放不下 */
.page-toolbar .toolbar-select {
  width: 220px;
}

.settle-card {
  margin-top: 16px;
}
</style>
