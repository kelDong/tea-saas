<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'

import { franchiseApi } from '@/api/franchise'
import { orderApi, refundApi } from '@/api/order'
import type { OrdOrder, OrderDetailVO, WsOrderMessage } from '@/types/order'
import { ORDER_SOURCE_MAP, ORDER_STATUS_MAP, PAY_CHANNEL_MAP } from '@/constants/order'
import { fen2yuan, formatDateTime } from '@/utils/format'
import { createOrderWs, type WsStatus } from '@/utils/orderWs'

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

// ---------- 状态流转（2→3→4→5）+ 取消/退款 ----------
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

/** B 端取消（仅待支付单；回补库存/积分，券不退还） */
async function cancelOrder(row: OrdOrder) {
  let reason: string | undefined
  try {
    const { value } = await ElMessageBox.prompt('取消原因（可留空）', `取消订单 ${row.orderNo}`, {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      inputPlaceholder: '默认「门店取消」',
      inputValidator: (v: string) => !v || v.length <= 128 || '原因最长128字',
      type: 'warning',
    })
    reason = value?.trim() || undefined
  } catch {
    return
  }
  flowLoading.value = row.id
  try {
    await orderApi.cancel(row.id, reason)
    ElMessage.success('订单已取消')
    load()
  } finally {
    flowLoading.value = null
  }
}

/** 整单退款（仅已支付/制作中/待取餐；实付原路退回，回补库存，不可撤销） */
async function refundOrder(row: OrdOrder) {
  let reason: string | undefined
  try {
    const { value } = await ElMessageBox.prompt(
      `整单退款 ${fen2yuan(row.payAmount)} 元将按支付渠道退回并回补库存，不可撤销`,
      `退款 ${row.orderNo}`,
      {
        confirmButtonText: '确认退款',
        cancelButtonText: '返回',
        inputPlaceholder: '退款原因（可留空）',
        inputValidator: (v: string) => !v || v.length <= 128 || '原因最长128字',
        type: 'warning',
      },
    )
    reason = value?.trim() || undefined
  } catch {
    return
  }
  flowLoading.value = row.id
  try {
    await refundApi.apply(row.id, reason)
    ElMessage.success('退款已受理')
    load()
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

// ---------- 实时推送（/ws/orders，接口需求 8.3；非 R7/R8 角色握手被拒，查询按钮兜底） ----------
const wsStatus = ref<WsStatus>('connecting')
const WS_STATUS_VIEW: Record<WsStatus, { label: string; tag: 'success' | 'info' | 'warning' }> = {
  open: { label: '实时推送中', tag: 'success' },
  connecting: { label: '连接中', tag: 'warning' },
  closed: { label: '已断开·自动重连', tag: 'warning' },
  denied: { label: '实时推送不可用', tag: 'info' },
}

let reloadTimer: number | undefined
let audioCtx: AudioContext | null = null

/** 新单提示音（WebAudio；无用户手势时静默失败——通知横幅兜底） */
function beep() {
  try {
    audioCtx ??= new AudioContext()
    if (audioCtx.state === 'suspended') void audioCtx.resume()
    const osc = audioCtx.createOscillator()
    const gain = audioCtx.createGain()
    osc.type = 'sine'
    osc.frequency.value = 880
    gain.gain.setValueAtTime(0.3, audioCtx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.6)
    osc.connect(gain).connect(audioCtx.destination)
    osc.start()
    osc.stop(audioCtx.currentTime + 0.6)
  } catch {
    // 音频不可用忽略
  }
}

function handleWsMessage(msg: WsOrderMessage) {
  // WS 订阅名下门店全集，页面只关心当前门店（雪花 ID 两侧均为字符串口径）
  if (!storeId.value || String(msg.storeId) !== String(storeId.value)) return
  if (msg.type === 'NEW_ORDER') {
    ElNotification({
      title: '新订单',
      message: `${msg.orderNo}（取餐码 ${msg.takeCode ?? '—'}）待接单`,
      type: 'success',
      duration: 6000,
    })
    beep()
  }
  // 防抖合并：短时多单/多次状态变更只重查一次
  if (reloadTimer !== undefined) window.clearTimeout(reloadTimer)
  reloadTimer = window.setTimeout(() => {
    reloadTimer = undefined
    load()
  }, 300)
}

const orderWs = createOrderWs({
  onMessage: handleWsMessage,
  onStatus: (status) => {
    wsStatus.value = status
  },
})

onUnmounted(() => {
  orderWs.stop()
  if (reloadTimer !== undefined) window.clearTimeout(reloadTimer)
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
        <el-tag
          :type="WS_STATUS_VIEW[wsStatus].tag"
          size="small"
          effect="plain"
          class="ws-status"
        >{{ WS_STATUS_VIEW[wsStatus].label }}</el-tag>
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
        <el-table-column label="操作" width="240" fixed="right">
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
            <el-button
              v-if="row.status === 1"
              v-permission="'order:cancel'"
              link
              type="danger"
              :loading="flowLoading === row.id"
              @click="cancelOrder(row)"
            >取消</el-button>
            <el-button
              v-if="[2, 3, 4].includes(row.status)"
              v-permission="'order:refund'"
              link
              type="danger"
              :loading="flowLoading === row.id"
              @click="refundOrder(row)"
            >退款</el-button>
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

.ws-status {
  margin-left: 4px;
}

/* 平板/触屏（门店平板复用后台，PRD §2.2）：放大字号与触控行高 */
@media (pointer: coarse) {
  .store-orders :deep(.el-table) {
    font-size: 15px;
    --el-table-row-height: 56px;
  }

  .store-orders :deep(.el-button) {
    font-size: 15px;
  }

  .store-orders :deep(.el-tag) {
    font-size: 13px;
  }
}
</style>
