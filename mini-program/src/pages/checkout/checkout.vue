<script setup lang="ts">
/**
 * 确认订单（F-ORD-01，原型 s-checkout）
 * - 进页 POST /orders/preview 驱动金额区；券/积分变更即时重试算
 * - payChannel 在本页选定（下单入参口径：1微信/2储值；储值须余额≥应付，P0-4 不支持混付）
 * - 提交 POST /orders → pay-result
 */

import { onLoad } from '@dcloudio/uni-app';
import { computed, ref } from 'vue';
import { couponApi, orderApi } from '@/api';
import { useCartStore } from '@/stores/cart';
import { useMemberStore } from '@/stores/member';
import { fen2yuan, couponDesc } from '@/utils/format';
import type { CouponVO, OrderPreviewVO } from '@/types/api';

const cart = useCartStore();
const member = useMemberStore();

const preview = ref<OrderPreviewVO | null>(null);
const submitting = ref(false);

const coupons = ref<CouponVO[]>([]);
const couponShow = ref(false);
const couponId = ref<string | null>(null);

const usePoints = ref(false);
const payChannel = ref<1 | 2>(1);

const payAmount = computed(() => preview.value?.payAmount ?? cart.totalAmount);
const walletEnough = computed(() => member.walletBalance >= payAmount.value);
const selectedCoupon = computed(() => coupons.value.find((c) => c.id === couponId.value) ?? null);

async function refresh() {
  const dto = {
    storeId: cart.storeId!,
    items: cart.lines.map((l) => ({ listingId: l.listingId, qty: l.qty, specOptionIds: l.specOptionIds })),
    ...(couponId.value ? { couponId: couponId.value } : {}),
    ...(usePoints.value ? { pointsUsed: member.availablePoints } : {}),
  };
  try {
    preview.value = await orderApi.preview(dto);
  } catch (e) {
    // 券不可用（如指定商品券无适用商品）等场景：清除券重试一次
    if (couponId.value) {
      couponId.value = null;
      preview.value = await orderApi.preview(dto);
    } else {
      throw e;
    }
  }
}

async function loadCoupons() {
  const page = await couponApi.my(1, 1, 50);
  coupons.value = page.list;
}

function pickCoupon(c: CouponVO | null) {
  couponId.value = c?.id ?? null;
  couponShow.value = false;
  refresh().catch(() => undefined);
}

function togglePoints() {
  usePoints.value = !usePoints.value;
  refresh().catch(() => undefined);
}

function couponUsable(c: CouponVO): boolean {
  return c.threshold <= (preview.value?.totalAmount ?? cart.totalAmount);
}

