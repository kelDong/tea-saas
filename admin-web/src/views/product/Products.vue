<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'

import { formulaApi } from '@/api/bom'
import { categoryApi, productApi } from '@/api/product'
import { uploadApi } from '@/api/upload'
import type { BomFormula } from '@/types/bom'
import type { PrdCategory, PrdProduct, ProductSpec } from '@/types/product'
import { fen2yuan, formatDateTime } from '@/utils/format'

// ---------- 商品列表 ----------
const loading = ref(false)
const rows = ref<PrdProduct[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  categoryId: undefined as number | undefined,
  publishStatus: undefined as number | undefined,
})

const categories = ref<PrdCategory[]>([])
const categoryMap = ref(new Map<number, string>())

async function loadCategories() {
  categories.value = await categoryApi.list()
  categoryMap.value = new Map(categories.value.map((item) => [item.id, item.categoryName]))
}

async function load() {
  loading.value = true
  try {
    const result = await productApi.page({
      ...query,
      keyword: query.keyword || undefined,
      categoryId: query.categoryId,
      publishStatus: query.publishStatus,
    })
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

function reset() {
  query.keyword = ''
  query.categoryId = undefined
  query.publishStatus = undefined
  search()
}

// ---------- 配方下拉（已发布配方可选） ----------
const formulas = ref<BomFormula[]>([])

async function loadFormulas() {
  const result = await formulaApi.page({ page: 1, size: 100 })
  formulas.value = result.list
}

// ---------- 新增/编辑（规格/选项动态行） ----------
const dialogVisible = ref(false)
const editing = ref<PrdProduct | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  categoryId: undefined as number | undefined,
  productCode: '',
  productName: '',
  formulaId: undefined as number | undefined,
  suggestPriceYuan: undefined as number | undefined,
  image: '',
  description: '',
  publishStatus: 0,
  specs: [] as ProductSpec[],
})

const rules: FormRules = {
  productCode: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  formulaId: [{ required: true, message: '请选择配方', trigger: 'change' }],
  suggestPriceYuan: [{ required: true, message: '请输入建议售价', trigger: 'blur' }],
}

function emptySpec(): ProductSpec {
  return { specName: '', isRequired: 1, options: [{ optionName: '', extraPrice: undefined }] }
}

function openCreate() {
  editing.value = null
  Object.assign(form, {
    categoryId: undefined,
    productCode: '',
    productName: '',
    formulaId: undefined,
    suggestPriceYuan: undefined,
    image: '',
    description: '',
    publishStatus: 0,
    specs: [],
  })
  dialogVisible.value = true
}

async function openEdit(row: PrdProduct) {
  const detail = await productApi.detail(row.id)
  editing.value = row
  Object.assign(form, {
    categoryId: detail.categoryId ?? undefined,
    productCode: detail.productCode,
    productName: detail.productName,
    formulaId: detail.formulaId,
    suggestPriceYuan: detail.suggestPrice / 100,
    image: detail.image ?? '',
    description: detail.description ?? '',
    publishStatus: detail.publishStatus,
    specs: detail.specs.map((spec) => ({
      id: spec.id,
      specName: spec.specName,
      isRequired: spec.isRequired,
      sort: spec.sort,
      options: spec.options.map((option) => ({
        id: option.id,
        optionName: option.optionName,
        extraPrice: option.extraPrice,
        sort: option.sort,
      })),
    })),
  })
  dialogVisible.value = true
}

function addSpec() {
  form.specs.push(emptySpec())
}

function removeSpec(index: number) {
  form.specs.splice(index, 1)
}

function addOption(spec: ProductSpec) {
  spec.options.push({ optionName: '', extraPrice: undefined })
}

function removeOption(spec: ProductSpec, index: number) {
  spec.options.splice(index, 1)
}

// ---------- 图片上传（MinIO，form.image 为唯一事实源） ----------
const uploading = ref(false)

const IMAGE_MAX_BYTES = 5 * 1024 * 1024
const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

function beforeImageUpload(file: File): boolean {
  if (!IMAGE_TYPES.includes(file.type)) {
    ElMessage.warning('仅支持 jpg/jpeg/png/webp/gif 图片')
    return false
  }
  if (file.size > IMAGE_MAX_BYTES) {
    ElMessage.warning('图片大小不能超过 5MB')
    return false
  }
  return true
}

async function doUploadImage(options: UploadRequestOptions): Promise<void> {
  uploading.value = true
  try {
    form.image = await uploadApi.upload(options.file)
  } finally {
    uploading.value = false
  }
}

async function submit() {
  await formRef.value?.validate()
  if (form.specs.some((spec) => !spec.specName || spec.options.some((option) => !option.optionName))) {
    ElMessage.warning('规格/选项名称不能为空，请补全或删除空行')
    return
  }
  const payload = {
    categoryId: form.categoryId,
    productCode: form.productCode,
    productName: form.productName,
    formulaId: form.formulaId!,
    suggestPrice: Math.round((form.suggestPriceYuan ?? 0) * 100),
    image: form.image || undefined,
    description: form.description || undefined,
    publishStatus: form.publishStatus,
    specs: form.specs,
  }
  if (editing.value) {
    await productApi.update(editing.value.id, payload)
    ElMessage.success('已保存（规格全量重绑）')
  } else {
    await productApi.create(payload)
    ElMessage.success(form.publishStatus === 1 ? '已发布' : '已存草稿')
  }
  dialogVisible.value = false
  load()
}

