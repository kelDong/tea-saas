<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { reportApi } from '@/api/report'
import type { DashboardVO, HotProductVO } from '@/types/report'
import { fen2yuan } from '@/utils/format'

const loading = ref(false)
const dashboard = ref<DashboardVO | null>(null)
const hotLoading = ref(false)
const hotProducts = ref<HotProductVO[]>([])

/** 环比(昨日为 0 时不显示百分比,避免除零) */
function delta(today: number | undefined, yesterday: number | undefined): string {
  if (!yesterday || today == null) {
    return ''
  }
  const pct = ((today - yesterday) / yesterday) * 100
  return `${pct >= 0 ? '↑' : '↓'} ${Math.abs(pct).toFixed(1)}% vs 昨日`
}

const salesDelta = computed(() => delta(dashboard.value?.todaySales, dashboard.value?.yesterdaySales))
const ordersDelta = computed(() => delta(dashboard.value?.todayOrders, dashboard.value?.yesterdayOrders))

async function load() {
  loading.value = true
  hotLoading.value = true
  try {
    dashboard.value = await reportApi.dashboard()
  } finally {
    loading.value = false
  }
  try {
    hotProducts.value = await reportApi.hotProducts({ days: 7, limit: 10 })
  } finally {
    hotLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="workspace-page">
    <!-- 核心指标 -->
    <div v-loading="loading" class="stat-row">
      <div class="stat-card">
        <div class="label">今日销售额</div>
        <div class="value">{{ fen2yuan(dashboard?.todaySales ?? 0) }}</div>
        <div class="sub" :class="salesDelta.startsWith('↓') ? 'down' : 'up'">{{ salesDelta }}</div>
      </div>
      <div class="stat-card">
        <div class="label">今日订单数</div>
        <div class="value">{{ dashboard?.todayOrders ?? 0 }}</div>
        <div class="sub" :class="ordersDelta.startsWith('↓') ? 'down' : 'up'">{{ ordersDelta }}</div>
      </div>
      <div class="stat-card">
        <div class="label">会员总数</div>
        <div class="value">{{ dashboard?.memberTotal ?? 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="label">在营门店</div>
        <div class="value">{{ dashboard?.storeTotal ?? 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="label">库存预警</div>
        <div class="value down">{{ dashboard?.stockWarnCount ?? 0 }}</div>
      </div>
    </div>

    <!-- 近7日热销 -->
    <el-card shadow="never">
      <template #header>
        <div class="panel-title-row">
          <span>近 7 日热销商品 TOP10</span>
          <el-button link type="primary" @click="load">刷新</el-button>
        </div>
      </template>
      <el-table v-loading="hotLoading" :data="hotProducts" stripe>
        <el-table-column label="排名" width="70" align="center">
          <template #default="{ $index }">
            <span :class="['rank', $index < 3 ? `top${$index + 1}` : '']">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品" min-width="200" show-overflow-tooltip />
        <el-table-column prop="qty" label="销量(份)" width="110" align="right">
          <template #default="{ row }">
            <span class="mono">{{ row.qty }}</span>
          </template>
        </el-table-column>
        <el-table-column label="销售额" width="130" align="right">
          <template #default="{ row }">
            <span class="mono">{{ fen2yuan(row.salesAmount) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.panel-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 700;
}

.rank {
  color: var(--ts-ink-2);
  font-variant-numeric: tabular-nums;
}

.rank.top1 {
  color: var(--ts-primary-deep);
  font-weight: 700;
}

.rank.top2 {
  color: var(--ts-ink-2);
  font-weight: 700;
}

.rank.top3 {
  color: var(--ts-warning);
  font-weight: 700;
}
</style>
