<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { franchiseApi } from '@/api/franchise'
import { listingApi } from '@/api/product'
import type { ShortageNode } from '@/types/product'
import type { ListingVO, PrdProduct } from '@/types/product'
import { fen2yuan } from '@/utils/format'

// ---------- 门店选择 ----------
const stores = ref<{ id: number; storeName: string }[]>([])
const storeId = ref<number | undefined>(undefined)

async function loadStores() {
  const result = await franchiseApi.storePage({ page: 1, size: 100 })
  stores.value = result.list
  if (result.list.length) {
    storeId.value = result.list[0].id
  }
}

// ---------- 本店上架列表 ----------
const loading = ref(false)
const listings = ref<ListingVO[]>([])

async function loadListings() {
  if (!storeId.value) return
  loading.value = true
  try {
    listings.value = await listingApi.listByStore(storeId.value)
  } finally {
    loading.value = false
  }
}

// ---------- 上架（可上架商品） ----------
const availableVisible = ref(false)
const availableLoading = ref(false)
const availableRows = ref<PrdProduct[]>([])
const availableTotal = ref(0)
const availableQuery = reactive({ page: 1, size: 10, keyword: '' })

async function loadAvailable() {
  availableLoading.value = true
  try {
    const result = await listingApi.availableProducts({
      ...availableQuery,
      keyword: availableQuery.keyword || undefined,
    })
    availableRows.value = result.list
    availableTotal.value = result.total
  } finally {
    availableLoading.value = false
  }
}

function openAvailable() {
  if (!storeId.value) {
    ElMessage.warning('请先选择门店')
    return
  }
  availableQuery.page = 1
  availableQuery.keyword = ''
  availableVisible.value = true
  loadAvailable()
}

// 上架价格弹窗
const listVisible = ref(false)
const listTarget = ref<PrdProduct | null>(null)
const listPriceYuan = ref<number | undefined>(undefined)

function openList(row: PrdProduct) {
  listTarget.value = row
  listPriceYuan.value = row.suggestPrice / 100
  listVisible.value = true
}

async function submitList() {
  if (!listTarget.value || !storeId.value || listPriceYuan.value == null) {
    ElMessage.warning('请输入本店售价')
    return
  }
  await listingApi.create({
    storeId: storeId.value,
    productId: listTarget.value.id,
    salePrice: Math.round(listPriceYuan.value * 100),
  })
  ElMessage.success('已上架')
  listVisible.value = false
  loadListings()
}

// ---------- 调价 / 下架 ----------
const priceVisible = ref(false)
const priceTarget = ref<ListingVO | null>(null)
const priceYuan = ref<number | undefined>(undefined)

function openPrice(row: ListingVO) {
  priceTarget.value = row
  priceYuan.value = row.salePrice / 100
  priceVisible.value = true
}

async function submitPrice() {
  if (!priceTarget.value || priceYuan.value == null) {
    ElMessage.warning('请输入价格')
    return
  }
  const price = Math.round(priceYuan.value * 100)
  if (price > priceTarget.value.suggestPrice) {
    ElMessage.warning('本店售价不得高于总部建议售价')
    return
  }
  await listingApi.adjustPrice(priceTarget.value.id, price)
  ElMessage.success('已调价')
  priceVisible.value = false
  loadListings()
}

async function off(row: ListingVO) {
  await listingApi.off(row.id)
  ElMessage.success('已下架')
  loadListings()
}

// ---------- 可售校验（缺料明细） ----------
const checkVisible = ref(false)
const checkLoading = ref(false)
const checkResults = ref<Array<{ listingId: number; productName: string; sellable: boolean; shortages: ShortageNode[] }>>([])

async function openCheck() {
  if (!listings.value.length) {
    ElMessage.warning('当前门店无上架商品')
    return
  }
  checkVisible.value = true
  checkLoading.value = true
  try {
    const results = await listingApi.checkSellable(listings.value.map((item) => item.id))
    const nameMap = new Map(listings.value.map((item) => [item.id, item.productName]))
    checkResults.value = results.map((item) => ({
      listingId: item.listingId,
      productName: nameMap.get(item.listingId) ?? `上架${item.listingId}`,
      sellable: item.sellable,
      shortages: item.shortages,
    }))
  } finally {
    checkLoading.value = false
  }
}

onMounted(async () => {
  await loadStores()
  loadListings()
})
</script>

