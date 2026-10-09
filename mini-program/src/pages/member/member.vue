<script setup lang="ts">
/**
 * 会员中心（F-MEM-01/02/03 + F-MKT-01，原型 s-mine）
 * - 渐变头（昵称/等级）+ 资产三格（积分/储值/券数）
 * - 页内 tab：券包（三态）/ 积分明细 / 储值（固定档充值 mock 即时到账 + 流水）
 * - 绑定手机号（手输表单）；领券一期为「输入模板ID领取」调试入口（无 C 端模板列表接口）
 */

import { onShow } from '@dcloudio/uni-app';
import { computed, ref } from 'vue';
import EmptyState from '@/components/EmptyState.vue';
import { couponApi, memberApi } from '@/api';
import { useMemberStore } from '@/stores/member';
import { couponDesc, fen2yuan, formatDateTime } from '@/utils/format';
import type { CouponVO, PointsLog, RechargeRecord, WalletVO } from '@/types/api';

const member = useMemberStore();

const TABS = ['券包', '积分', '储值'] as const;
const activeTab = ref(0);

/** 券包 */
const COUPON_TABS: { label: string; status: number }[] = [
  { label: '可用', status: 1 },
  { label: '已用', status: 2 },
  { label: '已过期', status: 3 },
];
const couponTab = ref(0);
const coupons = ref<CouponVO[]>([]);
const couponTotal = ref(0);
const claimId = ref('');

/** 积分 */
const POINTS_BIZ: Record<number, string> = { 1: '消费得积分', 2: '积分抵扣', 3: '抵扣回补', 4: '积分过期' };
const points = ref<PointsLog[]>([]);

/** 储值 */
const TIERS = [5000, 10000, 20000, 50000]; // 分
const wallet = ref<WalletVO | null>(null);
const walletLogs = ref<RechargeRecord[]>([]);
const tierIdx = ref(1);
const recharging = ref(false);

/** 手机号 */
const phoneShow = ref(false);
const phoneInput = ref('');
const phoneSaving = ref(false);

const maskedPhone = computed(() => {
  const p = member.member?.phone;
  return p ? p.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '';
});

async function loadCoupons(): Promise<void> {
  const res = await couponApi.my(COUPON_TABS[couponTab.value].status, 1, 50);
  coupons.value = res.list;
}

async function loadPoints(): Promise<void> {
  const res = await memberApi.points(1, 50);
  points.value = res.list;
}

async function loadWallet(): Promise<void> {
  wallet.value = await memberApi.wallet();
  const res = await memberApi.walletLogs(1, 50);
  walletLogs.value = res.list;
}

function switchTab(i: number) {
  if (activeTab.value === i) return;
  activeTab.value = i;
  // 懒加载 + 每次切回刷新
  if (i === 0) loadCoupons().catch(() => undefined);
  if (i === 1) loadPoints().catch(() => undefined);
  if (i === 2) loadWallet().catch(() => undefined);
}

function pickCouponTab(i: number) {
  couponTab.value = i;
  loadCoupons().catch(() => undefined);
}

async function claimBtn(): Promise<void> {
  try {
    await claim();
  } catch {
    // 请求层已 toast
  }
}

async function rechargeBtn(): Promise<void> {
  try {
    await recharge();
  } catch {
    // 请求层已 toast
  }
}

async function claim() {
  const id = claimId.value.trim();
  if (!id) {
    uni.showToast({ title: '请输入模板ID', icon: 'none' });
    return;
  }
  await couponApi.claim(id);
  uni.showToast({ title: '领取成功', icon: 'none' });
  claimId.value = '';
  await Promise.all([loadCoupons(), loadCount()]);
}

async function loadCount(): Promise<void> {
  couponTotal.value = (await couponApi.my(1, 1, 1)).total;
}

async function recharge() {
  if (recharging.value) return;
  recharging.value = true;
  try {
    await member.recharge(TIERS[tierIdx.value]);
    uni.showToast({ title: '充值成功', icon: 'none' });
    await loadWallet();
  } finally {
    recharging.value = false;
  }
}

async function savePhone() {
  const p = phoneInput.value.trim();
  if (!/^1\d{10}$/.test(p)) {
    uni.showToast({ title: '手机号格式不正确', icon: 'none' });
    return;
  }
  if (phoneSaving.value) return;
  phoneSaving.value = true;
  try {
    await member.bindPhone(p);
    phoneShow.value = false;
    uni.showToast({ title: '绑定成功', icon: 'none' });
  } finally {
    phoneSaving.value = false;
  }
}

onShow(() => {
  member.fetchMe().catch(() => undefined);
  loadCount().catch(() => undefined);
  if (activeTab.value === 0) loadCoupons().catch(() => undefined);
  if (activeTab.value === 1) loadPoints().catch(() => undefined);
  if (activeTab.value === 2) loadWallet().catch(() => undefined);
});
</script>