// ---------- 调价 / 下架 ----------
const priceVisible = ref(false)
const priceTarget = ref<PrdProduct | null>(null)
const priceYuan = ref<number | undefined>(undefined)

function openPrice(row: PrdProduct) {
  priceTarget.value = row
  priceYuan.value = row.suggestPrice / 100
  priceVisible.value = true
}

async function submitPrice() {
  if (!priceTarget.value || priceYuan.value == null) {
    ElMessage.warning('请输入价格')
    return
  }
  await productApi.adjustPrice(priceTarget.value.id, Math.round(priceYuan.value * 100))
  ElMessage.success('已调价')
  priceVisible.value = false
  load()
}

async function off(row: PrdProduct) {
  await productApi.off(row.id)
  ElMessage.success('已下架（转草稿）')
  load()
}

// ---------- 分类管理 ----------
const categoryDialogVisible = ref(false)
const categoryEditing = ref<PrdCategory | null>(null)
const categoryForm = reactive({ categoryName: '', parentId: 0, sort: 0 })

function openCategoryCreate(parentId = 0) {
  categoryEditing.value = null
  Object.assign(categoryForm, { categoryName: '', parentId, sort: 0 })
  categoryDialogVisible.value = true
}

function openCategoryEdit(row: PrdCategory) {
  categoryEditing.value = row
  Object.assign(categoryForm, { categoryName: row.categoryName, parentId: row.parentId, sort: row.sort })
  categoryDialogVisible.value = true
}

async function submitCategory() {
  if (!categoryForm.categoryName) {
    ElMessage.warning('请输入分类名称')
    return
  }
  const payload = { categoryName: categoryForm.categoryName, parentId: categoryForm.parentId, sort: categoryForm.sort }
  if (categoryEditing.value) {
    await categoryApi.update(categoryEditing.value.id, payload)
    ElMessage.success('已保存')
  } else {
    await categoryApi.create(payload)
    ElMessage.success('已创建')
  }
  categoryDialogVisible.value = false
  loadCategories()
}

onMounted(() => {
  load()
  loadCategories()
  loadFormulas()
})
</script>

