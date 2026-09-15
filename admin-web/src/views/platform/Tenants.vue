<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { packageApi, tenantApi } from '@/api/platform'
import type { SysPackageVO, TenantVO } from '@/types/platform'
import { formatDateTime } from '@/utils/format'

// ---------- 套餐下拉（新建/编辑选择 packageCode） ----------
const packages = ref<SysPackageVO[]>([])

// ---------- 列表 ----------
const loading = ref(false)
const rows = ref<TenantVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

async function load() {
  loading.value = true
  try {
    const result = await tenantApi.page({ ...query, keyword: query.keyword || undefined })
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

// ---------- 新建/编辑 ----------
const dialogVisible = ref(false)
const editing = ref<TenantVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  tenantCode: '',
  tenantName: '',
  contactName: '',
  contactPhone: '',
  packageCode: '',
  storeLimit: 0,
  expireTime: '',
})

const rules: FormRules = {
  tenantCode: [
    { required: true, message: '请输入品牌编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母/数字/下划线/中划线', trigger: 'blur' },
  ],
  tenantName: [{ required: true, message: '请输入品牌名称', trigger: 'blur' }],
}

function openCreate() {
  editing.value = null
  Object.assign(form, { tenantCode: '', tenantName: '', contactName: '', contactPhone: '', packageCode: '', storeLimit: 0, expireTime: '' })
  dialogVisible.value = true
}

function openEdit(row: TenantVO) {
  editing.value = row
  Object.assign(form, {
    tenantCode: row.tenantCode,
    tenantName: row.tenantName,
    contactName: row.contactName ?? '',
    contactPhone: row.contactPhone ?? '',
    packageCode: row.packageCode ?? '',
    storeLimit: row.storeLimit,
    expireTime: row.expireTime ?? '',
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  const payload = {
    tenantName: form.tenantName,
    contactName: form.contactName || undefined,
    contactPhone: form.contactPhone || undefined,
    packageCode: form.packageCode || undefined,
    storeLimit: form.storeLimit,
    expireTime: form.expireTime || undefined,
  }
  if (editing.value) {
    await tenantApi.update(editing.value.id, payload)
    ElMessage.success('已保存（编码不可改）')
  } else {
    await tenantApi.create({ ...payload, tenantCode: form.tenantCode })
    ElMessage.success('品牌已创建（默认启用）')
  }
  dialogVisible.value = false
  load()
}

// ---------- 启停 ----------
const statusLoading = ref<number | null>(null)

async function toggleStatus(row: TenantVO) {
  statusLoading.value = row.id
  try {
    const next = row.status === 1 ? 0 : 1
    await tenantApi.updateStatus(row.id, next)
    ElMessage.success(next === 1 ? '已启用' : '已停用（存量会话不强制下线，新登录将被拦截）')
    load()
  } finally {
    statusLoading.value = null
  }
}

onMounted(async () => {
  load()
  packages.value = await packageApi.list()
})
</script>

<template>
  <div class="tenants-page">
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
        <el-button type="success" @click="openCreate">新增品牌</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="tenantCode" label="品牌编码" min-width="120" />
        <el-table-column prop="tenantName" label="品牌名称" min-width="150" />
        <el-table-column label="联系人" width="100">
          <template #default="{ row }">{{ row.contactName || '—' }}</template>
        </el-table-column>
        <el-table-column label="联系电话" width="130">
          <template #default="{ row }">{{ row.contactPhone || '—' }}</template>
        </el-table-column>
        <el-table-column label="套餐" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.packageCode" size="small">{{ row.packageCode }}</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="门店上限" width="90" align="right">
          <template #default="{ row }">{{ row.storeLimit === 0 ? '不限' : row.storeLimit }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="到期时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.expireTime) || '—' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              :loading="statusLoading === row.id"
              @click="toggleStatus(row)"
            >{{ row.status === 1 ? '停用' : '启用' }}</el-button>
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

    <!-- 新增/编辑品牌 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? `编辑品牌：${editing.tenantName}` : '新增品牌'"
      width="520px"
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="品牌编码" prop="tenantCode">
          <el-input v-model="form.tenantCode" :disabled="!!editing" maxlength="32" placeholder="字母/数字/下划线/中划线" />
        </el-form-item>
        <el-form-item label="品牌名称" prop="tenantName">
          <el-input v-model="form.tenantName" maxlength="64" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" maxlength="32" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" maxlength="20" />
        </el-form-item>
        <el-form-item label="套餐">
          <el-select v-model="form.packageCode" placeholder="未选套餐" clearable class="full">
            <el-option
              v-for="pkg in packages"
              :key="pkg.id"
              :value="pkg.packageCode"
              :label="`${pkg.packageName}（${pkg.packageCode}）`"
              :disabled="pkg.status !== 1"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="门店上限">
          <el-input-number v-model="form.storeLimit" :min="0" />
          <span class="tip">0 = 不限</span>
        </el-form-item>
        <el-form-item label="到期时间">
          <el-date-picker v-model="form.expireTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="full" />
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

.tip {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
