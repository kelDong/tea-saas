<script setup lang="ts">
/** 底部购物车入口条（原型 menu 页 pay-bar 左侧购物车 + 去结算） */

defineProps<{ count: number; total: number }>();
const emit = defineEmits<{ (e: 'open'): void; (e: 'checkout'): void }>();
</script>

<template>
  <view class="cart-bar">
    <view class="left" @tap="emit('open')">
      <view class="cart-ico">
        🛒
        <view v-if="count > 0" class="badge">{{ count > 99 ? '99+' : count }}</view>
      </view>
      <view class="t">
        <text v-if="count === 0">购物车是空的</text>
        <text v-else class="total">¥{{ (total / 100).toFixed(2) }}</text>
      </view>
    </view>
    <button class="btn" :class="{ disabled: count === 0 }" :disabled="count === 0" @tap="emit('checkout')">
      去结算
    </button>
  </view>
</template>

<style scoped lang="scss">
.cart-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--paper);
  border-top: 1px solid var(--line);
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 10;
}
.left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cart-ico {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--ink);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  position: relative;
}
.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: var(--amber);
  border-radius: 50%;
  min-width: 18px;
  height: 18px;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}
.t {
  font-size: 13px;
  color: var(--ink2);
}
.t .total {
  color: var(--amber-deep);
  font-size: 18px;
  font-weight: 700;
}
</style>