<template>
  <view class="page">
    <!-- 渐变头 -->
    <view class="mine-head">
      <image v-if="member.member?.avatar" class="avatar" :src="member.member.avatar" mode="aspectFill" />
      <view v-else class="avatar emoji">🧑</view>
      <view class="n">{{ member.member?.nickname || '茶友' }}</view>
      <view class="lvl">{{ member.member?.levelName || '普通会员' }}</view>
    </view>

    <!-- 资产三格 -->
    <view class="assets">
      <view class="a"><b>{{ member.availablePoints }}</b><view class="lbl">积分</view></view>
      <view class="a"><b>{{ fen2yuan(member.walletBalance) }}</b><view class="lbl">储值余额</view></view>
      <view class="a"><b>{{ couponTotal }}</b><view class="lbl">优惠券</view></view>
    </view>

    <!-- 手机号 -->
    <view class="cell phone-cell">
      <template v-if="!phoneShow">
        <view class="row tap" @tap="phoneShow = true">
          <text class="l">{{ maskedPhone ? '手机号' : '绑定手机号' }}</text>
          <text class="r" :class="{ amber: !maskedPhone }">{{ maskedPhone || '未绑定，点击绑定 ›' }}</text>
        </view>
      </template>
      <template v-else>
        <view class="cell-title">绑定手机号</view>
        <view class="phone-form">
          <input v-model="phoneInput" class="phone-input" type="number" maxlength="11" placeholder="请输入11位手机号" />
          <button class="btn mini" :disabled="phoneSaving" @tap="savePhone">{{ phoneSaving ? '保存中…' : '保存' }}</button>
        </view>
      </template>
    </view>

    <!-- 页内 tab -->
    <view class="tabs">
      <view v-for="(t, i) in TABS" :key="t" class="tab" :class="{ on: activeTab === i }" @tap="switchTab(i)">
        {{ t }}
      </view>
    </view>

    <!-- 券包 -->
    <view v-if="activeTab === 0">
      <view class="sub-tabs">
        <view
          v-for="(t, i) in COUPON_TABS"
          :key="t.label"
          class="sub-tab"
          :class="{ on: couponTab === i }"
          @tap="pickCouponTab(i)"
        >
          {{ t.label }}
        </view>
      </view>
      <view v-for="c in coupons" :key="c.id" class="coupon-item" :class="{ dead: c.status !== 1 }">
        <view class="c-left">
          <view class="c-name">{{ c.name }}</view>
          <view class="c-desc">{{ couponDesc(c) }}<text v-if="c.scopeType === 2" class="c-scope"> · 限指定商品</text></view>
          <view class="c-valid">{{ c.validStart.slice(0, 10) }} ~ {{ c.validEnd.slice(0, 10) }}</view>
        </view>
      </view>
      <EmptyState v-if="coupons.length === 0" text="暂无相关优惠券" />

      <!-- 领券调试入口（一期：无 C 端模板列表接口） -->
      <view class="cell">
        <view class="cell-title">领取优惠券 <text class="ct-sub">DEV：输入模板ID</text></view>
        <view class="phone-form">
          <input v-model="claimId" class="phone-input" placeholder="券模板ID（管理后台创建）" />
          <button class="btn mini" @tap="claimBtn">领取</button>
        </view>
      </view>
    </view>

    <!-- 积分明细 -->
    <view v-if="activeTab === 1">
      <view v-for="p in points" :key="p.id" class="log-row">
        <view class="l">
          <view class="t1">{{ POINTS_BIZ[p.bizType] ?? '积分变动' }} <text class="no">{{ p.bizNo }}</text></view>
          <view class="t2">{{ formatDateTime(p.createTime) }}</view>
        </view>
        <view class="delta" :class="{ plus: p.changePoints > 0 }">
          {{ p.changePoints > 0 ? '+' : '' }}{{ p.changePoints }}
          <text class="after">余 {{ p.afterPoints }}</text>
        </view>
      </view>
      <EmptyState v-if="points.length === 0" text="暂无积分明细" />
    </view>

    <!-- 储值 -->
    <view v-if="activeTab === 2">
      <view class="wallet-card">
        <view class="w-balance">{{ fen2yuan(wallet?.balance ?? member.walletBalance) }}</view>
        <view class="w-sub">当前余额</view>
        <view class="w-line">
          <text>累计充值 {{ fen2yuan(wallet?.totalRecharge) }}</text>
          <text>累计消费 {{ fen2yuan(wallet?.totalConsume) }}</text>
        </view>
      </view>
      <view class="cell">
        <view class="cell-title">选择充值金额</view>
        <view class="tier-grid">
          <view
            v-for="(t, i) in TIERS"
            :key="t"
            class="tier"
            :class="{ on: tierIdx === i }"
            @tap="tierIdx = i"
          >
            {{ fen2yuan(t) }}
          </view>
        </view>
        <button class="btn" :disabled="recharging" @tap="rechargeBtn">
          {{ recharging ? '充值中…' : '立即充值（DEV 直接到账）' }}
        </button>
      </view>
      <view class="cell">
        <view class="cell-title">充值记录</view>
        <view v-for="r in walletLogs" :key="r.id" class="log-row flat">
          <view class="l">
            <view class="t1">充值 {{ fen2yuan(r.amount) }}<text v-if="r.bonus > 0" class="bonus"> 赠 {{ fen2yuan(r.bonus) }}</text></view>
            <view class="t2">{{ formatDateTime(r.createTime) }}</view>
          </view>
          <view class="delta plus">余 {{ fen2yuan(r.balanceAfter) }}</view>
        </view>
        <EmptyState v-if="walletLogs.length === 0" text="暂无充值记录" />
      </view>
    </view>

    <view class="tail" />
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding-bottom: 30px;
}
.mine-head {
  background: linear-gradient(135deg, var(--amber), var(--amber-deep));
  color: #fff;
  padding: 26px 20px 22px;
  margin: 14px;
  border-radius: var(--radius);
  text-align: center;
}
.avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  margin: 0 auto 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  overflow: hidden;
}
.mine-head .n {
  font-size: 17px;
  font-weight: 700;
}
.mine-head .lvl {
  font-size: 12px;
  background: rgba(255, 255, 255, 0.22);
  display: inline-block;
  padding: 3px 12px;
  border-radius: 99px;
  margin-top: 6px;
}
.assets {
  display: flex;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  margin: 0 14px;
  padding: 14px 0;
}
.assets .a {
  flex: 1;
  text-align: center;
}
.assets .a b {
  font-size: 18px;
  color: var(--amber-deep);
}
.assets .a .lbl {
  font-size: 11px;
  color: var(--ink2);
  margin-top: 3px;
}
.phone-cell {
  margin-top: 12px;
}
.tap {
  cursor: pointer;
}
.phone-form {
  display: flex;
  gap: 10px;
  align-items: center;
}
.phone-input {
  flex: 1;
  background: var(--cream);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 14px;
}
.btn.mini {
  padding: 10px 20px;
  font-size: 13px;
}

