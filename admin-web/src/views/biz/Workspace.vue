<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { formulaApi } from '@/api/bom'
import { franchiseApi } from '@/api/franchise'
import { refundApi } from '@/api/order'
import { purchaseApi } from '@/api/supply'
import { reportApi } from '@/api/report'
import type { DashboardVO, HotProductVO, ReportInventoryRow, StoreRankVO } from '@/types/report'
import { fen2yuan } from '@/utils/format'

const router = useRouter()

// ---------- 统计卡 ----------
const loading = ref(false)
const dashboard = ref<DashboardVO | null>(null)
/** 在线门店（在营中 businessStatus=1）/ 打烊（在营中 businessStatus=0） */
const storeStat = ref({ online: 0, total: 0, closed: 0 })
/** 需补货门店数（预警行的去重门店数） */
const warnStoreCount = ref(0)

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

/** 门店名映射（待办文案用）；loadCards/loadTodos 并行，取名字经此共享兜底 */
const storeNames = ref<Map<number, string>>(new Map())

async function ensureStoreNames(): Promise<Map<number, string>> {
  if (storeNames.value.size === 0) {
    try {
      const stores = await franchiseApi.storePage({ page: 1, size: 100 })
      storeNames.value = new Map(stores.list.map((s) => [s.id, s.storeName]))
    } catch {
      /* 拿不到门店名时待办文案降级显示「门店」 */
    }
  }
  return storeNames.value
}

// ---------- 待办事项 ----------
interface TodoItem {
  type: string
  tagType: 'warning' | 'danger' | 'success'
  content: string
  status: string
  action: string
  to: string
}

const todoLoading = ref(false)
const todos = ref<TodoItem[]>([])

// ---------- 门店排行 ----------
interface RankRow {
  storeId: number
  storeName: string
  salesAmount: number
  orderCount: number
  /** 营业额环比（昨日为 0 时 null） */
  deltaPct: number | null
}

const rankLoading = ref(false)
const rankRows = ref<RankRow[]>([])
const RANK_MEDALS = ['🥇', '🥈', '🥉']

// ---------- 库存预警 ----------
const warnLoading = ref(false)
const warnRows = ref<ReportInventoryRow[]>([])

// ---------- 近 7 日热销 ----------
const hotLoading = ref(false)
const hotProducts = ref<HotProductVO[]>([])

