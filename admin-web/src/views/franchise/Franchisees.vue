<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { franchiseeApi } from '@/api/franchise'
import type { FraFranchisee } from '@/types/franchise'
import { formatDateTime } from '@/utils/format'

const LEVELS = ['A', 'B', 'C'] as const

// ---------- 列表 ----------
const loading = ref(false)
const rows = ref<FraFranchisee[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

async function load() {
  loading.value = true
  try {
    const result = await franchiseeApi.page({ ...query, keyword: query.keyword || undefined })
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
const editing = ref<FraFranchisee | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  franchiseeCode: '',
  franchiseeName: '',
  contactName: '',
  contactPhone: '',
  levelCode: 'C',
  joinDate: '' as string,
})

const rules: FormRules = {
  franchiseeCode: [{ required: true, message: '请输入加盟商编码', trigger: 'blur' }],
  franchiseeName: [{ required: true, message: '请输入加盟商名称', trigger: 'blur' }],
}

function openCreate() {
  editing.value = null
  Object.assign(form, { franchiseeCode: '', franchiseeName: '', contactName: '', contactPhone: '', levelCode: 'C', joinDate: '' })
  dialogVisible.value = true
}

function openEdit(row: FraFranchisee) {
  editing.value = row
  Object.assign(form, {
    franchiseeCode: row.franchiseeCode,
    franchiseeName: row.franchiseeName,
    contactName: row.contactName ?? '',
    contactPhone: row.contactPhone ?? '',
    levelCode: row.levelCode,
    joinDate: row.joinDate ?? '',
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  if (editing.value) {
    await franchiseeApi.update(editing.value.id, {
      franchiseeName: form.franchiseeName,
      contactName: form.contactName || undefined,
      contactPhone: form.contactPhone || undefined,
      joinDate: form.joinDate || undefined,
    })
    ElMessage.success('已保存（等级调整请用「调级」）')
  } else {
    await franchiseeApi.create({
      franchiseeCode: form.franchiseeCode,
      franchiseeName: form.franchiseeName,
      contactName: form.contactName || undefined,
      contactPhone: form.contactPhone || undefined,
      levelCode: form.levelCode,
      joinDate: form.joinDate || undefined,
    })
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  load()
}

// ---------- 调级（采购计价联动） ----------
const levelVisible = ref(false)
const levelTarget = ref<FraFranchisee | null>(null)
const levelForm = reactive({ levelCode: 'C', remark: '' })

function openLevel(row: FraFranchisee) {
  levelTarget.value = row
  Object.assign(levelForm, { levelCode: row.levelCode, remark: '' })
  levelVisible.value = true
}

async function submitLevel() {
  if (!levelTarget.value) return
  await franchiseeApi.level(levelTarget.value.id, {
    levelCode: levelForm.levelCode,
    remark: levelForm.remark || undefined,
  })
  ElMessage.success(`已调整为 ${levelForm.levelCode} 级（后续采购按新等级计价）`)
  levelVisible.value = false
  load()
}

// ---------- 开账号 ----------
const accountVisible = ref(false)
const accountTarget = ref<FraFranchisee | null>(null)
const accountForm = reactive({ username: '', nickname: '', password: '' })

function openAccount(row: FraFranchisee) {
  accountTarget.value = row
  Object.assign(accountForm, { username: '', nickname: row.franchiseeName, password: '' })
  accountVisible.value = true
}

async function submitAccount() {
  if (!accountTarget.value) return
  if (!accountForm.username) {
    ElMessage.warning('请输入登录账号')
    return
  }
  const userId = await franchiseeApi.account(accountTarget.value.id, {
    username: accountForm.username,
    nickname: accountForm.nickname || undefined,
    password: accountForm.password || undefined,
  })
  ElMessage.success(`账号已开通（userId=${userId}，未填密码则使用系统默认密码）`)
  accountVisible.value = false
}

// ---------- 合作状态 ----------
async function toggleStatus(row: FraFranchisee) {
  const next = row.status === 1 ? 0 : 1
  await franchiseeApi.status(row.id, { status: next })
  ElMessage.success(next === 1 ? '已恢复合作' : '已终止合作')
  load()
}

onMounted(load)
</script>

<template>
  <div class="franchisees-page">
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
        <el-button type="success" @click="openCreate">新增加盟商</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="franchiseeCode" label="编码" min-width="110" />
        <el-table-column prop="franchiseeName" label="加盟商" min-width="150" />
        <el-table-column label="联系人" width="100">
          <template #default="{ row }">{{ row.contactName || '—' }}</template>
        </el-table-column>
        <el-table-column label="联系电话" width="130">
          <template #default="{ row }">{{ row.contactPhone || '—' }}</template>
        </el-table-column>
        <el-table-column label="等级" width="80">
          <template #default="{ row }">
            <el-tag :type="row.levelCode === 'A' ? 'danger' : row.levelCode === 'B' ? 'warning' : 'info'" size="small">
              {{ row.levelCode }} 级
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="joinDate" label="加入日期" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '合作中' : '已终止' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="openLevel(row)">调级</el-button>
            <el-button link type="primary" @click="openAccount(row)">开账号</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '终止' : '恢复' }}
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

    <!-- 新增/编辑 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? `编辑加盟商：${editing.franchiseeName}` : '新增加盟商'"
      width="480px"
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="编码" prop="franchiseeCode">
          <el-input v-model="form.franchiseeCode" :disabled="!!editing" maxlength="32" />
        </el-form-item>
        <el-form-item label="名称" prop="franchiseeName">
          <el-input v-model="form.franchiseeName" maxlength="64" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" maxlength="32" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" maxlength="20" />
        </el-form-item>
        <el-form-item v-if="!editing" label="等级">
          <el-radio-group v-model="form.levelCode">
            <el-radio v-for="level in LEVELS" :key="level" :value="level">{{ level }} 级</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="加入日期">
          <el-date-picker v-model="form.joinDate" type="date" value-format="YYYY-MM-DD" class="full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 调级 -->
    <el-dialog v-model="levelVisible" :title="`调整等级：${levelTarget?.franchiseeName ?? ''}`" width="440px">
      <el-form label-width="90px">
        <el-form-item label="当前等级">
          <el-tag>{{ levelTarget?.levelCode }} 级</el-tag>
        </el-form-item>
        <el-form-item label="调整为">
          <el-radio-group v-model="levelForm.levelCode">
            <el-radio v-for="level in LEVELS" :key="level" :value="level">{{ level }} 级</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="levelForm.remark" type="textarea" maxlength="255" placeholder="留痕于操作日志" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLevel">确定</el-button>
      </template>
    </el-dialog>

    <!-- 开账号 -->
    <el-dialog v-model="accountVisible" :title="`开通账号：${accountTarget?.franchiseeName ?? ''}`" width="440px">
      <el-form label-width="90px">
        <el-form-item label="登录账号">
          <el-input v-model="accountForm.username" maxlength="32" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="accountForm.nickname" maxlength="64" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="accountForm.password" type="password" maxlength="32" placeholder="留空使用系统默认密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAccount">确定</el-button>
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
