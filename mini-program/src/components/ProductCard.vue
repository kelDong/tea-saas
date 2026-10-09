<script setup lang="ts">
/** 商品卡（原型 .g-card；可售性由接口保证，无售罄态） */

import type { AppProductVO } from '@/types/api';

defineProps<{ product: AppProductVO; qtyInCart?: number }>();
const emit = defineEmits<{ (e: 'tap'): void }>();

function specSummary(p: AppProductVO): string {
  const groups = p.specs ?? [];
  return groups.length ? groups.map((g) => g.specName).join(' / ') : p.description || '';
}
</script>

<template>
  <view class="g-card" @tap="emit('tap')">
    <image v-if="product.image" class="g-img" :src="product.image" mode="aspectFill" />
    <view v-else class="g-img emoji">🧋</view>
    <view class="g-info">
      <view class="g-name">{{ product.productName }}</view>
      <view class="g-spec">{{ specSummary(product) }}</view>
      <view class="g-row">
        <view class="g-price-wrap">
          <text class="g-price">¥{{ (product.salePrice / 100).toFixed(2) }}</text>
          <text v-if="qtyInCart" class="in-cart">已加 {{ qtyInCart }}</text>
        </view>
        <view class="add-btn">＋</view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.g-card {
  display: flex;
  gap: 12px;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 12px;
  margin-bottom: 10px;
}
.g-img {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: var(--amber-soft);
  flex-shrink: 0;
}
.g-img.emoji {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
}
.g-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.g-name {
  font-size: 14px;
  font-weight: 700;
}
.g-spec {
  font-size: 11px;
  color: var(--ink2);
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.g-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-top: auto;
}
.g-price-wrap {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.g-price {
  color: var(--amber-deep);
  font-weight: 700;
  font-size: 15px;
}
.in-cart {
  font-size: 10px;
  color: var(--green);
}
.add-btn {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--amber);
  color: #fff;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