<template>
  <div class="listings-page">
    <el-card shadow="never">
      <div class="page-toolbar">
        <el-select v-model="storeId" placeholder="选择门店" class="toolbar-select" @change="loadListings">
          <el-option v-for="store in stores" :key="store.id" :value="store.id" :label="store.storeName" />
        </el-select>
        <span class="toolbar-spacer" />
        <el-button v-permission="'listing:add'" type="success" @click="openAvailable">上架商品</el-button>
        <el-button @click="openCheck">可售校验</el-button>
      </div>

      <el-table v-loading="loading" :data="listings" stripe>
        <el-table-column label="编码" min-width="110">
          <template #default="{ row }">
            <span class="mono">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名" min-width="150" />
        <el-table-column label="本店售价" width="100" align="right">
          <template #default="{ row }">
            <span class="mono">{{ fen2yuan(row.salePrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总部建议价" width="110" align="right">
          <template #default="{ row }">
            <span class="mono">{{ fen2yuan(row.suggestPrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上架状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可售" width="90">
          <template #default="{ row }">
            <el-tag :type="row.sellable === 1 ? 'success' : 'danger'" size="small">
              {{ row.sellable === 1 ? '可售' : '缺料' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'listing:price'" link type="warning" @click="openPrice(row)">调价</el-button>
            <el-button v-if="row.status === 1" v-permission="'listing:off'" link type="danger" @click="off(row)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="page-note">对应 PRD：F-PRD-02/F-PRD-03</div>

    <!-- 可上架商品 -->
    <el-dialog v-model="availableVisible" title="可上架商品（已发布且未在本店上架）" width="720px">
      <div class="page-toolbar">
        <el-input
          v-model="availableQuery.keyword"
          placeholder="编码/名称"
          clearable
          class="toolbar-input"
          @keyup.enter="availableQuery.page = 1; loadAvailable()"
          @clear="availableQuery.page = 1; loadAvailable()"
        />
      </div>
      <el-table v-loading="availableLoading" :data="availableRows" stripe max-height="400">
        <el-table-column label="编码" min-width="110">
          <template #default="{ row }">
            <span class="mono">{{ row.productCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名" min-width="150" />
        <el-table-column label="建议售价" width="100" align="right">
          <template #default="{ row }">
            <span class="mono">{{ fen2yuan(row.suggestPrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'listing:add'" link type="primary" @click="openList(row)">上架</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="availableQuery.page"
        v-model:page-size="availableQuery.size"
        :total="availableTotal"
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        class="pager"
        @current-change="loadAvailable"
      />
    </el-dialog>

    <!-- 上架定价 -->
    <el-dialog v-model="listVisible" :title="`上架：${listTarget?.productName ?? ''}`" width="480px">
      <el-alert
        v-if="listTarget"
        :title="`总部建议售价 ${fen2yuan(listTarget.suggestPrice)}，本店售价不得高于它`"
        type="info"
        :closable="false"
        class="mb"
      />
      <el-input-number v-model="listPriceYuan" :min="0.01" :step="1" :precision="2" class="full" />
      <span class="tip">元</span>
      <template #footer>
        <el-button @click="listVisible = false">取消</el-button>
        <el-button type="primary" @click="submitList">确定</el-button>
      </template>
    </el-dialog>

    <!-- 调整本店售价 -->
    <el-dialog v-model="priceVisible" :title="`调整本店售价：${priceTarget?.productName ?? ''}`" width="480px">
      <el-input-number v-model="priceYuan" :min="0.01" :step="1" :precision="2" class="full" />
      <span class="tip">元</span>
      <template #footer>
        <el-button @click="priceVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPrice">确定</el-button>
      </template>
    </el-dialog>

    <!-- 可售校验 -->
    <el-dialog v-model="checkVisible" title="商品可售校验（实时库存比对配方）" width="640px">
      <div v-loading="checkLoading">
        <el-table :data="checkResults" row-key="listingId" default-expand-all>
          <el-table-column type="expand">
            <template #default="{ row }">
              <el-table v-if="row.shortages.length" :data="row.shortages" size="small" class="shortage-table">
                <el-table-column prop="materialName" label="缺料原料" min-width="120" />
                <el-table-column prop="unit" label="单位" width="60" />
                <el-table-column label="需求量" width="100" align="right">
                  <template #default="{ row: s }">
                    <span class="mono">{{ s.needQty }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="库存量" width="100" align="right">
                  <template #default="{ row: s }">
                    <span class="mono">{{ s.stockQty }}</span>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="原料充足" :image-size="48" />
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="商品" min-width="150" />
          <el-table-column label="结果" width="90">
            <template #default="{ row }">
              <el-tag :type="row.sellable ? 'success' : 'danger'" size="small">
                {{ row.sellable ? '可售' : '缺料' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.mb {
  margin-bottom: 12px;
}

.shortage-table {
  margin: 0 24px;
  width: auto;
}
</style>
