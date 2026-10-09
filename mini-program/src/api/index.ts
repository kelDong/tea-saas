/**
 * C 端 API（接口需求 §10/§11/§13，全部 /api/app/v1）
 */

import { http } from '@/utils/request';
import type {
  AppCategoryVO,
  AppOrderCreateDTO,
  AppOrderPreviewDTO,
  AppProductVO,
  CouponVO,
  MemberVO,
  OrderCreateVO,
  OrderDetailVO,
  OrderPreviewVO,
  PageResult,
  PaymentVO,
  PointsLog,
  RechargeRecord,
  ReorderVO,
  OrdOrderLite,
  StoreNearbyVO,
  WalletVO,
  WxLoginVO,
} from '@/types/api';

export const authApi = {
  wxLogin: (code: string) => http<WxLoginVO>('POST', '/api/app/v1/auth/wx-login', { code }),
};

export const storeApi = {
  nearby: (lng: number, lat: number) =>
    http<StoreNearbyVO[]>('GET', `/api/app/v1/stores/nearby?lng=${lng}&lat=${lat}`),
};

export const productApi = {
  categories: (storeId: string) => http<AppCategoryVO[]>('GET', `/api/app/v1/stores/${storeId}/categories`),
  products: (storeId: string, category?: string) =>
    http<AppProductVO[]>('GET', `/api/app/v1/stores/${storeId}/products${category ? `?category=${category}` : ''}`),
};

export const orderApi = {
  preview: (dto: AppOrderPreviewDTO) => http<OrderPreviewVO>('POST', '/api/app/v1/orders/preview', dto),
  create: (dto: AppOrderCreateDTO) => http<OrderCreateVO>('POST', '/api/app/v1/orders', dto),
  page: (page: number, size = 10) =>
    http<PageResult<OrdOrderLite>>('GET', `/api/app/v1/orders?page=${page}&size=${size}`),
  detail: (id: string) => http<OrderDetailVO>('GET', `/api/app/v1/orders/${id}`),
  cancel: (id: string) => http<void>('POST', `/api/app/v1/orders/${id}/cancel`),
  reorder: (id: string) => http<ReorderVO>('POST', `/api/app/v1/orders/${id}/reorder`),
};

export const payApi = {
  wxJsapi: (orderId: string) => http<PaymentVO>('POST', '/api/app/v1/payments/wx-jsapi', { orderId }),
  /** mock 支付闭环（tea.pay.mock=true 期间，DEV）：直连回调免鉴权接口模拟网关确认 */
  mockCallback: (paymentNo: string, amount: number) =>
    http<string>('POST', '/api/notify/wx-pay/callback', {
      paymentNo,
      transactionId: `MOCK${paymentNo}`,
      resultCode: 'SUCCESS',
      amount,
    }),
};

export const memberApi = {
  me: () => http<MemberVO>('GET', '/api/app/v1/members/me'),
  bindPhone: (phone: string) => http<void>('PUT', '/api/app/v1/members/me/phone', { phone }),
  points: (page: number, size = 10) =>
    http<PageResult<PointsLog>>('GET', `/api/app/v1/members/me/points?page=${page}&size=${size}`),
  wallet: () => http<WalletVO>('GET', '/api/app/v1/members/me/wallet'),
  recharge: (amount: number) => http<WalletVO>('POST', '/api/app/v1/members/me/wallet/recharge', { amount }),
  walletLogs: (page: number, size = 10) =>
    http<PageResult<RechargeRecord>>('GET', `/api/app/v1/members/me/wallet/logs?page=${page}&size=${size}`),
};

export const couponApi = {
  my: (status?: number, page = 1, size = 10) =>
    http<PageResult<CouponVO>>(
      'GET',
      `/api/app/v1/coupons?${status ? `status=${status}&` : ''}page=${page}&size=${size}`,
    ),
  claim: (templateId: string) => http<CouponVO>('POST', `/api/app/v1/coupons/${templateId}/claim`),
};
