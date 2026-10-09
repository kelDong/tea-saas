<script setup lang="ts">
/**
 * 收银台 + 支付成功/取餐码（F-PAY-01，原型 s-pay/s-success 合一）
 * - status=1：展示支付执行（微信单 wx-jsapi → mock 直连回调闭环；储值单创建即完成不会进入此态）
 * - 支付后轮询 detail 至 status>=2 → 成功页（取餐码 + 进度 steps）
 */

import { onLoad, onUnload } from '@dcloudio/uni-app';
import { computed, ref } from 'vue';
import { orderApi, payApi } from '@/api';
import { config } from '@/config';
import { fen2yuan, orderSteps } from '@/utils/format';
import type { OrderDetailVO } from '@/types/api';

const order = ref<OrderDetailVO | null>(null);
const paying = ref(false);
let pollTimer: ReturnType<typeof setTimeout> | null = null;
let pollCount = 0;

const paid = computed(() => (order.value?.status ?? 0) >= 2 && order.value!.status !== 6 && order.value!.status !== 7);
const steps = computed(() => orderSteps(order.value?.status ?? 2));

async function loadOrder(): Promise<void> {
  order.value = await orderApi.detail(orderId);
  uni.setNavigationBarTitle({ title: paid.value ? '支付成功' : '收银台' });
}

function pollStatus() {
  pollCount = 0;
  const tick = async () => {
    await loadOrder();
    if (paid.value || ++pollCount > 20) return; // ~30s 后停止
    pollTimer = setTimeout(tick, 1500);
  };
  pollTimer = setTimeout(tick, 1200);
}

async function payNow() {
  if (paying.value || !order.value) return;
  paying.value = true;
  try {
    const payment = await payApi.wxJsapi(orderId);
    if (config.useMockPay) {
      // DEV：资质未下发（tea.pay.mock=true），直连回调接口模拟网关确认 → 轮询订单状态
      await payApi.mockCallback(payment.paymentNo, payment.amount);
      pollStatus();
    } else {
      uni.requestPayment({
        provider: 'wxpay',
        timeStamp: payment.payParams?.timeStamp ?? '',
        nonceStr: payment.payParams?.nonceStr ?? '',
        package: payment.payParams?.package ?? '',
        signType: 'RSA',
        paySign: payment.payParams?.paySign ?? '',
        success: () => pollStatus(),
        fail: () => uni.showToast({ title: '支付未完成', icon: 'none' }),
      });
    }
  } finally {
    paying.value = false;
  }
}

function goDetail() {
  uni.redirectTo({ url: `/pages/order-detail/order-detail?id=${orderId}` });
}

function goHome() {
  uni.reLaunch({ url: '/pages/store-select/store-select' });
}

let orderId = '';

onLoad((opts) => {
  orderId = opts?.orderId ?? '';
  if (!orderId) {
    uni.showToast({ title: '缺少订单', icon: 'none' });
    return;
  }
  loadOrder().catch(() => undefined);
});

onUnload(() => {
  if (pollTimer) clearTimeout(pollTimer);
});
</script>

<template>
  <view class="page">
    <!-- 待支付：收银台 -->
    <view v-if="order && !paid">
      <view class="cell amount-cell">
        <view class="a-label">支付金额</view>
        <view class="a-num">{{ fen2yuan(order.payAmount) }}</view>
        <view class="a-sub">订单号 {{ order.orderNo }}</view>
      </view>
      <view class="cell pay-cell" @tap="payNow">
        <view class="p-ico">💚</view>
        <view class="p-info">
          <view class="p-name">微信支付</view>
          <view class="p-sub">{{ paying ? '支付发起中…' : '点击立即支付' }}</view>
        </view>
        <text class="arr">›</text>
      </view>
      <view class="cell mock-tip" v-if="config.useMockPay">
        <text>DEV 模式：点击上方微信支付将以 mock 网关直接确认（资质下发后自动切换真实支付）</text>
      </view>
    </view>

    <!-- 已支付：成功页 -->
    <view v-else-if="order">
      <view class="success-wrap">
        <view class="ok-ico">✓</view>
        <view class="h2">支付成功</view>
        <view class="p">订单号 {{ order.orderNo }} · 已推送门店制作</view>
      </view>

      <view class="code-card">
        <view class="cl">取餐码</view>
        <view class="code">{{ order.takeCode || '—' }}</view>
        <view class="cs">门店接单后请凭取餐码取餐</view>
      </view>

      <view class="steps">
        <view v-for="(s, i) in steps" :key="i" class="st" :class="s.state">
          <view class="c">{{ s.state === 'done' ? '✓' : i + 1 }}</view>
          {{ s.label }}
        </view>
      </view>

      <view class="btns">
        <button class="btn gray" @tap="goHome">返回首页</button>
        <button class="btn" @tap="goDetail">查看订单</button>
      </view>
    </view>

    <view v-else class="empty">订单加载中…</view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding-bottom: 40px;
}
.amount-cell {
  text-align: center;
  padding: 22px;
}
.a-label {
  font-size: 13px;
  color: var(--ink2);
}
.a-num {
  font-size: 34px;
  font-weight: 800;
  color: var(--amber-deep);
  margin: 8px 0;
}
.a-sub {
  font-size: 12px;
  color: var(--ink2);
}
.pay-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.p-ico {
  width: 36px;
  height: 36px;
  background: rgba(7, 193, 96, 0.15);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
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
}
.arr {
  color: var(--ink2);
}
.mock-tip {
  font-size: 11px;
  color: var(--ink2);
}

.success-wrap {
  text-align: center;
  padding: 48px 20px 20px;
}
.ok-ico {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--green-soft);
  color: var(--green);
  font-size: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
}
.h2 {
  font-size: 20px;
  font-weight: 700;
}
.success-wrap .p {
  color: var(--ink2);
  font-size: 13px;
  margin-top: 6px;
}
.code-card {
  background: var(--paper);
  border: 2px dashed var(--amber);
  border-radius: var(--radius);
  margin: 22px 20px;
  padding: 26px 16px;
  text-align: center;
}
.cl {
  font-size: 12px;
  color: var(--ink2);
}
.code {
  font-size: 52px;
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
  margin: 10px 24px;
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
.btns {
  display: flex;
  gap: 14px;
  margin: 24px 20px 0;
}
.btns .btn {
  flex: 1;
}
</style>
