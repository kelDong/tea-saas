<script setup lang="ts">
/** 购物车抽屉（原型 .cart-drawer/.cart-box） */

import type { CartLine } from '@/stores/cart';

defineProps<{ show: boolean; lines: CartLine[]; total: number }>();
const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'change', index: number, delta: number): void;
  (e: 'clear'): void;
  (e: 'checkout'): void;
}>();
</script>

<template>
  <view v-if="show" class="mask" @tap="emit('close')">
    <view class="cart-box" @tap.stop>
      <view class="cb-title">
        <text>购物车</text>
        <view class="cb-actions">
          <text v-if="lines.length" class="clear" @tap="emit('clear')">清空</text>
          <text class="close" @tap="emit('close')">✕</text>
        </view>
      </view>
      <scroll-view class="cart-items" scroll-y>
        <view v-if="lines.length === 0" class="empty">购物车是空的</view>
        <view v-for="(l, i) in lines" :key="i" class="ci">
          <view class="ci-left">
            <view class="n">{{ l.productName }}</view>
            <view class="s">{{ l.specDesc || '标准' }} · ¥{{ (l.unitPrice / 100).toFixed(2) }}</view>
          </view>
          <view class="ci-right">
            <text class="p">¥{{ ((l.unitPrice * l.qty) / 100).toFixed(2) }}</text>
            <view class="qbtn" @tap="emit('change', i, -1)">−</view>
            <text class="qty">{{ l.qty }}</text>
            <view class="qbtn plus" @tap="emit('change', i, 1)">＋</view>
          </view>
        </view>
      </scroll-view>
      <view class="cart-foot">
        <view class="cart-total">合计 <b>¥{{ (total / 100).toFixed(2) }}</b></view>
        <button class="btn" :class="{ disabled: lines.length === 0 }" :disabled="lines.length === 0" @tap="emit('checkout')">
          去结算
        </button>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.mask {
  position: fixed;
  inset: 0;
  background: rgba(42, 32, 22, 0.5);
  z-index: 30;
  display: flex;
  align-items: flex-end;
}
.cart-box {
  background: var(--paper);
  border-radius: 24px 24px 0 0;
  padding: 18px;
  width: 100%;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
}
.cb-title {
  font-size: 16px;
  font-weight: 700;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--line);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.cb-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}
.clear {
  font-size: 12px;
  color: var(--ink2);
  font-weight: 400;
}
.close {
  font-size: 14px;
  color: var(--ink2);
}
.cart-items {
  flex: 1;
  overflow-y: auto;
  max-height: 46vh;
}
.ci {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px dashed var(--line);
}
.ci .n {
  font-size: 14px;
  font-weight: 600;
}
.ci .s {
  font-size: 11px;
  color: var(--ink2);
  margin-top: 2px;
}
.ci-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ci .p {
  color: var(--amber-deep);
  font-weight: 700;
  font-size: 13px;
}
.qty {
  min-width: 16px;
  text-align: center;
  font-weight: 600;
}
.qbtn {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: #fff;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.qbtn.plus {
  background: var(--amber);
  border-color: var(--amber);
  color: #fff;
}
.cart-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
}
.cart-total {
  font-size: 13px;
  color: var(--ink2);
}
.cart-total b {
  color: var(--amber-deep);
  font-size: 18px;
}
</style>
