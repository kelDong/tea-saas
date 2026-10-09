/**
 * 门店 store：定位 / 附近门店 / 当前选中门店（持久化）
 */

import { defineStore } from 'pinia';
import { storeApi } from '@/api';
import { StorageKey, storage } from '@/utils/storage';
import type { StoreNearbyVO } from '@/types/api';

interface Loc {
  lng: number;
  lat: number;
}

/** 定位失败兜底坐标（成都春熙路，对齐演示库门店数据） */
const FALLBACK_LOC: Loc = { lng: 104.0817, lat: 30.6598 };

export const useShopStore = defineStore('shop', {
  state: () => ({
    location: storage.get<Loc>(StorageKey.LOCATION) ?? FALLBACK_LOC,
    stores: [] as StoreNearbyVO[],
    loading: false,
    selected: storage.get<{ id: string; storeName: string }>(StorageKey.LAST_STORE),
  }),
  actions: {
    locate(): Promise<Loc> {
      return new Promise((resolve) => {
        uni.getLocation({
          type: 'gcj02',
          success: (res) => {
            const loc = { lng: res.longitude, lat: res.latitude };
            this.location = loc;
            storage.set(StorageKey.LOCATION, loc);
            resolve(loc);
          },
          fail: () => resolve(this.location ?? FALLBACK_LOC), // devtools 未授权定位时兜底
        });
      });
    },
    async fetchNearby(): Promise<void> {
      this.loading = true;
      try {
        const loc = await this.locate();
        this.stores = await storeApi.nearby(loc.lng, loc.lat);
      } finally {
        this.loading = false;
      }
    },
    selectStore(store: { id: string; storeName: string }): void {
      this.selected = store;
      storage.set(StorageKey.LAST_STORE, store);
    },
  },
});
