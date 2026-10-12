<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

import { franchiseApi } from '@/api/franchise'
import { reportApi } from '@/api/report'
import type { DailySettleVO } from '@/types/report'
import { fen2yuan } from '@/utils/format'

const stores = ref<Array<{ id: number; storeName: string }>>([])
const storeId = ref<number | undefined>(undefined)

function yesterday(): string {
  return new Date(Date.now() - 86400000).toISOString().slice(0, 10)
}

const date = ref(yesterday())
const loading = ref(false)
const settle = ref<DailySettleVO | null>(null)

async function load() {
  if (!storeId.value) return
  loading.value = true
  try {
    settle.value = await reportApi.storeDaily(storeId.value, date.value || undefined)
  } finally {
    loading.value = false
  }
}

watch(storeId, load)

onMounted(async () => {
  stores.value = (await franchiseApi.storePage({ page: 1, size: 100 })).list
  if (stores.value.length) {
    storeId.value = stores.value[0].id
  }
})
</script>

<template>
  <div class="settle-page">
    <el-card shadow="never">
      <template #header>门店日结对账（R7 店长权限；口径：已支付未退款）</template>
      <div class="page-toolbar">
        <span class="label">门店</span>
        <el-select v-model="storeId" placeholder="选择门店" filterable class="toolbar-select">
          <el-option v-for="store in stores" :key="store.id" :value="store.id" :label="store.storeName" />
        </el-select>
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="load" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-empty v-if="!storeId" description="当前租户暂无门店" />
      <div v-else v-loading="loading" class="stat-row">
        <div class="stat-card">
          <div class="label">营业额</div>
          <div class="value">{{ fen2yuan(settle?.salesAmount ?? 0) }}</div>
        </div>
        <div class="stat-card">
          <div class="label">订单数</div>
          <div class="value">{{ settle?.orderCount ?? 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="label">客单价</div>
          <div class="value">{{ fen2yuan(settle?.avgOrderAmount ?? 0) }}</div>
        </div>
      </div>
    </el-card>

    <el-card v-if="storeId" shadow="never" class="channel-card">
      <template #header>分渠道实收（微信 + 储值 + 现金 = 营业额）</template>
      <div class="stat-row">
        <div class="stat-card">
          <div class="label">微信</div>
          <div class="value">{{ fen2yuan(settle?.wxAmount ?? 0) }}</div>
        </div>
        <div class="stat-card">
          <div class="label">储值</div>
          <div class="value">{{ fen2yuan(settle?.walletAmount ?? 0) }}</div>
        </div>
        <div class="stat-card">
          <div class="label">现金</div>
          <div class="value">{{ fen2yuan(settle?.cashAmount ?? 0) }}</div>
        </div>
      </div>

      <el-descriptions :column="2" border class="refund-block">
        <el-descriptions-item label="退款笔数">{{ settle?.refundCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="退款金额">{{ fen2yuan(settle?.refundAmount ?? 0) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <div class="page-note">对应 PRD：F-ORD-02（日结对账）</div>
  </div>
</template>

<style scoped>
.page-toolbar .label {
  font-size: 13px;
  color: var(--ts-ink-2);
}

.channel-card {
  margin-top: 16px;
}

.refund-block {
  margin-top: 16px;
}
</style>