/** 本地时区 yyyy-MM-dd（不能用 toISOString，UTC 会在 0-8 点错一天） */
function fmtDate(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

async function loadCards() {
  loading.value = true
  try {
    dashboard.value = await reportApi.dashboard()
  } finally {
    loading.value = false
  }
  // 在线门店口径：在营(status=1)中按 businessStatus 拆分
  try {
    const stores = await franchiseApi.storePage({ page: 1, size: 100 })
    if (storeNames.value.size === 0) {
      storeNames.value = new Map(stores.list.map((s) => [s.id, s.storeName]))
    }
    const active = stores.list.filter((s) => s.status === 1)
    const online = active.filter((s) => s.businessStatus === 1).length
    storeStat.value = { online, total: active.length, closed: active.length - online }
  } catch {
    storeStat.value = { online: 0, total: 0, closed: 0 }
  }
  // 库存预警行（预警优先排序）+ 去重门店数；size 上限 100，超大规模时门店数为近似
  warnLoading.value = true
  try {
    const inv = await reportApi.inventorySummary({ page: 1, size: 100 })
    const warns = inv.list.filter((r) => r.warn === 1)
    warnRows.value = warns.slice(0, 5)
    warnStoreCount.value = new Set(warns.map((r) => r.storeId)).size
  } finally {
    warnLoading.value = false
  }
}

async function loadTodos() {
  todoLoading.value = true
  try {
    const names = await ensureStoreNames()
    // 各来源独立兜底：单接口失败不断整面板
    const [confirming, shipping, refunds, formulas] = await Promise.allSettled([
      purchaseApi.page({ page: 1, size: 3, status: 1 }),
      purchaseApi.page({ page: 1, size: 2, status: 2 }),
      refundApi.page({ page: 1, size: 5 }),
      formulaApi.page({ page: 1, size: 5 }),
    ])
    const list: TodoItem[] = []
    if (confirming.status === 'fulfilled') {
      for (const po of confirming.value.list) {
        list.push({
          type: '采购',
          tagType: 'warning',
          content: `「${names.get(po.storeId) ?? '门店'}」采购单 #${po.orderNo}`,
          status: '待确认',
          action: '去处理',
          to: '/supply/purchases',
        })
      }
    }
    if (shipping.status === 'fulfilled') {
      for (const po of shipping.value.list) {
        list.push({
          type: '采购',
          tagType: 'warning',
          content: `「${names.get(po.storeId) ?? '门店'}」采购单 #${po.orderNo}`,
          status: '待发货',
          action: '去处理',
          to: '/supply/purchases',
        })
      }
    }
    if (refunds.status === 'fulfilled') {
      for (const rf of refunds.value.list.filter((r) => r.status === 1).slice(0, 2)) {
        list.push({
          type: '退款',
          tagType: 'danger',
          content: `订单 #${rf.orderId} 申请退款 ${fen2yuan(rf.amount)}`,
          status: '处理中',
          action: '去处理',
          to: '/biz/orders',
        })
      }
    }
    if (formulas.status === 'fulfilled') {
      // 0停用/1正常(未发布)/2已发布
      for (const f of formulas.value.list.filter((r) => r.status === 1).slice(0, 1)) {
        list.push({
          type: '配方',
          tagType: 'success',
          content: `「${f.formulaName}」配方待发布`,
          status: '待发布',
          action: '去审核',
          to: '/product/formulas',
        })
      }
    }
    todos.value = list.slice(0, 6)
  } finally {
    todoLoading.value = false
  }
}

async function loadRank() {
  rankLoading.value = true
  try {
    const today = fmtDate(new Date())
    const yesterday = fmtDate(new Date(Date.now() - 86_400_000))
    const [todayRank, yesterdayRank] = await Promise.all([
      reportApi.storeRanking(today),
      reportApi.storeRanking(yesterday).catch(() => [] as StoreRankVO[]),
    ])
    const yMap = new Map(yesterdayRank.map((r) => [r.storeId, r.salesAmount ?? 0]))
    rankRows.value = todayRank.slice(0, 5).map((r) => {
      const y = yMap.get(r.storeId)
      const t = r.salesAmount ?? 0
      return {
        storeId: r.storeId,
        storeName: r.storeName ?? `门店#${r.storeId}`,
        salesAmount: t,
        orderCount: r.orderCount ?? 0,
        deltaPct: y ? ((t - y) / y) * 100 : null,
      }
    })
  } catch {
    rankRows.value = []
  } finally {
    rankLoading.value = false
  }
}

async function loadHot() {
  hotLoading.value = true
  try {
    hotProducts.value = await reportApi.hotProducts({ days: 7, limit: 10 })
  } finally {
    hotLoading.value = false
  }
}

/** 库存水位 = 当前库存/预警阈值（预警行恒 ≤100%） */
function waterPct(row: ReportInventoryRow): number {
  if (!row.warnThreshold) return 0
  return Math.min(100, ((row.stockQty ?? 0) / row.warnThreshold) * 100)
}

function rankDelta(row: RankRow): string {
  if (row.deltaPct == null) return '—'
  return `${row.deltaPct >= 0 ? '↑' : '↓'} ${Math.abs(row.deltaPct).toFixed(0)}%`
}

function load() {
  loadCards()
  loadTodos()
  loadRank()
  loadHot()
}

onMounted(load)
</script>

<template>
  <div class="workspace-page">
    <!-- 核心指标 -->
    <div v-loading="loading" class="stat-row">
      <div class="stat-card">
        <div class="label">今日营业额</div>
        <div class="value">{{ fen2yuan(dashboard?.todaySales ?? 0) }}</div>
        <div class="sub" :class="salesDelta.startsWith('↓') ? 'down' : 'up'">{{ salesDelta }}</div>
      </div>
      <div class="stat-card">
        <div class="label">今日订单量</div>
        <div class="value">{{ dashboard?.todayOrders ?? 0 }}</div>
        <div class="sub" :class="ordersDelta.startsWith('↓') ? 'down' : 'up'">{{ ordersDelta }}</div>
      </div>
      <div class="stat-card">
        <div class="label">在线门店</div>
        <div class="value">{{ storeStat.online }} / {{ storeStat.total }}</div>
        <div class="sub down">{{ storeStat.closed }} 家打烊</div>
      </div>
      <div class="stat-card">
        <div class="label">低库存原料</div>
        <div class="value down">{{ dashboard?.stockWarnCount ?? 0 }}</div>
        <div class="sub down">{{ warnStoreCount }} 家门店需补货</div>
      </div>
    </div>

    <div class="grid2">
      <!-- 待办事项 -->
      <div v-loading="todoLoading" class="panel">
        <div class="panel-head">
          <span class="title">待办事项</span>
          <span class="more" @click="router.push('/supply/purchases')">查看全部 ›</span>
        </div>
        <el-table :data="todos">
          <el-table-column label="类型" width="70">
            <template #default="{ row }">
              <el-tag :type="row.tagType" size="small">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="80" />
          <el-table-column label="操作" width="76">
            <template #default="{ row }">
              <el-button link type="primary" @click="router.push(row.to)">{{ row.action }}</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty :image-size="48" description="暂无待办" />
          </template>
        </el-table>
      </div>

      <!-- 门店经营排行 -->
      <div v-loading="rankLoading" class="panel">
        <div class="panel-head">
          <span class="title">门店经营排行 · 今日</span>
          <span class="more" @click="router.push('/biz/store-report')">查看报表 ›</span>
        </div>
        <el-table :data="rankRows">
          <el-table-column label="排名" width="60" align="center">
            <template #default="{ $index }">
              <span class="rank-medal">{{ $index < 3 ? RANK_MEDALS[$index] : $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="storeName" label="门店" min-width="110" show-overflow-tooltip />
          <el-table-column label="营业额" width="100" align="right">
            <template #default="{ row }">
              <span class="mono">{{ fen2yuan(row.salesAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="订单" width="64" align="right">
            <template #default="{ row }">
              <span class="mono">{{ row.orderCount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="环比" width="84" align="center">
            <template #default="{ row }">
              <el-tag
                size="small"
                :type="row.deltaPct == null ? 'info' : row.deltaPct >= 0 ? 'success' : 'danger'"
              >
                {{ rankDelta(row) }}
              </el-tag>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty :image-size="48" description="今日暂无营业数据" />
          </template>
        </el-table>
      </div>
    </div>

    <!-- 库存预警 -->
    <div v-loading="warnLoading" class="panel">
      <div class="panel-head">
        <span class="title">库存预警 · 需补货</span>
        <span class="more" @click="router.push('/supply/inventory')">库存总览 ›</span>
      </div>
      <el-table :data="warnRows">
        <el-table-column prop="storeName" label="门店" min-width="140" show-overflow-tooltip />
        <el-table-column prop="materialName" label="原料" min-width="160" show-overflow-tooltip />
        <el-table-column label="当前库存" width="100" align="right">
          <template #default="{ row }">
            <span class="mono">{{ row.stockQty }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预警阈值" width="100" align="right">
          <template #default="{ row }">
            <span class="mono">{{ row.warnThreshold ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存水位" min-width="150">
          <template #default="{ row }">
            <div
              class="bar"
              :title="row.warnThreshold ? `库存/阈值 ${waterPct(row).toFixed(0)}%` : '未配置阈值'"
            >
              <i :style="{ width: `${waterPct(row)}%` }" />
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :image-size="48" description="库存充足，暂无预警" />
        </template>
      </el-table>
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

/* grid2 内的面板去掉自身下边距，由 grid 的 gap 接管 */
.grid2 {
  margin-bottom: 16px;
}

.grid2 .panel {
  margin-bottom: 0;
}

.rank-medal {
  font-variant-numeric: tabular-nums;
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
