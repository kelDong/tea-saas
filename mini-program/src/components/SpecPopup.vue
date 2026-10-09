<script setup lang="ts">
/**
 * 规格弹层（原型 .mask/.sheet）
 * - 必选组默认选第一个选项（isRequired=1），可选组默认不选
 * - 加价实时合计：unitPrice = salePrice + Σ extraPrice
 * - 确认回调由页面执行入车（含合并）
 */

import { computed, ref, watch } from 'vue';
import type { AppProductVO, SpecNode } from '@/types/api';

const props = defineProps<{ show: boolean; product: AppProductVO | null }>();
const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'confirm', payload: { specOptionIds: string[]; specDesc: string; unitPrice: number; qty: number }): void;
}>();

const selected = ref<Record<string, string | null>>({}); // specGroupId -> optionId
const qty = ref(1);

watch(
  () => [props.show, props.product?.listingId] as const,
  ([show]) => {
    if (show && props.product) {
      qty.value = 1;
      const init: Record<string, string | null> = {};
      for (const g of props.product.specs ?? []) {
        init[g.id] = g.isRequired === 1 ? (g.options[0]?.id ?? null) : null;
      }
      selected.value = init;
    }
  },
);

const groups = computed<SpecNode[]>(() => props.product?.specs ?? []);

const unitPrice = computed(() => {
  if (!props.product) return 0;
  let extra = 0;
  for (const g of groups.value) {
    const optId = selected.value[g.id];
    const opt = g.options.find((o) => o.id === optId);
    if (opt) extra += opt.extraPrice ?? 0;
  }
  return props.product.salePrice + extra;
});

function pick(groupId: string, optionId: string) {
  selected.value = { ...selected.value, [groupId]: optionId };
}

function missingRequired(): SpecNode | null {
  return groups.value.find((g) => g.isRequired === 1 && !selected.value[g.id]) ?? null;
}

function confirm() {
  if (!props.product) return;
  const miss = missingRequired();
  if (miss) {
    uni.showToast({ title: `请选择「${miss.specName}」`, icon: 'none' });
    return;
  }
  const ids: string[] = [];
  const names: string[] = [];
  for (const g of groups.value) {
    const opt = g.options.find((o) => o.id === selected.value[g.id]);
    if (opt) {
      ids.push(opt.id);
      names.push(opt.optionName);
    }
  }
  emit('confirm', {
    specOptionIds: ids,
    specDesc: names.join('/'),
    unitPrice: unitPrice.value,
    qty: qty.value,
  });
}
</script>

<template>
  <view v-if="show && product" class="mask" @tap="emit('close')">
    <view class="sheet" @tap.stop>
      <view class="sh1">
        <image v-if="product.image" class="big-img" :src="product.image" mode="aspectFill" />
        <view v-else class="big-img emoji">🧋</view>
        <view>
          <view class="sh-name">{{ product.productName }}</view>
          <view class="sh-price">¥{{ (unitPrice / 100).toFixed(2) }}</view>
        </view>
      </view>

      <view v-for="g in groups" :key="g.id" class="opt-group">
        <view class="og-title">
          {{ g.specName }}
          <text v-if="g.isRequired === 1" class="og-tag">必选</text>
          <text v-else class="og-tag">可选 · 加价</text>
        </view>
        <view class="chips">
          <view
            v-for="o in g.options"
            :key="o.id"
            class="chip"
            :class="{ on: selected[g.id] === o.id }"
            @tap="pick(g.id, o.id)"
          >
            {{ o.optionName }}<text v-if="o.extraPrice > 0" class="extra"> +¥{{ (o.extraPrice / 100).toFixed(0) }}</text>
          </view>
        </view>
      </view>

      <view class="qty-row">
        <text class="qty-label">数量</text>
        <view class="qty-box">
          <view class="qbtn" @tap="qty = Math.max(1, qty - 1)">−</view>
          <text class="q">{{ qty }}</text>
          <view class="qbtn plus" @tap="qty++">＋</view>
        </view>
      </view>

      <button class="add-cart" @tap="confirm">加入购物车</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.mask {
  position: fixed;
  inset: 0;
  background: rgba(42, 32, 22, 0.5);
  z-index: 20;
  display: flex;
  align-items: flex-end;
}
.sheet {
  background: var(--paper);
  border-radius: 24px 24px 0 0;
  padding: 22px 18px 18px;
  width: 100%;
  max-height: 78vh;
  overflow-y: auto;
}
.sh1 {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
}
.big-img {
  width: 72px;
  height: 72px;
  border-radius: 14px;
  background: var(--amber-soft);
}
.big-img.emoji {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}
.sh-name {
  font-size: 15px;
  font-weight: 700;
  margin-top: 4px;
}
.sh-price {
  color: var(--amber-deep);
  font-weight: 700;
  font-size: 18px;
  margin-top: 6px;
}
.opt-group {
  margin-bottom: 14px;
}
.og-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
}
.og-tag {
  color: var(--ink2);
  font-weight: 400;
  margin-left: 6px;
  font-size: 11px;
}
.extra {
  font-size: 11px;
}
.qty-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}
.qty-label {
  font-size: 14px;
  font-weight: 600;
}
.qty-box {
  display: flex;
  align-items: center;
  gap: 14px;
}
.qbtn {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: #fff;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.qbtn.plus {
  background: var(--amber);
  border-color: var(--amber);
  color: #fff;
}
.q {
  font-weight: 700;
  min-width: 20px;
  text-align: center;
}
.add-cart {
  background: var(--amber);
  color: #fff;
  border: none;
  border-radius: 99px;
  padding: 14px;
  font-size: 15px;
  font-weight: 700;
  width: 100%;
  margin-top: 14px;
}
</style>
