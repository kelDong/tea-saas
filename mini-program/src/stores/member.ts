/**
 * 会员会话 store（token 真值在 storage，本 store 缓存 MemberVO）
 */

import { defineStore } from 'pinia';
import { memberApi } from '@/api';
import { relogin } from '@/utils/request';
import { StorageKey, storage } from '@/utils/storage';
import type { MemberVO } from '@/types/api';

export const useMemberStore = defineStore('member', {
  state: () => ({
    member: storage.get<MemberVO>(StorageKey.MEMBER),
  }),
  getters: {
    availablePoints: (s) => s.member?.availablePoints ?? 0,
    walletBalance: (s) => s.member?.walletBalance ?? 0,
    logged: () => !!storage.get<string>(StorageKey.TOKEN),
  },
  actions: {
    /** 静默登录：无 token 才登录（token 失效由请求层 401 单飞重登兜底） */
    async silentLogin(): Promise<void> {
      if (!storage.get<string>(StorageKey.TOKEN)) {
        await relogin();
      }
      await this.fetchMe();
    },
    async fetchMe(): Promise<MemberVO> {
      const me = await memberApi.me();
      this.member = me;
      storage.set(StorageKey.MEMBER, me);
      return me;
    },
    async bindPhone(phone: string): Promise<void> {
      await memberApi.bindPhone(phone);
      await this.fetchMe();
    },
    async recharge(amount: number) {
      const wallet = await memberApi.recharge(amount);
      await this.fetchMe();
      return wallet;
    },
  },
});
