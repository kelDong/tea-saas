<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import { franchiseApi } from '@/api/franchise'
import { listingApi } from '@/api/product'
import { posApi } from '@/api/pos'
import type { ListingVO } from '@/types/product'
import type { PosMemberVO, PosOrderCreateVO, PosPayVO } from '@/types/order'
import { fen2yuan } from '@/utils/format'

const stores = ref<Array<{ id: number; storeName: string }>>([])
const storeId = ref<number | undefined>(undefined)

// ---------- 可售商品（本店上架且未下架） ----------
const loading = ref(false)
const listings = ref<ListingVO[]>([])

async function loadListings() {
  if (!storeId.value) return
  loading.value = true
  try {
    const result = await listingApi.listByStore(storeId.value)
    listings.value = result.filter((item) => item.status === 1)
  } finally {
    loading.value = false
  }
}

// ---------- 购物车 ----------
interface CartRow {
  listing: ListingVO
  qty: number
}

const cart = ref<CartRow[]>([])

function addToCart(item: ListingVO) {
  if (item.sellable !== 1) {
    ElMessage.warning(`「${item.productName}」缺料，BOM 反算不可售`)
    return
  }
  const found = cart.value.find((row) => row.listing.id === item.id)
  if (found) found.qty += 1
  else cart.value.push({ listing: item, qty: 1 })
}

function inc(row: CartRow) {
  row.qty += 1
}

function dec(row: CartRow) {
  row.qty -= 1
  if (row.qty <= 0) cart.value = cart.value.filter((item) => item !== row)
}

function removeRow(row: CartRow) {
  cart.value = cart.value.filter((item) => item !== row)
}

/** 合计（分） */
const totalAmount = computed(() => cart.value.reduce((sum, row) => sum + row.listing.salePrice * row.qty, 0))

// ---------- 下单 ----------
const submitLoading = ref(false)
const currentOrder = ref<PosOrderCreateVO | null>(null)

async function submitOrder() {
  if (!storeId.value || !cart.value.length) return
  submitLoading.value = true
  try {
    const order = await posApi.create({
      storeId: storeId.value,
      items: cart.value.map((row) => ({ listingId: row.listing.id, qty: row.qty })),
    })
    currentOrder.value = order
    payChannel.value = 1
    receiveYuan.value = order.payAmount / 100
    memberId.value = undefined
    memberOptions.value = []
    payVisible.value = true
  } finally {
    submitLoading.value = false
  }
}

// ---------- 收款确认（POS 渠道：1现金/2微信Native/3储值） ----------
const payVisible = ref(false)
const payLoading = ref(false)
const payChannel = ref(1)
const receiveYuan = ref<number | undefined>(undefined)
const payResult = ref<PosPayVO | null>(null)
const resultVisible = ref(false)

// ---------- 会员（储值必选；现金/微信选填，绑会员自动积分） ----------
const memberId = ref<number | undefined>(undefined)
const memberOptions = ref<PosMemberVO[]>([])
const memberLoading = ref(false)

async function searchMembers(keyword: string) {
  if (!keyword) {
    memberOptions.value = []
    return
  }
  memberLoading.value = true
  try {
    memberOptions.value = (await posApi.members(keyword)).list
  } finally {
    memberLoading.value = false
  }
}

function memberLabel(m: PosMemberVO): string {
  const name = m.phone ? `${m.nickname ?? '会员'}(${m.phone})` : (m.nickname ?? '会员')
  return `${name} 余额¥${fen2yuan(m.balance)}`
}

const changePreview = computed(() => {
  if (payChannel.value !== 1 || receiveYuan.value == null || !currentOrder.value) return 0
  return Math.round(receiveYuan.value * 100) - currentOrder.value.payAmount
})

async function submitPay() {
  if (!currentOrder.value) return
  if (payChannel.value === 1 && receiveYuan.value == null) {
    ElMessage.warning('请输入实收金额')
    return
  }
  if (payChannel.value === 1 && changePreview.value < 0) {
    ElMessage.warning('实收金额不得小于应收金额')
    return
  }
  if (payChannel.value === 3 && memberId.value == null) {
    ElMessage.warning('储值收款请先选择会员')
    return
  }
  payLoading.value = true
  try {
    const result = await posApi.pay(currentOrder.value.id, {
      payChannel: payChannel.value,
      receiveAmount: payChannel.value === 1 ? Math.round(receiveYuan.value! * 100) : undefined,
      memberId: memberId.value,
    })
    payResult.value = result
    payVisible.value = false
    resultVisible.value = true
    cart.value = []
    currentOrder.value = null
  } finally {
    payLoading.value = false
  }
}

watch(storeId, () => {
  cart.value = []
  loadListings()
})

onMounted(async () => {
  stores.value = (await franchiseApi.storePage({ page: 1, size: 100 })).list
  if (stores.value.length) {
    storeId.value = stores.value[0].id
  }
})
</script>

