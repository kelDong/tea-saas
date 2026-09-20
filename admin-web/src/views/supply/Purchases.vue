<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import { materialApi } from '@/api/bom'
import { franchiseApi } from '@/api/franchise'
import { inventoryApi } from '@/api/inventory'
import { purchaseApi } from '@/api/supply'
import type { BomMaterial } from '@/types/bom'
import type { StockWarningVO } from '@/types/inventory'
import type { PurchaseOrderDetailVO, SupPurchaseOrder } from '@/types/supply'
import { fen2yuan, formatDateTime } from '@/utils/format'

const STATUS_LIST: Array<{ value: number; label: string; tag: 'warning' | 'primary' | 'info' | 'success' | 'danger' }> = [
  { value: 1, label: '待确认', tag: 'warning' },
  { value: 2, label: '已确认', tag: 'primary' },
  { value: 3, label: '已发货', tag: 'primary' },
  { value: 4, label: '已入库', tag: 'success' },
  { value: 5, label: '已取消', tag: 'info' },
]
const STATUS_MAP = new Map(STATUS_LIST.map((item) => [item.value, item]))

// ---------- 列表 ----------
const loading = ref(false)
const rows = ref<SupPurchaseOrder[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  status: undefined as number | undefined,
  storeId: undefined as number | undefined,
})

const stores = ref<Array<{ id: number; storeName: string }>>([])
const storeNameMap = computed(() => new Map(stores.value.map((item) => [item.id, item.storeName])))

function storeName(id: number): string {
  return storeNameMap.value.get(id) ?? `门店${id}`
}

async function loadStores() {
  stores.value = (await franchiseApi.storePage({ page: 1, size: 100 })).list
}

