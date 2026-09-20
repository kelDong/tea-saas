<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import { franchiseApi } from '@/api/franchise'
import { orderApi } from '@/api/order'
import type { OrdOrder, OrderDetailVO } from '@/types/order'
import { ORDER_SOURCE_MAP, ORDER_STATUS_MAP, PAY_CHANNEL_MAP } from '@/constants/order'
import { fen2yuan, formatDateTime } from '@/utils/format'

// 快捷状态过滤（门店视角默认待处理链路）
const STATUS_OPTIONS = [2, 3, 4, 5, 1, 6, 7]

const stores = ref<Array<{ id: number; storeName: string }>>([])
const storeId = ref<number | undefined>(undefined)

const loading = ref(false)
const rows = ref<OrdOrder[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  status: undefined as number | undefined,
})

async function load() {
  if (!storeId.value) return
  loading.value = true
  try {
    const result = await orderApi.page({ ...query, storeId: storeId.value })
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

watch(storeId, search)

// ---------- 状态流转（2→3→4→5） ----------
const flowLoading = ref<number | null>(null)

async function flow(row: OrdOrder, action: 'accept' | 'make' | 'finish') {
  flowLoading.value = row.id
  try {
    if (action === 'accept') await orderApi.accept(row.id)
    else if (action === 'make') await orderApi.make(row.id)
    else await orderApi.finish(row.id)
    ElMessage.success('已流转')
    load()
    if (detail.value?.id === row.id) {
      detail.value = await orderApi.detail(row.id)
    }
  } finally {
    flowLoading.value = null
  }
}

// ---------- 详情 ----------
const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<OrderDetailVO | null>(null)

async function openDetail(row: OrdOrder) {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await orderApi.detail(row.id)
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  stores.value = (await franchiseApi.storePage({ page: 1, size: 100 })).list
  if (stores.value.length) {
    storeId.value = stores.value[0].id
  }
})
</script>

<template>
  <div class="store-orders">
    <el-card shadow="never">
      <div class="page-toolbar">
        <el-select v-model="storeId" placeholder="选择门店" filterable class="toolbar-select">
          <el-option v-for="store in stores" :key="store.id" :value="store.id" :label="store.storeName" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable class="toolbar-select" @change="search">
          <el-option v-for="value in STATUS_OPTIONS" :key="value" :value="value" :label="ORDER_STATUS_MAP.get(value)?.label ?? String(value)" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-empty v-if="!storeId" description="当前租户暂无门店" />
      <el-table v-else v-loading="loading" :data="rows" stripe>
        <el-table-column label="单号" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="takeCode" label="取餐码" width="80" />
        <el-table-column label="来源" width="90">
          <template #default="{ row }">{{ ORDER_SOURCE_MAP[row.orderSource] ?? row.orderSource }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="ORDER_STATUS_MAP.get(row.status)?.tag ?? 'info'" size="small">
              {{ ORDER_STATUS_MAP.get(row.status)?.label ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="实付" width="100" align="right">
          <template #default="{ row }">
            <span class="mono">{{ fen2yuan(row.payAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支付方式" width="90">
          <template #default="{ row }">
            {{ row.payChannel ? (PAY_CHANNEL_MAP[row.payChannel] ?? row.payChannel) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="下单时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 2"
              v-permission="'order:accept'"
              link
              type="warning"
              :loading="flowLoading === row.id"
              @click="flow(row, 'accept')"
            >接单</el-button>
            <el-button
              v-if="row.status === 3"
              v-permission="'order:make'"
              link
              type="primary"
              :loading="flowLoading === row.id"
              @click="flow(row, 'make')"
            >制作完成</el-button>
            <el-button
              v-if="row.status === 4"
              v-permission="'order:finish'"
              link
              type="success"
              :loading="flowLoading === row.id"
              @click="flow(row, 'finish')"
            >核销取餐</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="storeId"
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

    <!-- 订单详情 -->
    <el-drawer v-model="drawerVisible" title="订单详情" size="560px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="单号" :span="2"><span class="mono">{{ detail.orderNo }}</span></el-descriptions-item>
            <el-descriptions-item label="取餐码">{{ detail.takeCode ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ ORDER_SOURCE_MAP[detail.orderSource] ?? detail.orderSource }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="ORDER_STATUS_MAP.get(detail.status)?.tag ?? 'info'" size="small">
                {{ ORDER_STATUS_MAP.get(detail.status)?.label ?? detail.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="支付方式">
              {{ detail.payChannel ? (PAY_CHANNEL_MAP[detail.payChannel] ?? detail.payChannel) : '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="应付"><span class="mono">{{ fen2yuan(detail.totalAmount) }}</span></el-descriptions-item>
            <el-descriptions-item label="实付"><span class="mono">{{ fen2yuan(detail.payAmount) }}</span></el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ formatDateTime(detail.payTime) }}</el-descriptions-item>
            <el-descriptions-item label="取餐时间">{{ formatDateTime(detail.finishTime) }}</el-descriptions-item>
          </el-descriptions>

          <el-table :data="detail.items" stripe class="items-table" size="small">
            <el-table-column prop="productName" label="商品" min-width="150">
              <template #default="{ row }">
                {{ row.productName }}
                <div v-if="row.specDesc" class="spec">{{ row.specDesc }}</div>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="70" align="right">
              <template #default="{ row }"><span class="mono">{{ row.qty }}</span></template>
            </el-table-column>
            <el-table-column label="单价" width="90" align="right">
              <template #default="{ row }"><span class="mono">{{ fen2yuan(row.price) }}</span></template>
            </el-table-column>
            <el-table-column label="小计" width="90" align="right">
              <template #default="{ row }"><span class="mono">{{ fen2yuan(row.amount) }}</span></template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>

    <div class="page-note">对应 PRD：F-ORD-03</div>
  </div>
</template>

<style scoped>
.items-table {
  margin-top: 16px;
}

.spec {
  font-size: 12px;
  color: var(--ts-ink-2);
}
</style>