async function submit() {
  if (submitting.value || !preview.value) return;
  if (payChannel.value === 2 && !walletEnough.value) {
    uni.showToast({ title: '储值余额不足', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    const vo = await orderApi.create({
      storeId: cart.storeId!,
      items: cart.lines.map((l) => ({ listingId: l.listingId, qty: l.qty, specOptionIds: l.specOptionIds })),
      ...(couponId.value ? { couponId: couponId.value } : {}),
      ...(usePoints.value ? { pointsUsed: preview.value.pointsUsed } : {}),
      payChannel: payChannel.value,
    });
    cart.clearLines();
    uni.redirectTo({ url: `/pages/pay-result/pay-result?orderId=${vo.id}` });
  } finally {
    submitting.value = false;
  }
}

onLoad(() => {
  if (cart.lines.length === 0) {
    uni.showToast({ title: '购物车为空', icon: 'none' });
    setTimeout(() => uni.navigateBack(), 600);
    return;
  }
  member.fetchMe().catch(() => undefined); // 储值渠道余额判定
  loadCoupons().catch(() => undefined);
  refresh().catch(() => undefined);
});
</script>

<template>
  <view class="page">
    <view class="cell">
      <view class="row"><text class="l">自取门店</text><text class="r">{{ cart.storeName }}</text></view>
      <view class="row"><text class="l">取餐方式</text><text class="r">到店自取</text></view>
    </view>

    <view class="cell">
      <view class="cell-title">商品清单 <text class="ct-sub">{{ cart.count }} 件商品</text></view>
      <view v-for="(l, i) in cart.lines" :key="i" class="row">
        <text class="l item-name">{{ l.productName }}（{{ l.specDesc || '标准' }}）×{{ l.qty }}</text>
        <text class="r">{{ fen2yuan(l.unitPrice * l.qty) }}</text>
      </view>
    </view>

    <view class="cell">
      <view class="row tap" @tap="couponShow = true">
        <text class="l">优惠券</text>
        <text class="r" :class="{ amber: selectedCoupon }">
          {{ selectedCoupon ? `${selectedCoupon.name}（${couponDesc(selectedCoupon)}）` : `${coupons.length} 张可用 ›` }}
        </text>
      </view>
      <view class="row tap" @tap="togglePoints">
        <text class="l">积分抵扣</text>
        <text class="r" :class="{ amber: usePoints }">
          {{ usePoints ? `已抵 ${preview?.pointsUsed ?? 0} 分` : `可用 ${member.availablePoints} 分 ›` }}
        </text>
      </view>
    </view>

    <view class="cell">
      <view class="row"><text class="l">商品金额</text><text class="r">{{ fen2yuan(preview?.totalAmount) }}</text></view>
      <view class="row"><text class="l">优惠券</text><text class="r green">-{{ fen2yuan(preview?.couponAmount ?? 0) }}</text></view>
      <view class="row"><text class="l">积分抵扣</text><text class="r green">-{{ fen2yuan(preview?.pointsAmount ?? 0) }}</text></view>
      <view class="row sum-row"><text class="l strong">应付金额</text><text class="r amber big">{{ fen2yuan(payAmount) }}</text></view>
    </view>

    <view class="cell">
      <view class="cell-title">支付方式</view>
      <view class="pay-opt" :class="{ on: payChannel === 1 }" @tap="payChannel = 1">
        <view class="p-ico wx">💚</view>
        <view class="p-info"><view class="p-name">微信支付</view><view class="p-sub">推荐使用</view></view>
        <view class="radio" :class="{ on: payChannel === 1 }" />
      </view>
      <view class="pay-opt" :class="{ on: payChannel === 2, disabled: !walletEnough }" @tap="walletEnough && (payChannel = 2)">
        <view class="p-ico wallet">💰</view>
        <view class="p-info">
          <view class="p-name">储值支付 <text v-if="!walletEnough" class="p-warn">余额不足</text></view>
          <view class="p-sub">余额 {{ fen2yuan(member.walletBalance) }} · 不支持混付</view>
        </view>
        <view class="radio" :class="{ on: payChannel === 2 }" />
      </view>
    </view>

    <view class="pay-bar">
      <view class="t">
        应付 <b>{{ fen2yuan(payAmount) }}</b>
        <view class="tip">15 分钟未支付自动取消并回补库存</view>
      </view>
      <button class="btn" :class="{ disabled: submitting }" :disabled="submitting" @tap="submit">
        {{ submitting ? '提交中…' : '提交订单' }}
      </button>
    </view>

    <!-- 券选择弹层 -->
    <view v-if="couponShow" class="mask" @tap="couponShow = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">选择优惠券</view>
        <scroll-view class="coupon-list" scroll-y>
          <view class="coupon-none" :class="{ on: !couponId }" @tap="pickCoupon(null)">不使用优惠券</view>
          <view
            v-for="c in coupons"
            :key="c.id"
            class="coupon-item"
            :class="{ on: couponId === c.id, unusable: !couponUsable(c) }"
            @tap="couponUsable(c) && pickCoupon(c)"
          >
            <view class="c-left">
              <view class="c-name">{{ c.name }}</view>
              <view class="c-desc">{{ couponDesc(c) }}<text v-if="c.scopeType === 2" class="c-scope"> · 限指定商品</text></view>
              <view class="c-valid">有效期至 {{ c.validEnd.slice(0, 10) }}</view>
            </view>
            <view v-if="!couponUsable(c)" class="c-lock">差 {{ fen2yuan(c.threshold - (preview?.totalAmount ?? cart.totalAmount)) }}</view>
            <view v-else class="radio" :class="{ on: couponId === c.id }" />
          </view>
          <EmptyState v-if="coupons.length === 0" text="暂无可用优惠券" />
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.page {
  padding-bottom: 120px;
  min-height: 100vh;
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
.tap {
  cursor: pointer;
}

.pay-opt {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 8px;
  border-radius: 12px;
}
.pay-opt.on {
  background: var(--amber-soft);
}
.pay-opt.disabled {
  opacity: 0.55;
}
.p-ico {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}
.p-ico.wx {
  background: rgba(7, 193, 96, 0.15);
}
.p-ico.wallet {
  background: var(--green-soft);
}
.p-info {
  flex: 1;
}
.p-name {
  font-size: 14px;
  font-weight: 700;
}
.p-sub {
  font-size: 11px;
  color: var(--ink2);
  margin-top: 2px;
}
.p-warn {
  font-size: 11px;
  color: var(--red);
}
.radio {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid var(--line);
  flex-shrink: 0;
}
.radio.on {
  border-color: var(--amber);
  background: var(--amber);
  box-shadow: inset 0 0 0 3px #fff;
}

.pay-bar {
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
.pay-bar .t {
  font-size: 13px;
  color: var(--ink2);
}
.pay-bar .t b {
  color: var(--amber-deep);
  font-size: 18px;
}
.tip {
  font-size: 10px;
  color: var(--ink2);
}

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
  padding: 20px 18px 18px;
  width: 100%;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
}
.sheet-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 12px;
}
.coupon-list {
  max-height: 52vh;
}
.coupon-none {
  padding: 14px;
  border-radius: 12px;
  border: 1px solid var(--line);
  margin-bottom: 10px;
  font-size: 14px;
  text-align: center;
}
.coupon-none.on {
  border-color: var(--amber);
  color: var(--amber-deep);
  font-weight: 600;
  background: var(--amber-soft);
}
.coupon-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 10px;
}
.coupon-item.on {
  border-color: var(--amber);
  background: var(--amber-soft);
}
.coupon-item.unusable {
  opacity: 0.6;
}
.c-name {
  font-size: 14px;
  font-weight: 700;
}
.c-desc {
  font-size: 12px;
  color: var(--amber-deep);
  margin-top: 4px;
}
.c-scope {
  color: var(--ink2);
}
.c-valid {
  font-size: 11px;
  color: var(--ink2);
  margin-top: 4px;
}
.c-lock {
  font-size: 11px;
  color: var(--red);
  flex-shrink: 0;
}
</style>
