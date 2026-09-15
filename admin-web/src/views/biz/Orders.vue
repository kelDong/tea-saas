<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { franchiseApi } from '@/api/franchise'
import { orderApi } from '@/api/order'
import { ORDER_STATUS_MAP, ORDER_STATUS_OPTIONS, ORDER_SOURCE_MAP, PAY_CHANNEL_MAP } from '@/constants/order'
import type { FraStore } from '@/types/franchise'
import type { OrderDetailVO, OrdOrder } from '@/types/order'
import { fen2yuan, formatDateTime } from '@/utils/format'

// ---------- 列表 ----------
const loading = ref(false)
const rows = ref<OrdOrder[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, storeId: undefined as number | undefined, status: undefined as number | undefined })

const stores = ref<FraStore[]>([])
const storeNameMap = computed(() => new Map(stores.value.map((store) => [store.id, store.storeName])))

async function loadStores() {
  const result = await franchiseApi.storePage({ page: 1, size: 100 })
  stores.value = result.records
}

async function load() {
  loading.value = true
  try {
    const result = await orderApi.page(query)
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.storeId = undefined
  query.status = undefined
  search()
}

function statusInfo(status: number) {
  return ORDER_STATUS_MAP.get(status) ?? { label: String(status), tag: 'info' as const }
}

function storeName(storeId: number): string {
  return storeNameMap.value.get(storeId) ?? `门店${storeId}`
}

// ---------- 详情 ----------
const drawerVisible = ref(false)
const detail = ref<OrderDetailVO | null>(null)
const detailLoading = ref(false)

async function openDetail(row: OrdOrder) {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await orderApi.detail(row.id)
  } finally {
    detailLoading.value = false
  }
}

// ---------- 状态流转（v-permission 按钮码：order:accept/make/finish） ----------
const flowLoading = ref<number | null>(null)

/** 行与详情抽屉共用（OrdOrder/OrderDetailVO 都满足此形状） */
async function flow(row: { id: number; status: number }, action: 'accept' | 'make' | 'finish', tip: string) {
  flowLoading.value = row.id
  try {
    if (action === 'accept') await orderApi.accept(row.id)
    else if (action === 'make') await orderApi.make(row.id)
    else await orderApi.finish(row.id)
    ElMessage.success(tip)
    load()
    if (detail.value?.id === row.id) {
      detail.value = await orderApi.detail(row.id)
    }
  } finally {
    flowLoading.value = null
  }
}

onMounted(() => {
  load()
  loadStores()
})
</script>

<template>
  <div class="orders-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.storeId" placeholder="全部门店" clearable class="toolbar-select" @change="search">
          <el-option v-for="store in stores" :key="store.id" :value="store.id" :label="store.storeName" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable class="toolbar-select" @change="search">
          <el-option v-for="item in ORDER_STATUS_OPTIONS" :key="item.value" :value="item.value" :label="item.label" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="200" show-overflow-tooltip />
        <el-table-column label="门店" min-width="130">
          <template #default="{ row }">{{ storeName(row.storeId) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="90">
          <template #default="{ row }">{{ ORDER_SOURCE_MAP[row.orderSource] ?? row.orderSource }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusInfo(row.status).tag" size="small">{{ statusInfo(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="商品总额" width="100" align="right">
          <template #default="{ row }">{{ fen2yuan(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="优惠" width="90" align="right">
          <template #default="{ row }">{{ fen2yuan(row.discountAmount) }}</template>
        </el-table-column>
        <el-table-column label="实付" width="100" align="right">
          <template #default="{ row }">{{ fen2yuan(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column label="支付渠道" width="90">
          <template #default="{ row }">
            {{ row.payChannel != null ? (PAY_CHANNEL_MAP[row.payChannel] ?? row.payChannel) : '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="takeCode" label="取餐码" width="80">
          <template #default="{ row }">{{ row.takeCode || '—' }}</template>
        </el-table-column>
        <el-table-column label="支付时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.payTime) }}</template>
        </el-table-column>
        <el-table-column label="下单时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 2"
              v-permission="'order:accept'"
              link
              type="primary"
              :loading="flowLoading === row.id"
              @click="flow(row, 'accept', '已接单')"
            >接单</el-button>
            <el-button
              v-if="row.status === 3"
              v-permission="'order:make'"
              link
              type="warning"
              :loading="flowLoading === row.id"
              @click="flow(row, 'make', '已标记制作完成')"
            >制作完成</el-button>
            <el-button
              v-if="row.status === 4"
              v-permission="'order:finish'"
              link
              type="success"
              :loading="flowLoading === row.id"
              @click="flow(row, 'finish', '已核销取餐')"
            >核销取餐</el-button>
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <!-- 订单详情 -->
    <el-drawer v-model="drawerVisible" title="订单详情" size="640px">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="2" border>
          <el-descriptions-item label="订单号" :span="2">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="门店">{{ storeName(detail.storeId) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusInfo(detail.status).tag" size="small">{{ statusInfo(detail.status).label }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="来源">{{ ORDER_SOURCE_MAP[detail.orderSource] ?? detail.orderSource }}</el-descriptions-item>
          <el-descriptions-item label="会员ID">{{ detail.memberId ?? '散客' }}</el-descriptions-item>
          <el-descriptions-item label="商品总额">{{ fen2yuan(detail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="优惠金额">{{ fen2yuan(detail.discountAmount) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">{{ fen2yuan(detail.payAmount) }}</el-descriptions-item>
          <el-descriptions-item label="支付渠道">
            {{ detail.payChannel != null ? (PAY_CHANNEL_MAP[detail.payChannel] ?? detail.payChannel) : '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="取餐码">{{ detail.takeCode || '—' }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ formatDateTime(detail.payTime) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatDateTime(detail.finishTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.cancelReason" label="取消原因" :span="2">
            {{ detail.cancelReason }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.remark" label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
        </el-descriptions>

        <el-table v-if="detail?.items?.length" :data="detail.items" stripe class="items-table">
          <el-table-column prop="productName" label="商品" min-width="140" />
          <el-table-column prop="specDesc" label="规格" min-width="120">
            <template #default="{ row }">{{ row.specDesc || '—' }}</template>
          </el-table-column>
          <el-table-column label="单价" width="90" align="right">
            <template #default="{ row }">{{ fen2yuan(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="qty" label="数量" width="70" align="center" />
          <el-table-column label="小计" width="100" align="right">
            <template #default="{ row }">{{ fen2yuan(row.amount) }}</template>
          </el-table-column>
        </el-table>

        <div class="drawer-actions">
          <el-button
            v-if="detail?.status === 2"
            v-permission="'order:accept'"
            type="primary"
            @click="detail && flow(detail, 'accept', '已接单')"
          >接单</el-button>
          <el-button
            v-if="detail?.status === 3"
            v-permission="'order:make'"
            type="warning"
            @click="detail && flow(detail, 'make', '已标记制作完成')"
          >标记制作完成</el-button>
          <el-button
            v-if="detail?.status === 4"
            v-permission="'order:finish'"
            type="success"
            @click="detail && flow(detail, 'finish', '已核销取餐')"
          >核销取餐</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-select {
  width: 180px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.items-table {
  margin-top: 16px;
}

.drawer-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