async function load() {
  loading.value = true
  try {
    const result = await purchaseApi.page({ ...query, status: query.status, storeId: query.storeId })
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

// ---------- 详情 ----------
const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<PurchaseOrderDetailVO | null>(null)

async function openDetail(row: SupPurchaseOrder) {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await purchaseApi.detail(row.id)
  } finally {
    detailLoading.value = false
  }
}

// ---------- 新建采购单（R6 发起，P1-1） ----------
const materials = ref<BomMaterial[]>([])
const createVisible = ref(false)
const createLoading = ref(false)
const createForm = reactive({
  storeId: undefined as number | undefined,
  remark: '',
  items: [] as Array<{ materialId: number | undefined; qty: number | undefined }>,
})

async function openCreate() {
  if (!materials.value.length) {
    materials.value = (await materialApi.page({ page: 1, size: 100 })).list
  }
  Object.assign(createForm, { storeId: undefined, remark: '', items: [{ materialId: undefined, qty: undefined }] })
  createVisible.value = true
}

function addItem() {
  createForm.items.push({ materialId: undefined, qty: undefined })
}

function removeItem(index: number) {
  createForm.items.splice(index, 1)
}

// ---------- 发起弹窗：库存低预警联动（F-SUP-03 AC「从门店库存低预警选品下单」） ----------
const warnings = ref<StockWarningVO[]>([])
const warningLoading = ref(false)

async function loadWarnings() {
  if (!createForm.storeId) {
    warnings.value = []
    return
  }
  warningLoading.value = true
  try {
    warnings.value = await inventoryApi.warnings(createForm.storeId)
  } finally {
    warningLoading.value = false
  }
}

watch(() => createForm.storeId, loadWarnings)

/** 缺口补回阈值，向上取 3 位小数并兜底最小采购量 */
function suggestedQty(gap: number): number {
  return Math.max(Number(gap.toFixed(3)), 0.001)
}

function bringWarning(row: StockWarningVO) {
  if (createForm.items.some((item) => item.materialId === row.materialId)) {
    ElMessage.info(`${row.materialName} 已在采购明细中`)
    return
  }
  dropEmptyItems()
  createForm.items.push({ materialId: row.materialId, qty: suggestedQty(row.gap) })
}

function bringAllWarnings() {
  const existing = new Set(createForm.items.map((item) => item.materialId).filter((id) => id != null))
  const additions = warnings.value
    .filter((row) => !existing.has(row.materialId))
    .map((row) => ({ materialId: row.materialId, qty: suggestedQty(row.gap) }))
  if (!additions.length) {
    ElMessage.info('预警原料均已在采购明细中')
    return
  }
  dropEmptyItems()
  createForm.items.push(...additions)
}

function dropEmptyItems() {
  for (let i = createForm.items.length - 1; i >= 0; i--) {
    if (createForm.items[i].materialId == null && createForm.items[i].qty == null) {
      createForm.items.splice(i, 1)
    }
  }
}

async function submitCreate() {
  if (!createForm.storeId) {
    ElMessage.warning('请选择门店')
    return
  }
  const items = createForm.items.filter((item) => item.materialId != null && item.qty != null)
  if (!items.length) {
    ElMessage.warning('请至少填写一行采购明细')
    return
  }
  createLoading.value = true
  try {
    await purchaseApi.create({
      storeId: createForm.storeId,
      remark: createForm.remark || undefined,
      items: items.map((item) => ({ materialId: item.materialId!, qty: item.qty! })),
    })
    ElMessage.success('采购单已提交（等级计价快照后端生成）')
    createVisible.value = false
    load()
  } finally {
    createLoading.value = false
  }
}

// ---------- 状态机 ----------
const flowLoading = ref<number | null>(null)

async function flow(row: SupPurchaseOrder, action: 'confirm' | 'ship' | 'receive' | 'cancel', tip: string) {
  flowLoading.value = row.id
  try {
    if (action === 'confirm') await purchaseApi.confirm(row.id)
    else if (action === 'ship') await purchaseApi.ship(row.id)
    else if (action === 'receive') await purchaseApi.receive(row.id)
    else await purchaseApi.cancel(row.id)
    ElMessage.success(tip)
    load()
    if (detail.value?.order.id === row.id) {
      detail.value = await purchaseApi.detail(row.id)
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
  <div class="purchases-page">
    <el-card shadow="never">
      <div class="page-toolbar">
        <el-select v-model="query.storeId" placeholder="全部门店" clearable filterable class="toolbar-select" @change="search">
          <el-option v-for="store in stores" :key="store.id" :value="store.id" :label="store.storeName" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable class="toolbar-select" @change="search">
          <el-option v-for="item in STATUS_LIST" :key="item.value" :value="item.value" :label="item.label" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <span class="toolbar-spacer" />
        <el-button v-permission="'purchase:submit'" type="success" @click="openCreate">发起采购单</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="采购单号" min-width="190" show-overflow-tooltip>
          <template #default="{ row }"><span class="mono">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="门店" min-width="130">
          <template #default="{ row }">{{ storeName(row.storeId) }}</template>
        </el-table-column>
        <el-table-column label="计价等级" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.levelCode || '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总额" width="100" align="right">
          <template #default="{ row }"><span class="mono">{{ fen2yuan(row.totalAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="(STATUS_MAP.get(row.status) ?? { tag: 'info' }).tag" size="small">
              {{ STATUS_MAP.get(row.status)?.label ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 1"
              v-permission="'purchase:confirm'"
              link
              type="warning"
              :loading="flowLoading === row.id"
              @click="flow(row, 'confirm', '已确认计价')"
            >确认计价</el-button>
            <el-button
              v-if="row.status === 2"
              v-permission="'purchase:ship'"
              link
              type="primary"
              :loading="flowLoading === row.id"
              @click="flow(row, 'ship', '已发货')"
            >发货</el-button>
            <el-button
              v-if="row.status === 3"
              v-permission="'purchase:receive'"
              link
              type="success"
              :loading="flowLoading === row.id"
              @click="flow(row, 'receive', '已入库')"
            >确认入库</el-button>
            <el-button
              v-if="row.status === 1 || row.status === 2"
              v-permission="'purchase:cancel'"
              link
              type="danger"
              :loading="flowLoading === row.id"
              @click="flow(row, 'cancel', '已取消')"
            >取消</el-button>
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

    <div class="page-note">对应 PRD：F-SUP-03/F-SUP-04｜发起采购按门店等级计价（快照），入库自动增加门店台账（F-INV-02）</div>

    <!-- 新建采购单 -->
    <el-dialog v-model="createVisible" title="发起采购单（按门店加盟商等级计价）" width="720px">
      <el-form label-width="100px">
        <el-form-item label="门店">
          <el-select v-model="createForm.storeId" placeholder="选择门店" filterable class="full">
            <el-option v-for="store in stores" :key="store.id" :value="store.id" :label="store.storeName" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存预警">
          <div class="full">
            <el-alert
              v-if="!createForm.storeId"
              type="info"
              :closable="false"
              show-icon
              title="选择门店后显示该门店库存低预警，可一键带出缺口补货"
            />
            <div v-else v-loading="warningLoading" class="warning-block">
              <template v-if="warnings.length">
                <div class="warning-head">
                  <span>共 {{ warnings.length }} 项原料低于预警阈值</span>
                  <el-button link type="primary" @click="bringAllWarnings">全部带出</el-button>
                </div>
                <el-table :data="warnings" size="small" max-height="220" stripe>
                  <el-table-column prop="materialName" label="原料" min-width="120" show-overflow-tooltip />
                  <el-table-column label="当前库存" width="90" align="right">
                    <template #default="{ row }"><span class="mono">{{ row.stockQty }}</span></template>
                  </el-table-column>
                  <el-table-column label="预警阈值" width="90" align="right">
                    <template #default="{ row }"><span class="mono">{{ row.warnThreshold }}</span></template>
                  </el-table-column>
                  <el-table-column label="缺口" width="80" align="right">
                    <template #default="{ row }"><span class="mono warning-gap">{{ row.gap }}</span></template>
                  </el-table-column>
                  <el-table-column label="操作" width="64">
                    <template #default="{ row }">
                      <el-button link type="primary" @click="bringWarning(row)">带出</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </template>
              <el-alert v-else-if="!warningLoading" type="success" :closable="false" show-icon title="该门店暂无低库存预警" />
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" maxlength="255" />
        </el-form-item>
        <el-form-item label="采购明细">
          <div class="full">
            <div v-for="(item, index) in createForm.items" :key="index" class="item-row">
              <el-select v-model="item.materialId" placeholder="选择原料" filterable class="item-material">
                <el-option
                  v-for="material in materials"
                  :key="material.id"
                  :value="material.id"
                  :label="`${material.materialName}（${material.unit}）`"
                />
              </el-select>
              <el-input-number v-model="item.qty" :min="0.001" :step="1" :precision="3" />
              <el-button link type="danger" :disabled="createForm.items.length <= 1" @click="removeItem(index)">删除</el-button>
            </div>
            <el-button link type="primary" @click="addItem">+ 添加原料</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>

    <!-- 采购单详情 -->
    <el-drawer v-model="drawerVisible" title="采购单详情" size="640px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="采购单号" :span="2"><span class="mono">{{ detail.order.orderNo }}</span></el-descriptions-item>
            <el-descriptions-item label="门店">{{ storeName(detail.order.storeId) }}</el-descriptions-item>
            <el-descriptions-item label="计价等级">{{ detail.order.levelCode || '—' }}</el-descriptions-item>
            <el-descriptions-item label="总额">{{ fen2yuan(detail.order.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="(STATUS_MAP.get(detail.order.status) ?? { tag: 'info' }).tag" size="small">
                {{ STATUS_MAP.get(detail.order.status)?.label ?? detail.order.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(detail.order.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="确认时间">{{ formatDateTime(detail.order.confirmTime) }}</el-descriptions-item>
            <el-descriptions-item label="发货时间">{{ formatDateTime(detail.order.shipTime) }}</el-descriptions-item>
            <el-descriptions-item label="入库时间">{{ formatDateTime(detail.order.receiveTime) }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.order.remark" label="备注" :span="2">{{ detail.order.remark }}</el-descriptions-item>
          </el-descriptions>

          <el-table :data="detail.items" stripe class="items-table">
            <el-table-column label="原料" min-width="140">
              <template #default="{ row }">
                {{ materials.find((m) => m.id === row.materialId)?.materialName ?? `原料${row.materialId}` }}
              </template>
            </el-table-column>
            <el-table-column label="数量" width="90" align="right">
              <template #default="{ row }"><span class="mono">{{ row.qty }}</span></template>
            </el-table-column>
            <el-table-column label="单价" width="100" align="right">
              <template #default="{ row }"><span class="mono">{{ fen2yuan(row.unitPrice) }}</span></template>
            </el-table-column>
            <el-table-column label="小计" width="110" align="right">
              <template #default="{ row }"><span class="mono">{{ fen2yuan(row.amount) }}</span></template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.item-material {
  flex: 1;
}

.items-table {
  margin-top: 16px;
}

.warning-block {
  min-height: 44px;
}

.warning-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
}

.warning-gap {
  color: var(--el-color-danger);
  font-weight: 600;
}
</style>
