<script setup lang="ts">
/**
 * 订单卡片（原型 .order-card）
 * 契约限制：列表返回裸 OrdOrder（无商品摘要），o2 降级为单号/取餐码
 */

import { computed } from 'vue';
import { fen2yuan, formatDateTime, ORDER_STATUS } from '@/utils/format';
import type { OrdOrderLite } from '@/types/api';

const props = defineProps<{ order: OrdOrderLite }>();
const emit = defineEmits<{ (e: 'open'): void; (e: 'pay'): void; (e: 'reorder'): void }>();

const colorCls = computed(() => `c-${ORDER_STATUS[props.order.status]?.color ?? 'gray'}`);
</script>

<template>
  <view class="order-card" @tap="emit('open')">
    <view class="o1">
      <text class="time">{{ formatDateTime(order.createTime) }}</text>
      <text class="st" :class="colorCls">{{ ORDER_STATUS[order.status]?.text ?? '未知' }}</text>
    </view>
    <view class="o2">
      <view>订单号 {{ order.orderNo }}</view>
      <view v-if="order.takeCode" class="take">取餐码 {{ order.takeCode }}</view>
    </view>
    <view class="o3">
      <text class="amt">实付 <b>{{ fen2yuan(order.payAmount) }}</b></text>
      <view class="btns">
        <text v-if="order.status === 1" class="pill solid" @tap.stop="emit('pay')">立即支付</text>
        <text v-if="order.status >= 2" class="pill" @tap.stop="emit('reorder')">再来一单</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.order-card {
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  margin: 12px 14px 0;
  padding: 14px;
}
.o1 {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 700;
}
.o1 .st {
  font-size: 12px;
  font-weight: 600;
}
.c-amber {
  color: var(--amber-deep);
}
.c-green {
  color: var(--green);
}
.c-red {
  color: var(--red);
}
.c-gray {
  color: var(--ink2);
}
.o2 {
  font-size: 12px;
  color: var(--ink2);
  margin: 8px 0;
}
.o2 .take {
  margin-top: 2px;
  color: var(--amber-deep);
}
.o3 {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}
.o3 .amt b {
  color: var(--amber-deep);
}
.btns {
  display: flex;
  gap: 8px;
}
.pill {
  font-size: 12px;
  color: var(--amber-deep);
  border: 1px solid var(--amber);
  border-radius: 99px;
  padding: 4px 12px;
  background: var(--paper);
}
.pill.solid {
  background: var(--amber);
  color: #fff;
}
</style>
