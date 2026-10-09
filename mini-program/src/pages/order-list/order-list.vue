<script setup lang="ts">
/**
 * 历史订单（F-APP-03，原型 s-orders）
 * - GET /orders 分页（无 status 入参）→ 前端状态 tab 过滤 + onReachBottom 翻页
 * - 再来一单：POST /orders/{id}/reorder → skipped 提示 → 队列重选规格跳 menu
 */

import { onLoad, onReachBottom } from '@dcloudio/uni-app';
import { computed, ref } from 'vue';
import { orderApi } from '@/api';
import { useCartStore } from '@/stores/cart';
import { useShopStore } from '@/stores/shop';
import OrderCard from '@/components/OrderCard.vue';
import EmptyState from '@/components/EmptyState.vue';
import type { OrdOrderLite, ReorderVO } from '@/types/api';

const cart = useCartStore();
const shop = useShopStore();

const TABS: { label: string; status?: number[] }[] = [
  { label: '全部' },
  { label: '进行中', status: [1, 2, 3, 4] },
  { label: '已完成', status: [5] },
  { label: '取消/退款', status: [6, 7] },
];
const activeTab = ref(0);

const list = ref<OrdOrderLite[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);

const filtered = computed(() => {
  const st = TABS[activeTab.value].status;
  return st ? list.value.filter((o) => st.includes(o.status)) : list.value;
});

async function load(reset = false): Promise<void> {
  if (loading.value) return;
  if (reset) {
    page.value = 1;
    list.value = [];
  } else if (list.value.length >= total.value && list.value.length > 0) {
    return;
  }
  loading.value = true;
  try {
    const res = await orderApi.page(page.value);
    total.value = res.total;
    if (page.value === 1) list.value = res.list;
    else list.value.push(...res.list);
    page.value += 1;
  } catch {
    // 静默登录竞态等场景由请求层 toast，这里仅终止本轮
  } finally {
    loading.value = false;
  }
}

function switchTab(i: number) {
  if (activeTab.value === i) return;
  activeTab.value = i;
}

function goDetail(id: string) {
  uni.navigateTo({ url: `/pages/order-detail/order-detail?id=${id}` });
}

function goPay(id: string) {
  uni.navigateTo({ url: `/pages/pay-result/pay-result?orderId=${id}` });
}

/** 再来一单（列表卡片与详情页共用口径） */
async function reorder(id: string) {
  let vo: ReorderVO;
  try {
    vo = await orderApi.reorder(id);
  } catch {
    return; // 请求层已 toast
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

onLoad(() => load(true));
onReachBottom(() => load());
</script>

<template>
  <view class="page">
    <view class="tabs">
      <view v-for="(t, i) in TABS" :key="t.label" class="tab" :class="{ on: activeTab === i }" @tap="switchTab(i)">
        {{ t.label }}
      </view>
    </view>

    <OrderCard
      v-for="o in filtered"
      :key="o.id"
      :order="o"
      @open="goDetail(o.id)"
      @pay="goPay(o.id)"
      @reorder="reorder(o.id)"
    />

    <EmptyState v-if="!loading && filtered.length === 0" text="暂无相关订单" />
    <view v-if="loading" class="loading">加载中…</view>
    <view v-else-if="filtered.length > 0 && list.length >= total" class="loading">没有更多了</view>
    <view class="tail" />
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding-bottom: 20px;
}
.tabs {
  display: flex;
  background: var(--paper);
  border-bottom: 1px solid var(--line);
  position: sticky;
  top: 0;
  z-index: 5;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 13px;
  color: var(--ink2);
  position: relative;
}
.tab.on {
  color: var(--amber-deep);
  font-weight: 700;
}
.tab.on::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 0;
  width: 24px;
  height: 3px;
  border-radius: 3px;
  background: var(--amber);
}
.loading {
  text-align: center;
  color: var(--ink2);
  font-size: 12px;
  padding: 14px 0;
}
.tail {
  height: 10px;
}
</style>
