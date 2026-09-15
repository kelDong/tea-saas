<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { franchiseApi } from '@/api/franchise'
import { sysRoleApi, sysUserApi } from '@/api/system'
import type { SysRole, SysUserVO } from '@/types/system'
import type { FraStore } from '@/types/franchise'
import { formatDateTime } from '@/utils/format'

const USER_TYPE_MAP: Record<number, string> = { 0: '平台', 1: '总部', 2: '加盟商', 3: '门店' }

// ---------- 用户列表 ----------
const loading = ref(false)
const rows = ref<SysUserVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

async function loadUsers() {
  loading.value = true
  try {
    const result = await sysUserApi.page({ ...query, keyword: query.keyword || undefined })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  loadUsers()
}

// ---------- 下拉数据（角色/门店） ----------
const roles = ref<SysRole[]>([])
const stores = ref<FraStore[]>([])

async function loadOptions() {
  roles.value = await sysRoleApi.list()
  const storePage = await franchiseApi.storePage({ page: 1, size: 100 })
  stores.value = storePage.records
}

// ---------- 新增/编辑 ----------
const dialogVisible = ref(false)
const editing = ref<SysUserVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  roleIds: [] as number[],
  storeIds: [] as number[],
})

const createRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' },
  ],
  roleIds: [{ required: true, message: '至少分配一个角色', trigger: 'change' }],
}

function openCreate() {
  editing.value = null
  Object.assign(form, { username: '', password: '', nickname: '', phone: '', roleIds: [], storeIds: [] })
  dialogVisible.value = true
}

function openEdit(row: SysUserVO) {
  editing.value = row
  Object.assign(form, {
    username: row.username,
    password: '',
    nickname: row.nickname ?? '',
    phone: row.phone ?? '',
    roleIds: row.roleIds ?? [],
    storeIds: row.storeIds ?? [],
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  if (editing.value) {
    // 后端 update 全量覆盖 nickname/phone，须原值带回
    await sysUserApi.update(editing.value.id, {
      nickname: form.nickname,
      phone: form.phone,
      roleIds: form.roleIds,
      storeIds: form.storeIds,
    })
    ElMessage.success('已保存')
  } else {
    await sysUserApi.create({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      phone: form.phone || undefined,
      roleIds: form.roleIds,
      storeIds: form.storeIds,
    })
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  loadUsers()
}

// ---------- 重置密码 ----------
const resetVisible = ref(false)
const resetTarget = ref<SysUserVO | null>(null)
const newPassword = ref('')

function openReset(row: SysUserVO) {
  resetTarget.value = row
  newPassword.value = ''
  resetVisible.value = true
}

async function submitReset() {
  if (!resetTarget.value) return
  if (newPassword.value.length < 6 || newPassword.value.length > 32) {
    ElMessage.warning('密码长度 6-32 位')
    return
  }
  await sysUserApi.resetPassword(resetTarget.value.id, newPassword.value)
  ElMessage.success('密码已重置，该账号已强制下线')
  resetVisible.value = false
}

onMounted(() => {
  loadUsers()
  loadOptions()
})
</script>

<template>
  <div class="users-page">
    <el-tabs>
      <el-tab-pane label="用户">
        <el-card shadow="never">
          <div class="toolbar">
            <el-input
              v-model="query.keyword"
              placeholder="用户名/昵称"
              clearable
              class="toolbar-input"
              @keyup.enter="search"
              @clear="search"
            />
            <el-button type="primary" @click="search">查询</el-button>
            <el-button type="success" @click="openCreate">新增用户</el-button>
          </div>

          <el-table v-loading="loading" :data="rows" stripe>
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="nickname" label="昵称" min-width="100">
              <template #default="{ row }">{{ row.nickname || '—' }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" min-width="120">
              <template #default="{ row }">{{ row.phone || '—' }}</template>
            </el-table-column>
            <el-table-column label="用户类型" width="90">
              <template #default="{ row }">{{ USER_TYPE_MAP[row.userType] ?? row.userType }}</template>
            </el-table-column>
            <el-table-column label="角色" min-width="160">
              <template #default="{ row }">
                <el-tag v-for="name in row.roleNames" :key="name" size="small" class="tag-gap">{{ name }}</el-tag>
                <span v-if="!row.roleNames?.length">—</span>
              </template>
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
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button link type="warning" @click="openReset(row)">重置密码</el-button>
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
            @current-change="loadUsers"
            @size-change="search"
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="角色">
        <el-card shadow="never">
          <!-- 菜单授权（PUT /roles/{id}/menus）需先有"角色已绑菜单"回显接口，后端二期补齐后开放 -->
          <el-alert
            title="角色为平台预置（R1-R8）；菜单授权编辑待后端提供角色已绑菜单回显后开放"
            type="info"
            :closable="false"
            class="mb"
          />
          <el-table :data="roles" stripe>
            <el-table-column prop="roleCode" label="角色编码" width="100" />
            <el-table-column prop="roleName" label="角色名称" min-width="140" />
            <el-table-column label="数据范围" width="120">
              <template #default="{ row }">
                {{ ({ 1: '全部品牌', 2: '本租户', 3: '名下门店', 4: '本店' } as Record<number, string>)[row.dataScope] ?? row.dataScope }}
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="说明" min-width="220">
              <template #default="{ row }">{{ row.remark || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑用户 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? `编辑用户：${editing.username}` : '新增用户'"
      width="520px"
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="form" :rules="createRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!editing" maxlength="32" />
        </el-form-item>
        <el-form-item v-if="!editing" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password maxlength="32" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" maxlength="32" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="至少分配一个角色" class="full">
            <el-option v-for="role in roles" :key="role.id" :value="role.id" :label="`${role.roleName}（${role.roleCode}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定门店">
          <el-select v-model="form.storeIds" multiple placeholder="总部员工可空" class="full">
            <el-option v-for="store in stores" :key="store.id" :value="store.id" :label="store.storeName" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" :title="`重置密码：${resetTarget?.username ?? ''}`" width="420px">
      <el-input v-model="newPassword" type="password" show-password placeholder="新密码（6-32 位）" maxlength="32" />
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReset">确定</el-button>
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

.tag-gap {
  margin-right: 4px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.mb {
  margin-bottom: 16px;
}

.full {
  width: 100%;
}
</style>
