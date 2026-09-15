<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { reportApi } from '@/api/report'
import type { DashboardVO, HotProductVO } from '@/types/report'
import { fen2yuan } from '@/utils/format'

const loading = ref(false)
const dashboard = ref<DashboardVO | null>(null)
const hotLoading = ref(false)
const hotProducts = ref<HotProductVO[]>([])

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
    <el-row v-loading="loading" :gutter="16">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat">
            <div class="label">今日销售额</div>
            <div class="value">{{ fen2yuan(dashboard?.todaySales ?? 0) }}</div>
            <div class="sub">昨日 {{ fen2yuan(dashboard?.yesterdaySales ?? 0) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat">
            <div class="label">今日订单数</div>
            <div class="value">{{ dashboard?.todayOrders ?? 0 }}</div>
            <div class="sub">昨日 {{ dashboard?.yesterdayOrders ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never">
          <div class="stat">
            <div class="label">会员总数</div>
            <div class="value">{{ dashboard?.memberTotal ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never">
          <div class="stat">
            <div class="label">在营门店</div>
            <div class="value">{{ dashboard?.storeTotal ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never">
          <div class="stat">
            <div class="label">库存预警</div>
            <div class="value warn">{{ dashboard?.stockWarnCount ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 近7日热销 -->
    <el-card shadow="never" class="hot-card">
      <template #header>
        <div class="card-header">
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
        <el-table-column prop="qty" label="销量(份)" width="110" align="right" />
        <el-table-column label="销售额" width="130" align="right">
          <template #default="{ row }">{{ fen2yuan(row.salesAmount) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.stat .label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.stat .value {
  font-size: 24px;
  font-weight: 600;
  margin-top: 8px;
}

.stat .value.warn {
  color: var(--el-color-danger);
}

.stat .sub {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-top: 4px;
}

.hot-card {
  margin-top: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rank {
  color: var(--el-text-color-secondary);
}

.rank.top1 {
  color: #e6a23c;
  font-weight: 700;
}

.rank.top2 {
  color: #909399;
  font-weight: 700;
}

.rank.top3 {
  color: #b87400;
  font-weight: 700;
}
</style>
