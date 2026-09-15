<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { franchiseApi, franchiseeApi } from '@/api/franchise'
import type { FraFranchisee, FraStore } from '@/types/franchise'

// ---------- 加盟商下拉（franchiseeId 显示/选择） ----------
const franchisees = ref<FraFranchisee[]>([])
const franchiseeMap = computed(() => new Map(franchisees.value.map((item) => [item.id, item.franchiseeName])))

function franchiseeName(id: number | null): string {
  if (id == null) return '直营'
  return franchiseeMap.value.get(id) ?? `加盟商${id}`
}

// ---------- 列表 ----------
const loading = ref(false)
const rows = ref<FraStore[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

async function load() {
  loading.value = true
  try {
    const result = await franchiseApi.storePage({ ...query, keyword: query.keyword || undefined })
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

// ---------- 营业状态（1营业/0打烊） ----------
const statusLoading = ref<number | null>(null)

async function toggleBusiness(row: FraStore) {
  statusLoading.value = row.id
  try {
    const next = row.businessStatus === 1 ? 0 : 1
    await franchiseApi.storeBusinessStatus(row.id, { businessStatus: next })
    row.businessStatus = next
    ElMessage.success(next === 1 ? '已开店营业' : '已打烊')
  } finally {
    statusLoading.value = null
  }
}

// ---------- 新增/编辑 ----------
const dialogVisible = ref(false)
const editing = ref<FraStore | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  storeCode: '',
  storeName: '',
  franchiseeId: undefined as number | undefined,
  address: '',
  longitude: undefined as number | undefined,
  latitude: undefined as number | undefined,
  openTime: '',
  closeTime: '',
})

const rules: FormRules = {
  storeCode: [{ required: true, message: '请输入门店编码', trigger: 'blur' }],
  storeName: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
}

function openCreate() {
  editing.value = null
  Object.assign(form, {
    storeCode: '',
    storeName: '',
    franchiseeId: undefined,
    address: '',
    longitude: undefined,
    latitude: undefined,
    openTime: '',
    closeTime: '',
  })
  dialogVisible.value = true
}

function openEdit(row: FraStore) {
  editing.value = row
  Object.assign(form, {
    storeCode: row.storeCode,
    storeName: row.storeName,
    franchiseeId: row.franchiseeId ?? undefined,
    address: row.address ?? '',
    longitude: row.longitude ?? undefined,
    latitude: row.latitude ?? undefined,
    openTime: row.openTime ?? '',
    closeTime: row.closeTime ?? '',
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  const payload = {
    storeCode: form.storeCode,
    storeName: form.storeName,
    franchiseeId: form.franchiseeId,
    address: form.address || undefined,
    longitude: form.longitude,
    latitude: form.latitude,
    openTime: form.openTime || undefined,
    closeTime: form.closeTime || undefined,
  }
  if (editing.value) {
    await franchiseApi.storeUpdate(editing.value.id, payload)
    ElMessage.success('已保存')
  } else {
    await franchiseApi.storeCreate(payload)
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  load()
}

onMounted(async () => {
  load()
  franchisees.value = (await franchiseeApi.page({ page: 1, size: 100 })).list
})
</script>

<template>
  <div class="stores-page">
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
        <el-button type="success" @click="openCreate">新增门店</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="storeCode" label="编码" min-width="110" />
        <el-table-column prop="storeName" label="门店" min-width="150" />
        <el-table-column label="归属" width="120">
          <template #default="{ row }">
            <el-tag :type="row.franchiseeId == null ? 'success' : 'primary'" size="small">
              {{ franchiseeName(row.franchiseeId) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="地址" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.address || '—' }}</template>
        </el-table-column>
        <el-table-column label="营业时间" width="130">
          <template #default="{ row }">{{ row.openTime && row.closeTime ? `${row.openTime}~${row.closeTime}` : '—' }}</template>
        </el-table-column>
        <el-table-column label="营业状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.businessStatus === 1"
              :loading="statusLoading === row.id"
              inline-prompt
              active-text="营业"
              inactive-text="打烊"
              @change="toggleBusiness(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <!-- 新增/编辑门店 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? `编辑门店：${editing.storeName}` : '新增门店'"
      width="520px"
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="编码" prop="storeCode">
          <el-input v-model="form.storeCode" :disabled="!!editing" maxlength="32" />
        </el-form-item>
        <el-form-item label="名称" prop="storeName">
          <el-input v-model="form.storeName" maxlength="64" />
        </el-form-item>
        <el-form-item label="归属加盟商">
          <el-select v-model="form.franchiseeId" placeholder="直营（不选）" clearable filterable class="full">
            <el-option v-for="item in franchisees" :key="item.id" :value="item.id" :label="item.franchiseeName" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" maxlength="255" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="form.longitude" :min="-180" :max="180" :precision="6" :step="0.000001" class="full" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number v-model="form.latitude" :min="-90" :max="90" :precision="6" :step="0.000001" class="full" />
        </el-form-item>
        <el-form-item label="营业时间">
          <div class="time-range">
            <el-time-select v-model="form.openTime" start="00:00" step="00:30" end="23:30" placeholder="开店" />
            <span class="sep">至</span>
            <el-time-select v-model="form.closeTime" start="00:00" step="00:30" end="23:30" placeholder="打烊" />
          </div>
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

.time-range {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.sep {
  color: var(--el-text-color-secondary);
}
</style>
