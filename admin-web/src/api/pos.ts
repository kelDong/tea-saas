import { http } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { PosMemberVO, PosOrderCreateDTO, PosOrderCreateVO, PosPayDTO, PosPayVO } from '@/types/order'

/** POS 收银接口（后端 PosOrderController / PosPayController / PosMemberController，F-ORD-03/04）
 *  流程：下单（即时扣库存）→ 收款确认（三选一渠道，即时记账 1→2） */
export const posApi = {
  /** 手工下单（R7/R8 名下门店） */
  create: (data: PosOrderCreateDTO) => http.post<PosOrderCreateVO>('/pos/orders', data),
  /** 收款确认（储值渠道须先经 members 选定 memberId） */
  pay: (orderId: number, data: PosPayDTO) => http.post<PosPayVO>(`/pos/orders/${orderId}/pay`, data),
  /** 会员搜索（昵称/手机号模糊，带储值余额；储值收款选会员） */
  members: (keyword: string) =>
    http.get<PageResult<PosMemberVO>>('/pos/members', { keyword, page: 1, size: 20 }),
}
