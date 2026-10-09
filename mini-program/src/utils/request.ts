/**
 * 请求封装（uni.request Promise 化）
 *
 * - 头注入：恒定 X-Tenant-Id（公开接口多租户口径）+ 有 token 时 Authorization: Bearer
 * - Result 解包：code===0 返回 data，否则 BizError（默认 toast message，可 silent）
 * - 401（token 失效）：单飞静默重登（重新 wx.login 取 code——code 一次性）后重放一次
 */

import { config } from '@/config';
import { StorageKey, storage } from '@/utils/storage';
import type { Result, WxLoginVO } from '@/types/api';

export class BizError extends Error {
  code: number;
  constructor(code: number, message: string) {
    super(message);
    this.code = code;
  }
}

class UnauthorizedError extends Error {}

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE';

function rawRequest<T>(method: HttpMethod, url: string, data?: unknown, token?: string): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: config.baseUrl + url,
      method,
      data: data as Record<string, unknown> | undefined,
      header: {
        'Content-Type': 'application/json',
        'X-Tenant-Id': String(config.tenantId),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success: (res) => {
        if (res.statusCode === 401) {
          reject(new UnauthorizedError());
          return;
        }
        const body = res.data as Result<T>;
        if (res.statusCode >= 200 && res.statusCode < 300 && body && body.code === 0) {
          resolve(body.data);
        } else {
          reject(new BizError(body?.code ?? res.statusCode, body?.message || `请求失败(${res.statusCode})`));
        }
      },
      fail: (err) => reject(new BizError(-1, err.errMsg || '网络异常')),
    });
  });
}

export function uniLogin(): Promise<{ code: string }> {
  return new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: (res) => resolve(res as { code: string }),
      fail: (err) => reject(new BizError(-1, err.errMsg || 'wx.login 失败')),
    });
  });
}

/** 静默重登（单飞去重）：wx.login → wx-login（mock 语义 code 即 openid）→ 存 token */
export function relogin(): Promise<string> {
  if (!reloginPromise) {
    reloginPromise = (async () => {
      const { code } = await uniLogin();
      const vo = await rawRequest<WxLoginVO>('POST', '/api/app/v1/auth/wx-login', { code });
      storage.set(StorageKey.TOKEN, vo.token);
      return vo.token;
    })().finally(() => {
      reloginPromise = null;
    });
  }
  return reloginPromise;
}

let reloginPromise: Promise<string> | null = null;

export async function http<T>(method: HttpMethod, url: string, data?: unknown, opts?: { silent?: boolean }): Promise<T> {
  const token = storage.get<string>(StorageKey.TOKEN) ?? undefined;
  try {
    return await rawRequest<T>(method, url, data, token);
  } catch (e) {
    if (e instanceof UnauthorizedError) {
      // token 失效：重登一次后重放；仍失败则按业务错误抛出
      const newToken = await relogin();
      return rawRequest<T>(method, url, data, newToken);
    }
    if (e instanceof BizError && !opts?.silent) {
      uni.showToast({ title: e.message, icon: 'none' });
    }
    throw e;
  }
}
