/**
 * 购物车 store（持久化到 storage；跨门店切换由页面确认后调用）
 */

import { defineStore } from 'pinia';
import { StorageKey, storage } from '@/utils/storage';
import type { ReorderItem } from '@/types/api';

export interface CartLine {
  listingId: string;
  productId: string;
  productName: string;
  image: string | null;
  basePrice: number; // 商品基础价（分）
  specOptionIds: string[];
  specDesc: string;
  unitPrice: number; // 基础价 + 规格加价（分）
  qty: number;
}

interface CartState {
  storeId: string | null;
  storeName: string;
  lines: CartLine[];
}

/** 合并键：同 listing + 同规格（排序后）视为同一行 */
function mergeKey(l: { listingId: string; specOptionIds: string[] }): string {
  return `${l.listingId}|${[...l.specOptionIds].sort().join(',')}`;
}

export const useCartStore = defineStore('cart', {
  state: (): CartState & { pendingReorder: ReorderItem[] } => ({
    ...(storage.get<CartState>(StorageKey.CART) ?? { storeId: null, storeName: '', lines: [] }),
    // 再来一单规格重选队列（内存态，不持久化）
    pendingReorder: [],
  }),
  getters: {
    count: (s) => s.lines.reduce((n, l) => n + l.qty, 0),
    totalAmount: (s) => s.lines.reduce((n, l) => n + l.unitPrice * l.qty, 0),
  },
  actions: {
    persist() {
      storage.set(StorageKey.CART, { storeId: this.storeId, storeName: this.storeName, lines: this.lines });
    },
    addItem(line: CartLine): void {
      const key = mergeKey(line);
      const exist = this.lines.find((l) => mergeKey(l) === key);
      if (exist) {
        exist.qty += line.qty;
      } else {
        this.lines.push(line);
      }
      this.persist();
    },
    changeQty(index: number, delta: number): void {
      const line = this.lines[index];
      if (!line) return;
      line.qty += delta;
      if (line.qty <= 0) this.lines.splice(index, 1);
      this.persist();
    },
    /** 覆盖某商品数量（规格弹层内直改，qty 到 0 移除） */
    setQty(listingId: string, specOptionIds: string[], qty: number): void {
      const key = mergeKey({ listingId, specOptionIds });
      const idx = this.lines.findIndex((l) => mergeKey(l) === key);
      if (idx >= 0) {
        if (qty <= 0) this.lines.splice(idx, 1);
        else this.lines[idx].qty = qty;
        this.persist();
      }
    },
    qtyOf(listingId: string, specOptionIds: string[]): number {
      const key = mergeKey({ listingId, specOptionIds });
      return this.lines.find((l) => mergeKey(l) === key)?.qty ?? 0;
    },
    /** 切换门店（页面负责弹确认；确认后调用本方法清空重建） */
    switchStore(store: { id: string; storeName: string }): void {
      this.storeId = store.id;
      this.storeName = store.storeName;
      this.lines = [];
      this.persist();
    },
    clearLines(): void {
      this.lines = [];
      this.persist();
    },
    /** 再来一单：入队等待逐个重选规格 */
    queueReorder(items: ReorderItem[]): void {
      this.pendingReorder = [...items];
    },
    takeReorder(): ReorderItem | null {
      return this.pendingReorder.shift() ?? null;
    },
  },
});
