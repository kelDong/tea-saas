package com.teasaas.member.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.member.dto.RechargeDTO;
import com.teasaas.member.entity.MemRechargeRecord;
import com.teasaas.member.vo.WalletVO;

/**
 * 储值服务（F-MEM-03，接口需求 10.3，P0-4 决策；供 tea-order/tea-payment 跨模块调用）
 *
 * <p>储值为独立支付通道（pay_channel=2），不与微信混合；余额不足整单时不可用。
 * 钱包变动全部走条件更新（balance 侧防超扣）；一期 mock 充值即时到账，
 * 正式接入后充值走微信支付回调入账（批次外）。
 * 一期"储值流水"=充值记录，消费流水经订单/支付流水（pay_payment channel=2）查询，
 * 专用钱包流水表二期。</p>
 */
public interface MemWalletService {

    /** 充值（C 端本人，mock 即时到账：amount+bonus 入账，写 mem_recharge_record） */
    WalletVO recharge(Long memberId, RechargeDTO dto);

    /** 储值账户（C 端本人；无钱包记录返回 0 值） */
    WalletVO getWallet(Long memberId);

    /** 充值记录分页（C 端本人） */
    PageResult<MemRechargeRecord> pageLogs(Long memberId, int page, int size);

    /**
     * 储值支付整单（供 tea-order 下单联动）：条件更新 balance>=amount 防超扣，
     * 失败抛 WALLET_BALANCE_NOT_ENOUGH。幂等由调用方保证（orderNo 唯一，
     * appCreate 单一入口事务内调用一次）。
     */
    void payForOrder(Long memberId, String orderNo, long amount);

    /**
     * 退款回补（供 tea-payment 储值退款，bizNo=orderNo）：
     * 幂等由退款单唯一性保证（pay_refund 防重）。
     */
    void refundOfOrder(Long memberId, String orderNo, long amount);
}
