<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { materialApi } from '@/api/bom'
import { supplierApi } from '@/api/supply'
import type { BomMaterial } from '@/types/bom'
import type { SupSupplier } from '@/types/supply'
import { formatDateTime } from '@/utils/format'

// ---------- 列表 ----------
const loading = ref(false)
const rows = ref<SupSupplier[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

async function load() {
  loading.value = true
  try {
    const result = await supplierApi.page({ ...query, keyword: query.keyword || undefined })
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

// ---------- 新增/编辑 ----------
const dialogVisible = ref(false)
const editing = ref<SupSupplier | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  supplierCode: '',
  supplierName: '',
  contactName: '',
  contactPhone: '',
  paymentTerms: '',
})

const rules: FormRules = {
  supplierCode: [{ required: true, message: '请输入供应商编码', trigger: 'blur' }],
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
}

function openCreate() {
  editing.value = null
  Object.assign(form, { supplierCode: '', supplierName: '', contactName: '', contactPhone: '', paymentTerms: '' })
  dialogVisible.value = true
}

function openEdit(row: SupSupplier) {
  editing.value = row
  Object.assign(form, {
    supplierCode: row.supplierCode,
    supplierName: row.supplierName,
    contactName: row.contactName ?? '',
    contactPhone: row.contactPhone ?? '',
    paymentTerms: row.paymentTerms ?? '',
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  const payload = {
    supplierCode: form.supplierCode,
    supplierName: form.supplierName,
    contactName: form.contactName || undefined,
    contactPhone: form.contactPhone || undefined,
    paymentTerms: form.paymentTerms || undefined,
  }
  if (editing.value) {
    await supplierApi.update(editing.value.id, payload)
    ElMessage.success('已保存')
  } else {
    await supplierApi.create(payload)
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: SupSupplier) {
  const next = row.status === 1 ? 0 : 1
  await supplierApi.updateStatus(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已停用')
  load()
}

// ---------- 供货原料绑定（全量重绑） ----------
const bindVisible = ref(false)
const bindTarget = ref<SupSupplier | null>(null)
const bindLoading = ref(false)
const materials = ref<BomMaterial[]>([])
const bindRows = ref<Array<{ materialId: number | undefined; supplyPrice: number | undefined }>>([])

async function openBind(row: SupSupplier) {
  bindTarget.value = row
  bindVisible.value = true
  bindLoading.value = true
  try {
    if (!materials.value.length) {
      materials.value = (await materialApi.page({ page: 1, size: 100 })).list
    }
    const bound = await supplierApi.materials(row.id)
    bindRows.value = bound.map((item) => ({ materialId: item.materialId, supplyPrice: item.supplyPrice }))
    if (!bindRows.value.length) {
      bindRows.value = [{ materialId: undefined, supplyPrice: undefined }]
    }
  } finally {
    bindLoading.value = false
  }
}

function addBindRow() {
  bindRows.value.push({ materialId: undefined, supplyPrice: undefined })
}

function removeBindRow(index: number) {
  bindRows.value.splice(index, 1)
}

async function submitBind() {
  if (!bindTarget.value) return
  const items = bindRows.value.filter((row) => row.materialId != null && row.supplyPrice != null)
  if (!items.length) {
    ElMessage.warning('请至少填写一行完整的供货记录')
    return
  }
  await supplierApi.bindMaterials(bindTarget.value.id, {
    items: items.map((row) => ({ materialId: row.materialId!, supplyPrice: row.supplyPrice! })),
  })
  ElMessage.success('已保存（全量重绑）')
  bindVisible.value = false
}

onMounted(load)
</script>

<template>
  <div class="suppliers-page">
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
        <el-button type="success" @click="openCreate">新增供应商</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="supplierCode" label="编码" min-width="110" />
        <el-table-column prop="supplierName" label="供应商" min-width="150" />
        <el-table-column label="联系人" width="100">
          <template #default="{ row }">{{ row.contactName || '—' }}</template>
        </el-table-column>
        <el-table-column label="联系电话" width="130">
          <template #default="{ row }">{{ row.contactPhone || '—' }}</template>
        </el-table-column>
        <el-table-column label="账期" width="120">
          <template #default="{ row }">{{ row.paymentTerms || '—' }}</template>
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
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="openBind(row)">供货原料</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
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

    <!-- 新增/编辑供应商 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? `编辑供应商：${editing.supplierName}` : '新增供应商'"
      width="480px"
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="编码" prop="supplierCode">
          <el-input v-model="form.supplierCode" :disabled="!!editing" maxlength="32" />
        </el-form-item>
        <el-form-item label="名称" prop="supplierName">
          <el-input v-model="form.supplierName" maxlength="64" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" maxlength="32" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" maxlength="20" />
        </el-form-item>
        <el-form-item label="账期">
          <el-input v-model="form.paymentTerms" maxlength="64" placeholder="如 月结30天" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 供货原料绑定 -->
    <el-dialog v-model="bindVisible" :title="`供货原料：${bindTarget?.supplierName ?? ''}`" width="560px">
      <div v-loading="bindLoading">
        <div v-for="(row, index) in bindRows" :key="index" class="bind-row">
          <el-select v-model="row.materialId" placeholder="选择原料" filterable class="bind-material">
            <el-option
              v-for="material in materials"
              :key="material.id"
              :value="material.id"
              :label="`${material.materialName}（${material.unit}）`"
            />
          </el-select>
          <el-input-number v-model="row.supplyPrice" :min="1" :step="100" placeholder="供货价(分)" />
          <el-button link type="danger" :disabled="bindRows.length <= 1" @click="removeBindRow(index)">删除</el-button>
        </div>
        <el-button link type="primary" @click="addBindRow">+ 添加原料</el-button>
      </div>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBind">确定</el-button>
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

.bind-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.bind-material {
  flex: 1;
}
</style>
