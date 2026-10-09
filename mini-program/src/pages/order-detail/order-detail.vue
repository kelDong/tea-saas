<script setup lang="ts">
/**
 * 订单详情（F-ORD-02，原型无独立屏——按契约全字段展示）
 * - steps/取餐码/items/金额/payChannel 文案/cancelReason
 * - 待支付单：继续支付（跳 pay-result）+ 取消（回补库存）
 * - 再来一单：reorder → 跨店确认 → 队列重选规格 → menu
 */

import { onLoad, onShow } from '@dcloudio/uni-app';
import { computed, ref } from 'vue';
import { orderApi } from '@/api';
import { useCartStore } from '@/stores/cart';
import { useShopStore } from '@/stores/shop';
import StateTag from '@/components/StateTag.vue';
import { fen2yuan, formatDateTime, orderSteps, PAY_CHANNEL } from '@/utils/format';
import type { OrderDetailVO, ReorderVO } from '@/types/api';

const cart = useCartStore();
const shop = useShopStore();

const order = ref<OrderDetailVO | null>(null);
let orderId = '';

/** 进行中（2~4）展示进度；2~5 已支付态展示取餐码 */
const inProgress = computed(() => {
  const s = order.value?.status ?? 0;
  return s >= 2 && s <= 5;
});
const steps = computed(() => orderSteps(order.value?.status ?? 2));

async function load(): Promise<void> {
  order.value = await orderApi.detail(orderId);
}

function goPay() {
  if (!order.value || order.value.status !== 1) return;
  uni.redirectTo({ url: `/pages/pay-result/pay-result?orderId=${order.value.id}` });
}

function cancelOrder() {
  if (!order.value || order.value.status !== 1) return;
  uni.showModal({
    title: '取消订单',
    content: '确认取消该待支付订单吗？库存将自动回补。',
    success: async (r) => {
      if (!r.confirm) return;
      await orderApi.cancel(order.value!.id);
      uni.showToast({ title: '已取消', icon: 'none' });
      load().catch(() => undefined);
    },
  });
}

async function reorder() {
  if (!order.value) return;
  let vo: ReorderVO;
  try {
    vo = await orderApi.reorder(order.value.id);
  } catch {
    return;
  }
  if (vo.skipped.length > 0) {
    uni.showToast({ title: `已跳过：${vo.skipped.map((s) => s.productName).join('、')}`, icon: 'none' });
  }
  if (vo.items.length === 0) return;
  const storeName = shop.stores.find((s) => s.id === vo.storeId)?.storeName ?? '';
  const go = () => {
    cart.queueReorder(vo.items);
    cart.switchStore({ id: vo.storeId, storeName });
    uni.navigateTo({ url: '/pages/menu/menu' });
  };
  if (cart.lines.length > 0 && cart.storeId && cart.storeId !== vo.storeId) {
    uni.showModal({
      title: '切换门店',
      content: '再来一单将切换回原门店并清空当前购物车，是否继续？',
      success: (r) => r.confirm && go(),
    });
  } else {
    go();
  }
}

onLoad((opts) => {
  orderId = opts?.id ?? '';
  if (!orderId) {
    uni.showToast({ title: '缺少订单', icon: 'none' });
    return;
  }
  load().catch(() => undefined);
});

// 从收银台支付完成 redirectTo 回本页时刷新
onShow(() => {
  if (orderId && order.value) load().catch(() => undefined);
});
</script>

