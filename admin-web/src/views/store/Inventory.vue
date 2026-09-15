<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import { materialApi } from '@/api/bom'
import { franchiseApi } from '@/api/franchise'
import { inventoryApi } from '@/api/inventory'
import type { BomMaterial } from '@/types/bom'
import type { InvStockLog, InvStoreMaterialStock, StockWarningVO } from '@/types/inventory'
import { STOCK_BIZ_TYPE_MAP } from '@/types/inventory'
import { formatDateTime } from '@/utils/format'

const stores = ref<Array<{ id: number; storeName: string }>>([])
const storeId = ref<number | undefined>(undefined)
const materials = ref<BomMaterial[]>([])
const materialMap = computed(() => new Map(materials.value.map((item) => [item.id, item.materialName])))

function materialName(id: number): string {
  return materialMap.value.get(id) ?? `原料${id}`
}

// ---------- 台账 ----------
const loading = ref(false)
const rows = ref<InvStoreMaterialStock[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  materialId: undefined as number | undefined,
})

async function load() {
  if (!storeId.value) return
  loading.value = true
  try {
    const result = await inventoryApi.page({ ...query, storeId: storeId.value })
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

// ---------- 流水 ----------
const logsVisible = ref(false)
const logsLoading = ref(false)
const logs = ref<InvStockLog[]>([])
const logsTotal = ref(0)
const logsQuery = reactive({ page: 1, size: 10 })
const logsTarget = ref<{ storeId: number; materialId: number } | null>(null)

async function openLogs(row: InvStoreMaterialStock) {
  logsTarget.value = { storeId: row.storeId, materialId: row.materialId }
  logsQuery.page = 1
  logsVisible.value = true
  await loadLogs()
}

async function loadLogs() {
  if (!logsTarget.value) return
  logsLoading.value = true
  try {
    const result = await inventoryApi.logs(logsTarget.value.storeId, logsTarget.value.materialId, logsQuery)
    logs.value = result.list
    logsTotal.value = result.total
  } finally {
    logsLoading.value = false
  }
}

// ---------- 盘点修正（R6/R7，R8 无此权限） ----------
const adjustVisible = ref(false)
const adjustLoading = ref(false)
const adjustForm = reactive({
  materialId: undefined as number | undefined,
  actualQty: undefined as number | undefined,
  remark: '',
})

function openAdjust(row: InvStoreMaterialStock) {
  Object.assign(adjustForm, { materialId: row.materialId, actualQty: row.stockQty, remark: '' })
  adjustVisible.value = true
}

async function submitAdjust() {
  if (!storeId.value || !adjustForm.materialId || adjustForm.actualQty == null) {
    ElMessage.warning('请补全原料与实盘数量')
    return
  }
  adjustLoading.value = true
  try {
    await inventoryApi.adjust({
      storeId: storeId.value,
      materialId: adjustForm.materialId,
      actualQty: adjustForm.actualQty,
      remark: adjustForm.remark || undefined,
    })
    ElMessage.success('盘点已生效（version+1，流水留痕）')
    adjustVisible.value = false
    load()
  } finally {
    adjustLoading.value = false
  }
}

// ---------- 预警 ----------
const warningsLoading = ref(false)
const warnings = ref<StockWarningVO[]>([])

async function loadWarnings() {
  if (!storeId.value) return
  warningsLoading.value = true
  try {
    warnings.value = await inventoryApi.warnings(storeId.value)
  } finally {
    warningsLoading.value = false
  }
}

function refresh() {
  load()
  loadWarnings()
}

watch(storeId, refresh)

onMounted(async () => {
  stores.value = (await franchiseApi.storePage({ page: 1, size: 100 })).list
  materials.value = (await materialApi.page({ page: 1, size: 100 })).list
  if (stores.value.length) {
    storeId.value = stores.value[0].id
  }
})
</script>

<template>
  <div class="store-inventory">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="label">门店</span>
        <el-select v-model="storeId" placeholder="选择门店" filterable class="toolbar-select">
          <el-option v-for="store in stores" :key="store.id" :value="store.id" :label="store.storeName" />
        </el-select>
      </div>

      <el-empty v-if="!storeId" description="当前租户暂无门店" />
      <el-tabs v-else @tab-change="(name: string | number) => name === 'warning' && loadWarnings()">
        <el-tab-pane label="库存台账">
          <div class="toolbar">
            <el-select v-model="query.materialId" placeholder="全部原料" clearable filterable class="toolbar-select" @change="search">
              <el-option v-for="material in materials" :key="material.id" :value="material.id" :label="material.materialName" />
            </el-select>
            <el-button type="primary" @click="search">查询</el-button>
          </div>

          <el-table v-loading="loading" :data="rows" stripe>
            <el-table-column label="原料" min-width="150">
              <template #default="{ row }">{{ materialName(row.materialId) }}</template>
            </el-table-column>
            <el-table-column prop="stockQty" label="库存量" width="110" align="right" />
            <el-table-column prop="version" label="版本" width="70" />
            <el-table-column label="更新时间" min-width="160">
              <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button v-permission="'inv:adjust'" link type="warning" @click="openAdjust(row)">盘点</el-button>
                <el-button v-permission="'inv:log'" link type="primary" @click="openLogs(row)">流水</el-button>
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
        </el-tab-pane>

        <el-tab-pane label="库存预警">
          <el-table v-loading="warningsLoading" :data="warnings" stripe>
            <el-table-column prop="materialCode" label="原料编码" width="110" />
            <el-table-column prop="materialName" label="原料" min-width="130" />
            <el-table-column prop="unit" label="单位" width="70" />
            <el-table-column prop="stockQty" label="库存" width="90" align="right" />
            <el-table-column prop="warnThreshold" label="阈值" width="90" align="right" />
            <el-table-column label="缺口" width="90" align="right">
              <template #default="{ row }">
                <span class="gap">{{ row.gap }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 盘点修正 -->
    <el-dialog v-model="adjustVisible" title="盘点修正（实盘覆盖 + 流水留痕）" width="440px">
      <el-form label-width="90px">
        <el-form-item label="门店">
          <el-input :value="stores.find((s) => s.id === storeId)?.storeName" disabled />
        </el-form-item>
        <el-form-item label="原料">
          <el-select v-model="adjustForm.materialId" placeholder="选择原料" filterable class="full">
            <el-option
              v-for="material in materials"
              :key="material.id"
              :value="material.id"
              :label="`${material.materialName}（${material.unit}）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="实盘数量">
          <el-input-number v-model="adjustForm.actualQty" :min="0" :step="10" :precision="3" class="full" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="adjustForm.remark" maxlength="128" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustLoading" @click="submitAdjust">确定</el-button>
      </template>
    </el-dialog>

    <!-- 库存流水 -->
    <el-drawer v-model="logsVisible" title="库存流水" size="620px">
      <el-table v-loading="logsLoading" :data="logs" stripe>
        <el-table-column label="时间" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ STOCK_BIZ_TYPE_MAP[row.bizType] ?? row.bizType }}</template>
        </el-table-column>
        <el-table-column label="变动" width="90" align="right">
          <template #default="{ row }">
            <span :class="row.changeQty >= 0 ? 'plus' : 'minus'">{{ row.changeQty >= 0 ? `+${row.changeQty}` : row.changeQty }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="beforeQty" label="前" width="80" align="right" />
        <el-table-column prop="afterQty" label="后" width="80" align="right" />
        <el-table-column prop="bizNo" label="业务单号" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.bizNo || '—' }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="logsQuery.page"
        v-model:page-size="logsQuery.size"
        :total="logsTotal"
        layout="total, prev, pager, next"
        class="pager"
        @current-change="loadLogs"
      />
    </el-drawer>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar .label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.toolbar-select {
  width: 200px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.full {
  width: 100%;
}

.gap {
  color: var(--el-color-danger);
  font-weight: 600;
}

.plus {
  color: var(--el-color-success);
}

.minus {
  color: var(--el-color-danger);
}
</style>