<template>
  <div class="pos-page">
    <!-- 顶部：门店选择 -->
    <el-card shadow="never" class="top-bar">
      <div class="page-toolbar">
        <span class="label">门店</span>
        <el-select v-model="storeId" placeholder="选择门店" filterable class="toolbar-select">
          <el-option v-for="store in stores" :key="store.id" :value="store.id" :label="store.storeName" />
        </el-select>
        <el-button :loading="loading" @click="loadListings">刷新菜单</el-button>
      </div>
    </el-card>

    <div v-if="storeId" class="pos-body">
      <!-- 商品网格 -->
      <el-card v-loading="loading" shadow="never" class="goods-panel">
        <template #header>本店菜单（缺料商品置灰）</template>
        <el-empty v-if="!loading && !listings.length" description="本店暂无上架商品" />
        <div class="goods-grid">
          <div
            v-for="item in listings"
            :key="item.id"
            class="goods-card"
            :class="{ disabled: item.sellable !== 1 }"
            @click="addToCart(item)"
          >
            <div class="goods-name">{{ item.productName }}</div>
            <div class="goods-price">{{ fen2yuan(item.salePrice) }}</div>
            <el-tag v-if="item.sellable !== 1" type="danger" size="small" class="goods-tag">缺料</el-tag>
          </div>
        </div>
      </el-card>

      <!-- 购物车 -->
      <el-card shadow="never" class="cart-panel">
        <template #header>购物车</template>
        <el-empty v-if="!cart.length" description="点击左侧商品加入" :image-size="60" />
        <div v-for="row in cart" :key="row.listing.id" class="cart-row">
          <div class="cart-name">{{ row.listing.productName }}</div>
          <div class="cart-price">{{ fen2yuan(row.listing.salePrice) }}</div>
          <div class="cart-qty">
            <el-button size="small" circle @click="dec(row)">-</el-button>
            <span class="qty">{{ row.qty }}</span>
            <el-button size="small" circle @click="inc(row)">+</el-button>
          </div>
          <el-button link type="danger" size="small" @click="removeRow(row)">删除</el-button>
        </div>
        <template v-if="cart.length">
          <div class="cart-total">
            <span>合计</span>
            <span class="amount">{{ fen2yuan(totalAmount) }}</span>
          </div>
          <el-button v-permission="'pos:create'" type="primary" class="full" size="large" :loading="submitLoading" @click="submitOrder">
            下单收款（{{ fen2yuan(totalAmount) }}）
          </el-button>
        </template>
      </el-card>
    </div>

    <!-- 收款确认 -->
    <el-dialog v-model="payVisible" :title="`收款：${currentOrder?.orderNo ?? ''}`" width="480px" :close-on-click-modal="false">
      <el-descriptions :column="2" border class="pay-info">
        <el-descriptions-item label="订单号" :span="2"><span class="mono">{{ currentOrder?.orderNo }}</span></el-descriptions-item>
        <el-descriptions-item label="取餐码">{{ currentOrder?.takeCode }}</el-descriptions-item>
        <el-descriptions-item label="应收">{{ fen2yuan(currentOrder?.payAmount ?? 0) }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="100px" class="pay-form">
        <el-form-item label="收款渠道">
          <el-radio-group v-model="payChannel">
            <el-radio :value="1">现金</el-radio>
            <el-radio :value="2">微信收款码</el-radio>
            <el-radio :value="3">储值</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="会员">
          <el-select
            v-model="memberId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="昵称/手机号搜索（储值必选）"
            :remote-method="searchMembers"
            :loading="memberLoading"
            class="full"
          >
            <el-option
              v-for="m in memberOptions"
              :key="m.id"
              :value="m.id"
              :label="memberLabel(m)"
            />
          </el-select>
          <span class="tip">{{ payChannel === 3 ? '储值扣款，余额不足将失败' : '选填：绑定会员自动积分' }}</span>
        </el-form-item>
        <el-form-item v-if="payChannel === 1" label="实收金额">
          <el-input-number v-model="receiveYuan" :min="0" :precision="2" :step="1" />
          <span class="tip">元；找零 {{ fen2yuan(Math.max(changePreview, 0)) }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">稍后收款（订单已扣库存）</el-button>
        <el-button type="primary" :loading="payLoading" @click="submitPay">确认收款</el-button>
      </template>
    </el-dialog>

    <!-- 收款结果 -->
    <el-dialog v-model="resultVisible" title="收款成功" width="480px">
      <el-result icon="success" title="收款完成" :sub-title="payResult?.orderNo">
        <template #extra>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="支付流水号">{{ payResult?.paymentNo }}</el-descriptions-item>
            <el-descriptions-item label="应收">{{ fen2yuan(payResult?.payAmount ?? 0) }}</el-descriptions-item>
            <el-descriptions-item label="实收">{{ fen2yuan(payResult?.receiveAmount ?? 0) }}</el-descriptions-item>
            <el-descriptions-item label="找零">{{ fen2yuan(payResult?.changeAmount ?? 0) }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </el-result>
    </el-dialog>
  </div>
</template>

<style scoped>
.top-bar {
  margin-bottom: 16px;
}

.top-bar .page-toolbar {
  margin-bottom: 0;
}

.page-toolbar .label {
  font-size: 13px;
  color: var(--ts-ink-2);
}

.pos-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.goods-panel {
  flex: 1;
  min-width: 0;
}

.cart-panel {
  width: 320px;
  flex-shrink: 0;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.goods-card {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 14px 10px;
  text-align: center;
  cursor: pointer;
  position: relative;
  transition: border-color 0.2s;
}

.goods-card:hover {
  border-color: var(--el-color-primary);
}

.goods-card.disabled {
  background: var(--el-fill-color-light);
  cursor: not-allowed;
  opacity: 0.6;
}

.goods-name {
  font-size: 14px;
  min-height: 40px;
}

.goods-price {
  margin-top: 6px;
  color: var(--ts-danger);
  font-weight: 600;
}

.goods-tag {
  position: absolute;
  top: 6px;
  right: 6px;
}

.cart-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.cart-name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-price {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.cart-qty {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cart-qty .qty {
  min-width: 20px;
  text-align: center;
}

.cart-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 12px 0;
  font-size: 14px;
}

.cart-total .amount {
  color: var(--ts-danger);
  font-size: 20px;
  font-weight: 700;
}

.pay-info {
  margin-bottom: 16px;
}
</style>