.tabs {
  display: flex;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  margin: 14px 14px 0;
  overflow: hidden;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 14px;
  color: var(--ink2);
}
.tab.on {
  color: var(--amber-deep);
  font-weight: 700;
  background: var(--amber-soft);
}
.sub-tabs {
  display: flex;
  gap: 8px;
  margin: 12px 14px 0;
}
.sub-tab {
  padding: 5px 16px;
  border-radius: 99px;
  border: 1px solid var(--line);
  background: var(--paper);
  font-size: 12px;
  color: var(--ink2);
}
.sub-tab.on {
  border-color: var(--amber);
  background: var(--amber-soft);
  color: var(--amber-deep);
  font-weight: 600;
}
.coupon-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--paper);
  border: 1px solid var(--amber);
  border-radius: 12px;
  padding: 14px;
  margin: 10px 14px 0;
  background: linear-gradient(135deg, var(--paper), var(--amber-soft));
}
.coupon-item.dead {
  border-color: var(--line);
  background: var(--paper);
  opacity: 0.65;
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
.log-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 12px 14px;
  margin: 10px 14px 0;
}
.log-row .t1 {
  font-size: 14px;
  font-weight: 600;
}
.log-row .no {
  font-size: 11px;
  color: var(--ink2);
  font-weight: 400;
  margin-left: 4px;
}
.log-row .t2 {
  font-size: 11px;
  color: var(--ink2);
  margin-top: 3px;
}
.delta {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
  text-align: right;
}
.delta.plus {
  color: var(--green);
}
.delta .after {
  display: block;
  font-size: 11px;
  color: var(--ink2);
  font-weight: 400;
  margin-top: 2px;
}
.wallet-card {
  background: linear-gradient(135deg, var(--green), #47603a);
  color: #fff;
  border-radius: var(--radius);
  margin: 12px 14px 0;
  padding: 20px;
}
.w-balance {
  font-size: 30px;
  font-weight: 800;
}
.w-sub {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 2px;
}
.w-line {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  opacity: 0.85;
  margin-top: 12px;
}
.tier-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}
.tier {
  text-align: center;
  padding: 12px 0;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--paper);
  font-size: 14px;
  font-weight: 700;
}
.tier.on {
  border-color: var(--amber);
  background: var(--amber-soft);
  color: var(--amber-deep);
}
.bonus {
  color: var(--green);
  font-size: 12px;
}
.tail {
  height: 10px;
}
</style>
