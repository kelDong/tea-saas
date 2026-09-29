<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { couponApi } from '@/api/marketing'
import type { CouponTemplateDTO, MktCouponTemplate } from '@/types/marketing'
import { fen2yuan, formatDateTime } from '@/utils/format'

// ---------- 列表 ----------
const loading = ref(false)
const rows = ref<MktCouponTemplate[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  status: undefined as number | undefined,
})

async function load() {
  loading.value = true
  try {
    const result = await couponApi.page({ ...query, status: query.status })
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

/** 优惠描述列 */
function benefit(row: MktCouponTemplate): string {
  if (row.type === 1) {
    const threshold = row.threshold ? `满${fen2yuan(row.threshold)}` : '无门槛'
    return `${threshold}减${fen2yuan(row.amount ?? 0)}`
  }
  const threshold = row.threshold ? `满${fen2yuan(row.threshold)}` : '无门槛'
  return `${threshold}打${(row.discount ?? 1) * 10}折`
}

function validText(row: MktCouponTemplate): string {
  if (row.validType === 1) return `${row.validStart ?? '—'} ~ ${row.validEnd ?? '—'}`
  return `领取后 ${row.validDays ?? '—'} 天有效`
}

// ---------- 新建/编辑（元↔分 换算仅在前端表单层） ----------
const dialogVisible = ref(false)
const editing = ref<MktCouponTemplate | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  type: 1,
  thresholdYuan: undefined as number | undefined,
  amountYuan: undefined as number | undefined,
  discount: undefined as number | undefined,
  totalQty: -1,
  perLimit: 1,
  validType: 1,
  range: [] as string[],
  validDays: undefined as number | undefined,
  status: 1,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入券名称', trigger: 'blur' }],
}

const isEdit = computed(() => !!editing.value)

function openCreate() {
  editing.value = null
  Object.assign(form, {
    name: '',
    type: 1,
    thresholdYuan: undefined,
    amountYuan: undefined,
    discount: undefined,
    totalQty: -1,
    perLimit: 1,
    validType: 1,
    range: [],
    validDays: undefined,
    status: 1,
  })
  dialogVisible.value = true
}

/** 编辑回填：按 id 拉详情（二期A 前列表行内数据回填改为真详情，防止列表字段不全） */
async function openEdit(row: MktCouponTemplate) {
  const detail = await couponApi.detail(row.id)
  editing.value = detail
  Object.assign(form, {
    name: detail.name,
    type: detail.type,
    thresholdYuan: detail.threshold ? detail.threshold / 100 : undefined,
    amountYuan: detail.amount ? detail.amount / 100 : undefined,
    discount: detail.discount ?? undefined,
    totalQty: detail.totalQty,
    perLimit: detail.perLimit,
    validType: detail.validType,
    range: detail.validStart && detail.validEnd ? [detail.validStart, detail.validEnd] : [],
    validDays: detail.validDays ?? undefined,
    status: detail.status,
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  if (form.type === 1 && !form.amountYuan) {
    ElMessage.warning('满减券请填写减免金额')
    return
  }
  if (form.type === 2 && form.discount == null) {
    ElMessage.warning('折扣券请填写折扣率')
    return
  }
  if (form.validType === 1 && form.range.length !== 2) {
    ElMessage.warning('请选择固定有效时段')
    return
  }
  if (form.validType === 2 && !form.validDays) {
    ElMessage.warning('请填写领取后有效天数')
    return
  }
  const payload: CouponTemplateDTO = {
    name: form.name,
    type: form.type,
    threshold: form.thresholdYuan ? Math.round(form.thresholdYuan * 100) : undefined,
    amount: form.type === 1 && form.amountYuan ? Math.round(form.amountYuan * 100) : undefined,
    discount: form.type === 2 ? form.discount : undefined,
    totalQty: form.totalQty,
    perLimit: form.perLimit,
    validType: form.validType,
    validStart: form.validType === 1 ? form.range[0] : undefined,
    validEnd: form.validType === 1 ? form.range[1] : undefined,
    validDays: form.validType === 2 ? form.validDays : undefined,
    status: form.status,
  }
  if (editing.value) {
    await couponApi.update(editing.value.id, payload)
    ElMessage.success('已保存（已领出的券不受影响）')
  } else {
    await couponApi.create(payload)
    ElMessage.success('券模板已创建')
  }
  dialogVisible.value = false
  load()
}

/** 快捷停用/启用（PUT status 切换，其余字段原样回传） */
async function toggleStatus(row: MktCouponTemplate) {
  const next = row.status === 1 ? 2 : 1
  await couponApi.update(row.id, {
    name: row.name,
    type: row.type,
    threshold: row.threshold ?? undefined,
    amount: row.amount ?? undefined,
    discount: row.discount ?? undefined,
    totalQty: row.totalQty,
    perLimit: row.perLimit,
    validType: row.validType,
    validStart: row.validStart ?? undefined,
    validEnd: row.validEnd ?? undefined,
    validDays: row.validDays ?? undefined,
    status: next,
  })
  ElMessage.success(next === 1 ? '已启用' : '已停用')
  load()
}

onMounted(load)
</script>

<template>
  <div class="coupons-page">
    <el-card shadow="never">
      <div class="page-toolbar">
        <el-select v-model="query.status" placeholder="全部状态" clearable class="toolbar-select" @change="search">
          <el-option :value="1" label="启用" />
          <el-option :value="2" label="停用" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <span class="toolbar-spacer" />
        <el-button type="success" @click="openCreate">新建券模板</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="name" label="券名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'warning' : 'danger'" size="small">
              {{ row.type === 1 ? '满减' : '折扣' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠内容" min-width="150">
          <template #default="{ row }">{{ benefit(row) }}</template>
        </el-table-column>
        <el-table-column label="发放总量" width="100" align="right">
          <template #default="{ row }">
            <span class="mono">{{ row.totalQty === -1 ? '不限' : row.totalQty }}</span>
          </template>
        </el-table-column>
        <el-table-column label="限领" width="70" align="right">
          <template #default="{ row }">
            <span class="mono">{{ row.perLimit }}</span>
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ validText(row) }}</template>
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
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <div class="page-note">对应 PRD：F-MKT-01｜停用模板不影响已领出的券；取消/退款不退券（PRD 决策）</div>

    <!-- 新建/编辑券模板 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? `编辑券模板：${editing?.name}` : '新建券模板'"
      width="560px"
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="券名称" prop="name">
          <el-input v-model="form.name" maxlength="32" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减</el-radio>
            <el-radio :value="2">折扣</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="使用门槛">
          <el-input-number v-model="form.thresholdYuan" :min="0" :precision="2" :step="1" />
          <span class="tip">元（0 或留空=无门槛）</span>
        </el-form-item>
        <el-form-item v-if="form.type === 1" label="减免金额">
          <el-input-number v-model="form.amountYuan" :min="0.01" :precision="2" :step="1" />
          <span class="tip">元</span>
        </el-form-item>
        <el-form-item v-if="form.type === 2" label="折扣率">
          <el-input-number v-model="form.discount" :min="0.01" :max="0.99" :precision="2" :step="0.01" />
          <span class="tip">如 0.90 = 9 折</span>
        </el-form-item>
        <el-form-item label="发放总量">
          <el-input-number v-model="form.totalQty" :min="-1" />
          <span class="tip">-1 不限量</span>
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="form.perLimit" :min="1" />
        </el-form-item>
        <el-form-item label="有效期类型">
          <el-radio-group v-model="form.validType">
            <el-radio :value="1">固定时段</el-radio>
            <el-radio :value="2">领取后N天</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.validType === 1" label="有效时段">
          <el-date-picker
            v-model="form.range"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            class="full"
          />
        </el-form-item>
        <el-form-item v-if="form.validType === 2" label="有效天数">
          <el-input-number v-model="form.validDays" :min="1" />
          <span class="tip">天（自领取之日起）</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="2">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