<template>
  <div class="products-page">
    <el-tabs>
      <el-tab-pane label="商品">
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
            <el-select v-model="query.categoryId" placeholder="全部分类" clearable class="toolbar-select" @change="search">
              <el-option v-for="item in categories" :key="item.id" :value="item.id" :label="item.categoryName" />
            </el-select>
            <el-select v-model="query.publishStatus" placeholder="全部状态" clearable class="toolbar-select" @change="search">
              <el-option :value="1" label="已发布" />
              <el-option :value="0" label="草稿" />
            </el-select>
            <el-button type="primary" @click="search">查询</el-button>
            <el-button @click="reset">重置</el-button>
            <el-button v-permission="'product:publish'" type="success" @click="openCreate">发布商品</el-button>
          </div>

          <el-table v-loading="loading" :data="rows" stripe>
            <el-table-column prop="productCode" label="编码" min-width="120" />
            <el-table-column prop="productName" label="商品名" min-width="150" />
            <el-table-column label="图片" width="70">
              <template #default="{ row }">
                <el-image
                  v-if="row.image"
                  :src="row.image"
                  fit="cover"
                  class="table-thumb"
                  :preview-src-list="[row.image]"
                  preview-teleported
                />
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="分类" width="100">
              <template #default="{ row }">
                {{ row.categoryId != null ? (categoryMap.get(row.categoryId) ?? `分类${row.categoryId}`) : '—' }}
              </template>
            </el-table-column>
            <el-table-column label="建议售价" width="100" align="right">
              <template #default="{ row }">{{ fen2yuan(row.suggestPrice) }}</template>
            </el-table-column>
            <el-table-column label="发布状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.publishStatus === 1 ? 'success' : 'info'" size="small">
                  {{ row.publishStatus === 1 ? '已发布' : '草稿' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="160">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button v-permission="'product:publish'" link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button v-permission="'product:price'" link type="warning" @click="openPrice(row)">调价</el-button>
                <el-button v-if="row.publishStatus === 1" v-permission="'product:off'" link type="danger" @click="off(row)">下架</el-button>
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
      </el-tab-pane>

      <el-tab-pane label="分类">
        <el-card shadow="never">
          <div class="toolbar">
            <el-button type="success" @click="openCategoryCreate()">新增顶级分类</el-button>
          </div>
          <el-table :data="categories" row-key="id" default-expand-all stripe>
            <el-table-column prop="categoryName" label="分类名" min-width="180" />
            <el-table-column prop="sort" label="排序" width="80" />
            <el-table-column label="创建时间" min-width="160">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openCategoryEdit(row)">编辑</el-button>
                <el-button link type="success" @click="openCategoryCreate(row.id)">加子分类</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 发布/编辑商品 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? `编辑商品：${editing.productName}` : '发布商品'"
      width="760px"
      top="6vh"
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="商品编码" prop="productCode">
          <el-input v-model="form.productCode" :disabled="!!editing" maxlength="32" />
        </el-form-item>
        <el-form-item label="商品名称" prop="productName">
          <el-input v-model="form.productName" maxlength="64" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="可空" clearable class="full">
            <el-option v-for="item in categories" :key="item.id" :value="item.id" :label="item.categoryName" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定配方" prop="formulaId">
          <el-select v-model="form.formulaId" placeholder="选择配方" filterable class="full">
            <el-option
              v-for="formula in formulas"
              :key="formula.id"
              :value="formula.id"
              :label="`${formula.formulaName}（v${formula.version}）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="建议售价(元)" prop="suggestPriceYuan">
          <el-input-number v-model="form.suggestPriceYuan" :min="0.01" :step="1" :precision="2" class="full" />
        </el-form-item>
        <el-form-item label="图片">
          <div v-loading="uploading" class="full image-block">
            <div class="image-row">
              <el-upload
                :show-file-list="false"
                accept="image/jpeg,image/png,image/webp,image/gif"
                :before-upload="beforeImageUpload"
                :http-request="doUploadImage"
              >
                <div class="image-card">
                  <el-image
                    v-if="form.image"
                    :src="form.image"
                    fit="cover"
                    class="image-fill"
                    :preview-src-list="[form.image]"
                    preview-teleported
                    @click.stop
                  />
                  <span v-else class="image-plus">+</span>
                </div>
              </el-upload>
              <el-button v-if="form.image" link type="danger" @click="form.image = ''">移除</el-button>
            </div>
            <el-input
              v-model="form.image"
              placeholder="或直接粘贴图片 URL（外部图床兜底）"
              maxlength="255"
              size="small"
              class="image-url-input"
            />
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
        <el-form-item label="发布状态">
          <el-radio-group v-model="form.publishStatus">
            <el-radio :value="1">发布</el-radio>
            <el-radio :value="0">存草稿</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="规格">
          <div class="full">
            <div v-for="(spec, specIndex) in form.specs" :key="specIndex" class="spec-block">
              <div class="spec-head">
                <el-input v-model="spec.specName" placeholder="规格名（如 杯型）" maxlength="16" class="spec-name" />
                <el-select v-model="spec.isRequired" class="spec-required">
                  <el-option :value="1" label="必选" />
                  <el-option :value="0" label="可选" />
                </el-select>
                <el-button link type="danger" @click="removeSpec(specIndex)">删除规格</el-button>
              </div>
              <div v-for="(option, optionIndex) in spec.options" :key="optionIndex" class="option-row">
                <el-input v-model="option.optionName" placeholder="选项名（如 大杯）" maxlength="16" class="option-name" />
                <el-input-number v-model="option.extraPrice" :min="0" :step="50" placeholder="加价(分)" />
                <el-button link type="danger" :disabled="spec.options.length <= 1" @click="removeOption(spec, optionIndex)">删除</el-button>
              </div>
              <el-button link type="primary" @click="addOption(spec)">+ 添加选项</el-button>
            </div>
            <el-button link type="primary" @click="addSpec">+ 添加规格</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 调整建议售价 -->
    <el-dialog v-model="priceVisible" :title="`调整建议售价：${priceTarget?.productName ?? ''}`" width="420px">
      <el-input-number v-model="priceYuan" :min="0.01" :step="1" :precision="2" class="full" />
      <template #footer>
        <el-button @click="priceVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPrice">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分类编辑 -->
    <el-dialog v-model="categoryDialogVisible" :title="categoryEditing ? '编辑分类' : '新增分类'" width="420px">
      <el-form label-width="90px">
        <el-form-item label="分类名">
          <el-input v-model="categoryForm.categoryName" maxlength="32" />
        </el-form-item>
        <el-form-item label="父分类ID">
          <el-input-number v-model="categoryForm.parentId" :min="0" controls-position="right" class="full" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sort" :min="0" :max="9999" controls-position="right" class="full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCategory">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar-input {
  width: 200px;
}

.toolbar-select {
  width: 140px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.full {
  width: 100%;
}

.spec-block {
  width: 100%;
  padding: 10px;
  margin-bottom: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
}

.spec-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.spec-name {
  flex: 1;
}

.spec-required {
  width: 100px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.option-name {
  flex: 1;
}

.image-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.image-card {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  overflow: hidden;
  cursor: pointer;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  transition: border-color 0.2s;
}

.image-card:hover {
  border-color: var(--el-color-primary);
}

.image-plus {
  font-size: 24px;
  color: var(--el-text-color-placeholder);
}

.image-fill {
  width: 100%;
  height: 100%;
}

.image-url-input {
  margin-top: 8px;
}

.table-thumb {
  display: block;
  width: 40px;
  height: 40px;
  border-radius: 4px;
}
</style>
