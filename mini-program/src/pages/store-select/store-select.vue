<script setup lang="ts">
/**
 * 选门店（F-APP-01，原型 s-store）
 * - 定位 → nearby（公开接口）；营业状态前端按 openTime/closeTime 判定
 * - 已选过店时「点单」tab 落本页 → onShow 直跳 menu
 * - 切换门店若购物车非空且跨店 → 确认后清空
 */

import { onShow } from '@dcloudio/uni-app';
import { computed, ref } from 'vue';
import { useShopStore } from '@/stores/shop';
import { useCartStore } from '@/stores/cart';
import { formatDistance } from '@/utils/format';
import type { StoreNearbyVO } from '@/types/api';

const shop = useShopStore();
const cart = useCartStore();
const jumping = ref(false);

const openCount = computed(() => shop.stores.filter((s) => isOpen(s)).length);

function isOpen(s: StoreNearbyVO): boolean {
  if (!s.openTime || !s.closeTime) return true;
  const now = new Date();
  const cur = `${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`;
  const open = s.openTime.replace(':', '');
  const close = s.closeTime.replace(':', '');
  // 跨零点营业（如 08:00-02:00）按开>关判定
  return open <= close ? cur >= open && cur < close : cur >= open || cur < close;
}

function enterStore(s: StoreNearbyVO) {
  if (!isOpen(s)) {
    uni.showToast({ title: '门店打烊中，不可下单', icon: 'none' });
    return;
  }
  const doEnter = () => {
    shop.selectStore({ id: s.id, storeName: s.storeName });
    cart.switchStore({ id: s.id, storeName: s.storeName });
    uni.navigateTo({ url: '/pages/menu/menu' });
  };
  if (cart.lines.length > 0 && cart.storeId && cart.storeId !== s.id) {
    uni.showModal({
      title: '切换门店',
      content: '切换门店将清空当前购物车，是否继续？',
      success: (r) => r.confirm && doEnter(),
    });
  } else {
    doEnter();
  }
}

onShow(() => {
  shop.fetchNearby().catch(() => undefined);
  // tab「点单」落点：已选店直达菜单（一次直达防循环：jumping 标记本次会话只跳一次）
  if (!jumping.value && shop.selected && cart.storeId) {
    jumping.value = true;
    uni.navigateTo({
      url: '/pages/menu/menu',
      complete: () => (jumping.value = false),
    });
  }
});
</script>

<template>
  <view class="page">
    <view class="loc-banner">
      <view class="l1">📍 当前定位</view>
      <view class="l2">为您推荐最近门店</view>
      <view class="l3">找到 {{ openCount }} 家营业中门店</view>
    </view>

    <view class="sec-title">附近门店 <text class="sub">仅营业中可下单</text></view>

    <view v-if="shop.loading" class="empty">正在加载门店…</view>
    <EmptyState v-else-if="shop.stores.length === 0" text="附近暂无门店" />

    <view
      v-for="s in shop.stores"
      :key="s.id"
      class="store-card"
      :class="{ closed: !isOpen(s) }"
      @tap="enterStore(s)"
    >
      <view class="s1">
        <text class="name">{{ s.storeName }}</text>
        <text class="dist">{{ formatDistance(s.distanceMeters) }}</text>
      </view>
      <view class="s2">
        <view class="dot" :class="{ off: !isOpen(s) }" />
        <text>{{ isOpen(s) ? `营业中 ${s.openTime}–${s.closeTime} · 可在线点单` : '打烊中 · 不可下单' }}</text>
      </view>
      <view class="s3">{{ s.address }}</view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.page {
  padding-bottom: 30px;
  min-height: 100vh;
}
.loc-banner {
  background: linear-gradient(135deg, var(--amber), var(--amber-deep));
  color: #fff;
  padding: 18px;
  margin: 14px;
  border-radius: var(--radius);
}
.loc-banner .l1 {
  font-size: 13px;
  opacity: 0.9;
}
.loc-banner .l2 {
  font-size: 18px;
  font-weight: 700;
  margin-top: 4px;
}
.loc-banner .l3 {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 4px;
}
.sub {
  font-weight: 400;
  color: var(--ink2);
  font-size: 12px;
}
.store-card {
  background: var(--paper);
  margin: 0 14px 10px;
  border-radius: var(--radius);
  padding: 14px 16px;
  border: 1px solid var(--line);
}
.store-card.closed {
  opacity: 0.75;
}
.store-card .s1 {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.store-card .name {
  font-size: 15px;
  font-weight: 700;
}
.store-card .dist {
  font-size: 12px;
  color: var(--green);
  background: var(--green-soft);
  padding: 2px 8px;
  border-radius: 99px;
}
.store-card .s2 {
  font-size: 12px;
  color: var(--ink2);
  margin-top: 6px;
  display: flex;
  gap: 8px;
  align-items: center;
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--green);
}
.dot.off {
  background: #c9bfac;
}
.store-card .s3 {
  font-size: 11px;
  color: var(--ink2);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
