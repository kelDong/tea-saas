<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { formulaApi, materialApi } from '@/api/bom'
import type { BomFormula, BomMaterial, FormulaItemReq, ProfitCalcVO } from '@/types/bom'
import { fen2yuan, formatDateTime } from '@/utils/format'

const STATUS_MAP: Record<number, { label: string; tag: 'info' | 'success' | 'primary' }> = {
  0: { label: '停用', tag: 'info' },
  1: { label: '正常', tag: 'success' },
  2: { label: '已发布', tag: 'primary' },
}

// ---------- 列表 ----------
const loading = ref(false)
const rows = ref<BomFormula[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

async function load() {
  loading.value = true
  try {
    const result = await formulaApi.page({ ...query, keyword: query.keyword || undefined })
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

// ---------- 原料下拉 ----------
const materials = ref<BomMaterial[]>([])

async function loadMaterials() {
  const result = await materialApi.page({ page: 1, size: 100 })
  materials.value = result.list
}

// ---------- 新增/编辑（明细行动态编辑） ----------
const dialogVisible = ref(false)
const editing = ref<BomFormula | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  formulaCode: '',
  formulaName: '',
  items: [] as FormulaItemReq[],
})

const rules: FormRules = {
  formulaCode: [{ required: true, message: '请输入配方编码', trigger: 'blur' }],
  formulaName: [{ required: true, message: '请输入配方名称', trigger: 'blur' }],
}

function openCreate() {
  editing.value = null
  Object.assign(form, { formulaCode: '', formulaName: '', items: [{ materialId: undefined as unknown as number, qty: 1, sort: 1 }] })
  dialogVisible.value = true
}

async function openEdit(row: BomFormula) {
  const detail = await formulaApi.detail(row.id)
  editing.value = row
  Object.assign(form, {
    formulaCode: detail.formulaCode,
    formulaName: detail.formulaName,
    items: detail.items
      .slice()
      .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
      .map((item) => ({ materialId: item.materialId, qty: item.qty, sort: item.sort })),
  })
  dialogVisible.value = true
}

function addItem() {
  form.items.push({ materialId: undefined as unknown as number, qty: 1, sort: form.items.length + 1 })
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

async function submit() {
  await formRef.value?.validate()
  if (!form.items.length || form.items.some((item) => !item.materialId)) {
    ElMessage.warning('请至少填写一行完整的配方明细')
    return
  }
  const payload = { formulaCode: form.formulaCode, formulaName: form.formulaName, items: form.items }
  if (editing.value) {
    await formulaApi.update(editing.value.id, payload)
    ElMessage.success('已保存（版本+1）')
  } else {
    await formulaApi.create(payload)
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  load()
}

async function copy(row: BomFormula) {
  await formulaApi.copy(row.id)
  ElMessage.success('已复制')
  load()
}

async function toggleStatus(row: BomFormula) {
  // 仅停用态可重新启用；已发布态先停用走启用/停用开关
  const next = row.status === 0 ? 1 : 0
  await formulaApi.updateStatus(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已停用')
  load()
}

async function publish(row: BomFormula) {
  await formulaApi.publish(row.id)
  ElMessage.success('已发布')
  load()
}

// ---------- 毛利试算 ----------
const calcVisible = ref(false)
const calcTarget = ref<BomFormula | null>(null)
const calcPriceYuan = ref<number | undefined>(undefined)
const calcResult = ref<ProfitCalcVO | null>(null)
const calcLoading = ref(false)

function openCalc(row: BomFormula) {
  calcTarget.value = row
  calcPriceYuan.value = undefined
  calcResult.value = null
  calcVisible.value = true
}

async function submitCalc() {
  if (!calcTarget.value || calcPriceYuan.value == null) {
    ElMessage.warning('请输入建议售价')
    return
  }
  calcLoading.value = true
  try {
    calcResult.value = await formulaApi.calcProfit({
      formulaId: calcTarget.value.id,
      suggestPrice: Math.round(calcPriceYuan.value * 100),
    })
  } finally {
    calcLoading.value = false
  }
}

onMounted(() => {
  load()
  loadMaterials()
})
</script>

<template>
  <div class="formulas-page">
    <el-card shadow="never">
      <div class="page-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="编码/名称"
          clearable
          class="toolbar-input"
          @keyup.enter="search"
          @clear="search"
        />
        <el-button type="primary" @click="search">查询</el-button>
        <span class="toolbar-spacer" />
        <el-button v-permission="'bom:add'" type="success" @click="openCreate">新增配方</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="编码" min-width="120">
          <template #default="{ row }">
            <span class="mono">{{ row.formulaCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="formulaName" label="名称" min-width="150" />
        <el-table-column label="版本" width="70" align="right">
          <template #default="{ row }">
            <span class="mono">{{ row.version }}</span>
          </template>
        </el-table-column>
        <el-table-column label="试算成本" width="100" align="right">
          <template #default="{ row }">
            <span class="mono">{{ fen2yuan(row.costTotal) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="(STATUS_MAP[row.status] ?? { tag: 'info' }).tag" size="small">
              {{ STATUS_MAP[row.status]?.label ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'bom:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'bom:calc'" link type="warning" @click="openCalc(row)">毛利试算</el-button>
            <el-button v-permission="'bom:edit'" link @click="copy(row)">复制</el-button>
            <el-button v-if="row.status === 1" v-permission="'bom:status'" link type="success" @click="publish(row)">发布</el-button>
            <el-button
              v-else
              v-permission="'bom:status'"
              link
              :type="row.status === 0 ? 'success' : 'danger'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 0 ? '启用' : '停用' }}
            </el-button>
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

    <div class="page-note">对应 PRD：F-BOM-02/F-BOM-03</div>

    <!-- 新增/编辑配方（明细全量重绑） -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? `编辑配方：${editing.formulaName}` : '新增配方'"
      width="560px"
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="配方编码" prop="formulaCode">
          <el-input v-model="form.formulaCode" :disabled="!!editing" maxlength="32" />
        </el-form-item>
        <el-form-item label="配方名称" prop="formulaName">
          <el-input v-model="form.formulaName" maxlength="64" />
        </el-form-item>
        <el-form-item label="配方明细">
          <div class="full">
            <div v-for="(item, index) in form.items" :key="index" class="item-row">
              <el-select v-model="item.materialId" placeholder="选择原料" filterable class="item-material">
                <el-option
                  v-for="material in materials"
                  :key="material.id"
                  :value="material.id"
                  :label="`${material.materialName}（${material.unit}）`"
                  :disabled="material.status !== 1"
                />
              </el-select>
              <el-input-number v-model="item.qty" :min="0.001" :step="1" :precision="3" controls-position="right" />
              <el-button link type="danger" :disabled="form.items.length <= 1" @click="removeItem(index)">删除</el-button>
            </div>
            <el-button link type="primary" @click="addItem">+ 添加原料</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 毛利试算 -->
    <el-dialog v-model="calcVisible" :title="`毛利试算：${calcTarget?.formulaName ?? ''}`" width="480px">
      <el-form label-width="100px">
        <el-form-item label="建议售价(元)">
          <el-input-number v-model="calcPriceYuan" :min="0" :step="1" :precision="2" class="full" />
          <span class="tip">元</span>
        </el-form-item>
      </el-form>
      <el-descriptions v-if="calcResult" :column="1" border class="calc-result">
        <el-descriptions-item label="成本合计">
          <span class="mono">{{ fen2yuan(calcResult.costTotal) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="建议售价">
          <span class="mono">{{ fen2yuan(calcResult.suggestPrice) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="毛利">
          <span class="mono">{{ fen2yuan(calcResult.grossProfit) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="毛利率">
          <span class="mono">{{ calcResult.grossMargin }}%</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="calcVisible = false">关闭</el-button>
        <el-button type="primary" :loading="calcLoading" @click="submitCalc">试算</el-button>
      </template>
    </el-dialog>
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

.calc-result {
  margin-top: 12px;
}
</style>