<template>
  <view class="page">
    <view v-if="order">
      <!-- 状态区 -->
      <view class="status-head">
        <StateTag :status="order.status" />
        <text class="s-no">{{ order.orderNo }}</text>
      </view>
      <view v-if="order.status === 6 || order.status === 7" class="cell reason">
        {{ order.status === 6 ? '取消原因' : '退款说明' }}：{{ order.cancelReason || '—' }}
      </view>

      <!-- 取餐码 -->
      <view v-if="inProgress && order.takeCode" class="code-card">
        <view class="cl">取餐码</view>
        <view class="code">{{ order.takeCode }}</view>
        <view class="cs">门店接单后请凭取餐码取餐</view>
      </view>

      <!-- 进度 -->
      <view v-if="inProgress" class="steps">
        <view v-for="(s, i) in steps" :key="i" class="st" :class="s.state">
          <view class="c">{{ s.state === 'done' ? '✓' : i + 1 }}</view>
          {{ s.label }}
        </view>
      </view>

      <!-- 商品明细 -->
      <view class="cell">
        <view class="cell-title">商品清单</view>
        <view v-for="(it, i) in order.items" :key="i" class="row">
          <text class="l item-name">{{ it.productName }}（{{ it.specDesc || '标准' }}）×{{ it.qty }}</text>
          <text class="r">{{ fen2yuan(it.price * it.qty) }}</text>
        </view>
      </view>

      <!-- 金额 -->
      <view class="cell">
        <view class="row"><text class="l">商品金额</text><text class="r">{{ fen2yuan(order.totalAmount) }}</text></view>
        <view class="row"><text class="l">优惠合计</text><text class="r green">-{{ fen2yuan(order.discountAmount) }}</text></view>
        <view class="row sum-row"><text class="l strong">实付金额</text><text class="r amber big">{{ fen2yuan(order.payAmount) }}</text></view>
      </view>

      <!-- 订单信息 -->
      <view class="cell">
        <view class="cell-title">订单信息</view>
        <view class="row"><text class="l">下单时间</text><text class="r">{{ formatDateTime(order.createTime) }}</text></view>
        <view class="row"><text class="l">支付时间</text><text class="r">{{ formatDateTime(order.payTime) }}</text></view>
        <view class="row"><text class="l">支付方式</text><text class="r">{{ order.payChannel ? PAY_CHANNEL[order.payChannel] ?? '—' : '—' }}</text></view>
      </view>

      <!-- 操作 -->
      <view class="btns">
        <template v-if="order.status === 1">
          <button class="btn gray" @tap="cancelOrder">取消订单</button>
          <button class="btn" @tap="goPay">继续支付</button>
        </template>
        <button v-else-if="order.status >= 2 && order.status !== 6" class="btn" @tap="reorder">再来一单</button>
      </view>
    </view>

    <view v-else class="empty">订单加载中…</view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding-bottom: 30px;
}
.status-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 14px 14px 0;
  padding: 14px 16px;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
}
.s-no {
  font-size: 12px;
  color: var(--ink2);
}
.reason {
  color: var(--red);
  font-size: 12px;
}
.code-card {
  background: var(--paper);
  border: 2px dashed var(--amber);
  border-radius: var(--radius);
  margin: 12px 20px;
  padding: 20px 16px;
  text-align: center;
}
.cl {
  font-size: 12px;
  color: var(--ink2);
}
.code {
  font-size: 44px;
  font-weight: 800;
  color: var(--amber-deep);
  letter-spacing: 0.12em;
  margin-top: 6px;
  font-variant-numeric: tabular-nums;
}
.cs {
  font-size: 12px;
  color: var(--green);
  font-weight: 600;
  margin-top: 8px;
}
.steps {
  display: flex;
  justify-content: space-between;
  margin: 12px 24px 2px;
  font-size: 11px;
  color: var(--ink2);
}
.steps .st {
  text-align: center;
  flex: 1;
}
.steps .st .c {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #e7dfce;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 6px;
  font-size: 11px;
}
.steps .st.done .c {
  background: var(--green);
}
.steps .st.now .c {
  background: var(--amber);
  box-shadow: 0 0 0 4px var(--amber-soft);
}
.item-name {
  max-width: 60vw;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.green {
  color: var(--green);
}
.sum-row {
  border-top: 1px dashed var(--line);
  margin-top: 6px;
  padding-top: 10px;
}
.strong {
  font-weight: 700;
  color: var(--ink);
}
.big {
  font-size: 20px;
}
.btns {
  display: flex;
  gap: 14px;
  margin: 20px 14px 0;
}
.btns .btn {
  flex: 1;
}
</style>
