<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { reportApi } from '@/api/report'
import type { HotProductVO, ReportInventoryRow, StoreRankVO } from '@/types/report'
import { fen2yuan } from '@/utils/format'

function yesterday(): string {
  return new Date(Date.now() - 86400000).toISOString().slice(0, 10)
}

// ---------- 门店排行 ----------
const rankLoading = ref(false)
const rankDate = ref(yesterday())
const ranks = ref<StoreRankVO[]>([])

async function loadRanks() {
  rankLoading.value = true
  try {
    ranks.value = await reportApi.storeRanking(rankDate.value || undefined)
  } finally {
    rankLoading.value = false
  }
}

// ---------- 热销商品 ----------
const hotLoading = ref(false)
const hotQuery = reactive({ days: 7, limit: 10 })
const hotProducts = ref<HotProductVO[]>([])

async function loadHot() {
  hotLoading.value = true
  try {
    hotProducts.value = await reportApi.hotProducts({ days: hotQuery.days, limit: hotQuery.limit })
  } finally {
    hotLoading.value = false
  }
}

// ---------- 库存总览（预警优先） ----------
const invLoading = ref(false)
const invRows = ref<ReportInventoryRow[]>([])
const invTotal = ref(0)
const invQuery = reactive({ page: 1, size: 10 })

async function loadInv() {
  invLoading.value = true
  try {
    const result = await reportApi.inventorySummary({ page: invQuery.page, size: invQuery.size })
    invRows.value = result.list
    invTotal.value = result.total
  } finally {
    invLoading.value = false
  }
}

function invSearch() {
  invQuery.page = 1
  loadInv()
}

function onTab(name: string | number) {
  if (name === 'rank' && !ranks.value.length) loadRanks()
}

onMounted(() => {
  loadRanks()
  loadHot()
  loadInv()
})
</script>

<template>
  <div class="brand-report-page">
    <el-tabs @tab-change="onTab">
      <el-tab-pane label="门店排行">
        <el-card shadow="never">
          <div class="toolbar">
            <el-date-picker v-model="rankDate" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="loadRanks" />
            <el-button type="primary" @click="loadRanks">查询</el-button>
          </div>
          <el-table v-loading="rankLoading" :data="ranks" stripe>
            <el-table-column label="名次" width="70" align="center">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column prop="storeName" label="门店" min-width="160" />
            <el-table-column label="日实收" width="140" align="right">
              <template #default="{ row }">{{ fen2yuan(row.salesAmount) }}</template>
            </el-table-column>
            <el-table-column prop="orderCount" label="订单数" width="110" align="right" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="热销商品">
        <el-card shadow="never">
          <div class="toolbar">
            <el-select v-model="hotQuery.days" class="toolbar-select" @change="loadHot">
              <el-option :value="7" label="近 7 天" />
              <el-option :value="30" label="近 30 天" />
            </el-select>
            <el-button type="primary" @click="loadHot">查询</el-button>
          </div>
          <el-table v-loading="hotLoading" :data="hotProducts" stripe>
            <el-table-column label="名次" width="70" align="center">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column prop="productName" label="商品（订单快照名）" min-width="200" show-overflow-tooltip />
            <el-table-column prop="qty" label="销量(份)" width="110" align="right" />
            <el-table-column label="销售额" width="140" align="right">
              <template #default="{ row }">{{ fen2yuan(row.salesAmount) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="库存总览">
        <el-card shadow="never">
          <el-table v-loading="invLoading" :data="invRows" stripe>
            <el-table-column prop="storeName" label="门店" min-width="140" />
            <el-table-column prop="materialName" label="原料" min-width="140" />
            <el-table-column prop="stockQty" label="库存量" width="110" align="right" />
            <el-table-column label="预警阈值" width="110" align="right">
              <template #default="{ row }">{{ row.warnThreshold ?? '未配置' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.warn === 1 ? 'danger' : 'success'" size="small">
                  {{ row.warn === 1 ? '预警' : '正常' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="invQuery.page"
            v-model:page-size="invQuery.size"
            :total="invTotal"
            layout="total, prev, pager, next, sizes"
            :page-sizes="[10, 20, 50]"
            class="pager"
            @current-change="loadInv"
            @size-change="invSearch"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-select {
  width: 140px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
