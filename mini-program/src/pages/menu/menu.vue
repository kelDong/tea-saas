<script setup lang="ts">
/**
 * 点单页（F-APP-02，原型 s-menu）
 * - 左分类（stores/{id}/categories）+ 右分组商品流（stores/{id}/products，仅可售）
 * - 无规格商品点＋直接入车；有规格弹 SpecPopup
 * - 再来一单规格重选队列：onShow 检测 pendingReorder 逐项弹层
 */

import { onLoad, onShow } from '@dcloudio/uni-app';
import { computed, nextTick, ref } from 'vue';
import { productApi } from '@/api';
import { useCartStore } from '@/stores/cart';
import { useShopStore } from '@/stores/shop';
import ProductCard from '@/components/ProductCard.vue';
import SpecPopup from '@/components/SpecPopup.vue';
import CartBar from '@/components/CartBar.vue';
import CartDrawer from '@/components/CartDrawer.vue';
import type { AppProductVO, ReorderItem } from '@/types/api';

const cart = useCartStore();
const shop = useShopStore();

const categories = ref<{ id: string; name: string }[]>([]);
const products = ref<AppProductVO[]>([]);
const activeCat = ref<string>('');
const loading = ref(true);
const drawerShow = ref(false);

const specShow = ref(false);
const specProduct = ref<AppProductVO | null>(null);
const reorderQty = ref<number | null>(null); // 再来一单重选场景带入数量

const grouped = computed(() =>
  categories.value.map((c) => ({ cat: c, items: products.value.filter((p) => p.categoryId === c.id) })),
);

function qtyOfProduct(p: AppProductVO): number {
  return cart.lines.filter((l) => l.productId === p.productId).reduce((n, l) => n + l.qty, 0);
}

async function loadMenu() {
  const storeId = cart.storeId ?? shop.selected?.id;
  if (!storeId) {
    uni.showToast({ title: '请先选择门店', icon: 'none' });
    setTimeout(() => uni.reLaunch({ url: '/pages/store-select/store-select' }), 600);
    return;
  }
  loading.value = true;
  try {
    const [cats, prods] = await Promise.all([productApi.categories(storeId), productApi.products(storeId)]);
    // 过滤空分类（接口已按有商品分类返回，双保险）并保序
    categories.value = cats.filter((c) => prods.some((p) => p.categoryId === c.id));
    products.value = prods;
    if (!activeCat.value && categories.value.length) activeCat.value = categories.value[0].id;
  } finally {
    loading.value = false;
  }
}

function selCat(id: string) {
  activeCat.value = id;
}

function tapProduct(p: AppProductVO) {
  if ((p.specs ?? []).length > 0) {
    reorderQty.value = null;
    specProduct.value = p;
    specShow.value = true;
  } else {
    quickAdd(p);
  }
}

function quickAdd(p: AppProductVO) {
  cart.addItem({
    listingId: p.listingId,
    productId: p.productId,
    productName: p.productName,
    image: p.image,
    basePrice: p.salePrice,
    specOptionIds: [],
    specDesc: '',
    unitPrice: p.salePrice,
    qty: 1,
  });
}

function onSpecConfirm(payload: { specOptionIds: string[]; specDesc: string; unitPrice: number; qty: number }) {
  const p = specProduct.value;
  if (!p) return;
  const qty = reorderQty.value ?? payload.qty;
  cart.addItem({
    listingId: p.listingId,
    productId: p.productId,
    productName: p.productName,
    image: p.image,
    basePrice: p.salePrice,
    specOptionIds: payload.specOptionIds,
    specDesc: payload.specDesc,
    unitPrice: payload.unitPrice,
    qty,
  });
  specShow.value = false;
  if (reorderQty.value !== null) nextTick(pumpReorderQueue); // 队列场景：确认后继续弹下一项
}

function changeQty(i: number, d: number) {
  cart.changeQty(i, d);
}

function clearCart() {
  uni.showModal({
    title: '清空购物车',
    content: '确定清空所有商品吗？',
    success: (r) => {
      if (r.confirm) {
        cart.clearLines();
        drawerShow.value = false;
      }
    },
  });
}

function goCheckout() {
  if (cart.lines.length === 0) return;
  drawerShow.value = false;
  uni.navigateTo({ url: '/pages/checkout/checkout' });
}

/** 再来一单规格重选队列（批次2 order 页入队） */
function pumpReorderQueue() {
  const item: ReorderItem | null = cart.takeReorder();
  if (!item) return;
  const p = products.value.find((x) => x.listingId === item.listingId);
  if (!p) return pumpReorderQueue(); // 本店已无该商品，跳过继续
  reorderQty.value = item.qty;
  specProduct.value = p;
  specShow.value = true;
}

onLoad(() => {
  if (cart.storeName) uni.setNavigationBarTitle({ title: cart.storeName });
  loadMenu();
});

onShow(() => {
  if (cart.pendingReorder.length && !loading.value) pumpReorderQueue();
});
</script>

<template>
  <view class="page">
    <view v-if="loading" class="loading">正在加载菜单…</view>

    <view v-else class="menu-wrap">
      <scroll-view class="cat-bar" scroll-y>
        <view
          v-for="c in categories"
          :key="c.id"
          class="cat-item"
          :class="{ on: activeCat === c.id }"
          @tap="selCat(c.id)"
        >
          {{ c.name }}
        </view>
      </scroll-view>

      <scroll-view class="goods" scroll-y :scroll-into-view="'sec-' + activeCat" scroll-with-animation>
        <view v-for="g in grouped" :id="'sec-' + g.cat.id" :key="g.cat.id" class="group">
          <view class="gcat">{{ g.cat.name }}</view>
          <ProductCard
            v-for="p in g.items"
            :key="p.listingId"
            :product="p"
            :qty-in-cart="qtyOfProduct(p)"
            @tap="tapProduct(p)"
          />
        </view>
        <EmptyState v-if="products.length === 0" text="本店暂无在售商品" />
        <view class="goods-tail" />
      </scroll-view>
    </view>

    <CartBar :count="cart.count" :total="cart.totalAmount" @open="drawerShow = true" @checkout="goCheckout" />

    <CartDrawer
      :show="drawerShow"
      :lines="cart.lines"
      :total="cart.totalAmount"
      @close="drawerShow = false"
      @change="changeQty"
      @clear="clearCart"
      @checkout="goCheckout"
    />

    <SpecPopup :show="specShow" :product="specProduct" @close="specShow = false" @confirm="onSpecConfirm" />
  </view>
</template>

<style scoped lang="scss">
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}
.loading {
  padding: 40px;
  text-align: center;
  color: var(--ink2);
}
.menu-wrap {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding-bottom: 76px; // CartBar 高度
}
.cat-bar {
  width: 96px;
  background: var(--paper);
  border-right: 1px solid var(--line);
  height: 100%;
}
.cat-item {
  padding: 16px 10px;
  font-size: 12px;
  text-align: center;
  color: var(--ink2);
  border-left: 3px solid transparent;
}
.cat-item.on {
  color: var(--amber-deep);
  font-weight: 700;
  background: var(--cream);
  border-left-color: var(--amber);
}
.goods {
  flex: 1;
  height: 100%;
  padding: 8px 12px;
}
.gcat {
  font-size: 13px;
  font-weight: 700;
  padding: 10px 4px 6px;
}
.goods-tail {
  height: 16px;
}
</style>
