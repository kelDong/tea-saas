/**
 * 本地存储封装（键名统一前缀 tea_app:）
 *
 * 统一走 uni.setStorageSync / uni.getStorageSync——
 * 禁用 localStorage（仅 H5 平台存在，小程序端无此 API）。
 */

export const StorageKey = {
  TOKEN: 'tea_app:token',
  MEMBER: 'tea_app:member',
  CART: 'tea_app:cart',
  LAST_STORE: 'tea_app:last_store',
  LOCATION: 'tea_app:location',
} as const;

export const storage = {
  get<T>(key: string): T | null {
    try {
      const v = uni.getStorageSync(key);
      return v === '' || v === undefined ? null : (v as T);
    } catch {
      return null;
    }
  },
  set(key: string, value: unknown): void {
    try {
      uni.setStorageSync(key, value);
    } catch {
      // 存储失败静默（不阻断主流程）
    }
  },
  remove(key: string): void {
    try {
      uni.removeStorageSync(key);
    } catch {
      // 忽略
    }
  },
};
