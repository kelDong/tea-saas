<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { materialApi } from '@/api/bom'
import { tierPriceApi } from '@/api/supply'
import type { BomMaterial } from '@/types/bom'
import type { SupTierPrice } from '@/types/supply'
import { fen2yuan, formatDateTime } from '@/utils/format'

const LEVELS = ['A', 'B', 'C'] as const

// ---------- 原料映射（列表无 join，前端补名称） ----------
const materials = ref<BomMaterial[]>([])
const materialMap = computed(() => new Map(materials.value.map((item) => [item.id, item])))

async function loadMaterials() {
  materials.value = (await materialApi.page({ page: 1, size: 100 })).list
}

function materialName(id: number): string {
  return materialMap.value.get(id)?.materialName ?? `原料${id}`
}

// ---------- 列表 ----------
const loading = ref(false)
const rows = ref<SupTierPrice[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  materialId: undefined as number | undefined,
  level: undefined as string | undefined,
})

async function load() {
  loading.value = true
  try {
    const result = await tierPriceApi.page({ ...query, materialId: query.materialId, level: query.level })
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

// ---------- 批量定价（选原料 × A/B/C） ----------
const dialogVisible = ref(false)
const priceForm = reactive({
  materialId: undefined as number | undefined,
  priceA: undefined as number | undefined,
  priceB: undefined as number | undefined,
  priceC: undefined as number | undefined,
})

function openDialog() {
  Object.assign(priceForm, { materialId: undefined, priceA: undefined, priceB: undefined, priceC: undefined })
  dialogVisible.value = true
}

async function submit() {
  if (!priceForm.materialId) {
    ElMessage.warning('请选择原料')
    return
  }
  const items = [
    { levelCode: 'A', price: priceForm.priceA },
    { levelCode: 'B', price: priceForm.priceB },
    { levelCode: 'C', price: priceForm.priceC },
  ].filter((item): item is { levelCode: string; price: number } => item.price != null)
  if (!items.length) {
    ElMessage.warning('请至少填写一档价格')
    return
  }
  await tierPriceApi.batchSave({
    items: items.map((item) => ({ materialId: priceForm.materialId!, levelCode: item.levelCode, price: item.price })),
  })
  ElMessage.success('已保存')
  dialogVisible.value = false
  load()
}

onMounted(() => {
  loadMaterials()
  load()
})
</script>

<template>
  <div class="tier-prices-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.materialId" placeholder="全部原料" clearable filterable class="toolbar-select" @change="search">
          <el-option v-for="item in materials" :key="item.id" :value="item.id" :label="item.materialName" />
        </el-select>
        <el-select v-model="query.level" placeholder="全部等级" clearable class="toolbar-level" @change="search">
          <el-option v-for="level in LEVELS" :key="level" :value="level" :label="`${level} 级`" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button type="success" @click="openDialog">批量定价</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="原料" min-width="160">
          <template #default="{ row }">{{ materialName(row.materialId) }}</template>
        </el-table-column>
        <el-table-column label="计价等级" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.levelCode }} 级</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="110" align="right">
          <template #default="{ row }">{{ fen2yuan(row.price) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
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

    <!-- 批量定价 -->
    <el-dialog v-model="dialogVisible" title="批量定价（按原料设置 A/B/C 三档）" width="460px">
      <el-form label-width="120px">
        <el-form-item label="原料">
          <el-select v-model="priceForm.materialId" placeholder="选择原料" filterable class="full">
            <el-option
              v-for="item in materials"
              :key="item.id"
              :value="item.id"
              :label="`${item.materialName}（${item.unit}）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-for="level in LEVELS" :key="level" :label="`${level} 级单价(分)`">
          <el-input-number
            v-model="priceForm[`price${level}` as 'priceA' | 'priceB' | 'priceC']"
            :min="1"
            :step="100"
            class="full"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-select {
  width: 200px;
}

.toolbar-level {
  width: 120px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.full {
  width: 100%;
}
</style>
