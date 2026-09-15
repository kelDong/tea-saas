<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { materialApi } from '@/api/bom'
import type { BomMaterial } from '@/types/bom'
import { fen2yuan, formatDateTime } from '@/utils/format'

// ---------- 列表 ----------
const loading = ref(false)
const rows = ref<BomMaterial[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

async function load() {
  loading.value = true
  try {
    const result = await materialApi.page({ ...query, keyword: query.keyword || undefined })
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

// ---------- 新增/编辑 ----------
const dialogVisible = ref(false)
const editing = ref<BomMaterial | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  materialCode: '',
  materialName: '',
  category: '',
  unit: '',
  warnThreshold: undefined as number | undefined,
  costPrice: undefined as number | undefined,
})

const rules: FormRules = {
  materialCode: [{ required: true, message: '请输入原料编码', trigger: 'blur' }],
  materialName: [{ required: true, message: '请输入原料名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  costPrice: [{ required: true, message: '请输入参考成本（分）', trigger: 'blur' }],
}

function openCreate() {
  editing.value = null
  Object.assign(form, { materialCode: '', materialName: '', category: '', unit: '', warnThreshold: undefined, costPrice: undefined })
  dialogVisible.value = true
}

function openEdit(row: BomMaterial) {
  editing.value = row
  Object.assign(form, {
    materialCode: row.materialCode,
    materialName: row.materialName,
    category: row.category ?? '',
    unit: row.unit,
    warnThreshold: row.warnThreshold ?? undefined,
    costPrice: row.costPrice,
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  const payload = {
    materialCode: form.materialCode,
    materialName: form.materialName,
    category: form.category || undefined,
    unit: form.unit,
    warnThreshold: form.warnThreshold,
    costPrice: form.costPrice!,
  }
  if (editing.value) {
    await materialApi.update(editing.value.id, payload)
    ElMessage.success('已保存')
  } else {
    await materialApi.create(payload)
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: BomMaterial) {
  const next = row.status === 1 ? 0 : 1
  await materialApi.updateStatus(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已停用')
  load()
}

onMounted(load)
</script>

<template>
  <div class="materials-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="编码/名称"
          clearable
          class="toolbar-input"
          @keyup.enter="search"
          @clear="search"
        />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button type="success" v-permission="'bom:add'" @click="openCreate">新增原料</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="materialCode" label="编码" min-width="120" />
        <el-table-column prop="materialName" label="名称" min-width="140" />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">{{ row.category || '—' }}</template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column label="参考成本" width="100" align="right">
          <template #default="{ row }">{{ fen2yuan(row.costPrice) }}</template>
        </el-table-column>
        <el-table-column label="预警阈值" width="100" align="right">
          <template #default="{ row }">{{ row.warnThreshold ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'bom:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'bom:status'" link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
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

    <el-dialog
      v-model="dialogVisible"
      :title="editing ? `编辑原料：${editing.materialName}` : '新增原料'"
      width="480px"
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="原料编码" prop="materialCode">
          <el-input v-model="form.materialCode" :disabled="!!editing" maxlength="32" />
        </el-form-item>
        <el-form-item label="原料名称" prop="materialName">
          <el-input v-model="form.materialName" maxlength="64" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" maxlength="32" placeholder="如 茶底/水果/小料" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" maxlength="8" placeholder="g/kg/个/杯/L" />
        </el-form-item>
        <el-form-item label="参考成本(分)" prop="costPrice">
          <el-input-number v-model="form.costPrice" :min="0" :step="100" class="full" />
        </el-form-item>
        <el-form-item label="预警阈值">
          <el-input-number v-model="form.warnThreshold" :min="0" :step="10" class="full" placeholder="低于此值预警" />
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

.toolbar-input {
  width: 220px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.full {
  width: 100%;
}
</style>
