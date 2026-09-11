package com.teasaas.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.member.dto.RechargeDTO;
import com.teasaas.member.entity.MemMemberConfig;
import com.teasaas.member.entity.MemRechargeRecord;
import com.teasaas.member.entity.MemWallet;
import com.teasaas.member.mapper.MemMemberConfigMapper;
import com.teasaas.member.mapper.MemRechargeRecordMapper;
import com.teasaas.member.mapper.MemWalletMapper;
import com.teasaas.member.service.MemWalletService;
import com.teasaas.member.vo.WalletVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 储值服务实现（F-MEM-03，接口需求 10.3，P0-4 决策）
 *
 * <p>钱包变动全部走条件更新/SQL 自增（balance 侧防超扣防读改写竞态）；
 * 一期 mock 充值即时到账；消费/退款流水经订单与支付流水查询，
 * 专用钱包流水表二期。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemWalletServiceImpl implements MemWalletService {

    private static final int DEFAULT_RECHARGE_BONUS = 0;

    private final MemWalletMapper memWalletMapper;
    private final MemRechargeRecordMapper memRechargeRecordMapper;
    private final MemMemberConfigMapper memMemberConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletVO recharge(Long memberId, RechargeDTO dto) {
        long bonus = dto.getAmount() * bonusPermille() / 1000;
        // 先入账再落充值记录（balanceAfter 取入账后值）；total_recharge 不含赠送
        long balanceAfter = addBalance(memberId, dto.getAmount() + bonus, 0L, dto.getAmount());

        MemRechargeRecord record = new MemRechargeRecord();
        record.setTenantId(TenantContext.getTenantId());
        record.setMemberId(memberId);
        record.setAmount(dto.getAmount());
        record.setBonus(bonus);
        record.setBalanceAfter(balanceAfter);
        record.setBizNo("RC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100000, 1000000));
        record.setCreateTime(LocalDateTime.now());
        memRechargeRecordMapper.insert(record);
        return toVO(requireWallet(memberId));
    }

    @Override
    public WalletVO getWallet(Long memberId) {
        MemWallet wallet = memWalletMapper.selectOne(new LambdaQueryWrapper<MemWallet>()
                .eq(MemWallet::getMemberId, memberId));
        if (wallet == null) {
            return new WalletVO(); // 未开卡返回 0 值（不隐式建档）
        }
        return toVO(wallet);
    }

    @Override
    public PageResult<MemRechargeRecord> pageLogs(Long memberId, int page, int size) {
        Page<MemRechargeRecord> result = memRechargeRecordMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<MemRechargeRecord>()
                        .eq(MemRechargeRecord::getMemberId, memberId)
                        .orderByDesc(MemRechargeRecord::getId));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payForOrder(Long memberId, String orderNo, long amount) {
        // 条件更新 balance>=amount 防超扣；幂等由调用方保证（见接口注释）
        int rows = memWalletMapper.update(null, new LambdaUpdateWrapper<MemWallet>()
                .setSql("balance = balance - " + amount)
                .setSql("total_consume = IFNULL(total_consume, 0) + " + amount)
                .eq(MemWallet::getMemberId, memberId)
                .ge(MemWallet::getBalance, amount));
        if (rows == 0) {
            throw new BizException(ErrorCode.WALLET_BALANCE_NOT_ENOUGH);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOfOrder(Long memberId, String orderNo, long amount) {
        // 幂等由退款单唯一性保证（pay_refund 防重，见接口注释）
        addBalance(memberId, amount, -amount, 0L);
    }

    // ==================== 私有工具 ====================

    /** 余额 SQL 自增：delta 入 balance，consumeDelta 联动累计消费（可为负=退款回冲），rechargeDelta 联动累计充值 */
    private long addBalance(Long memberId, long delta, long consumeDelta, long rechargeDelta) {
        MemWallet wallet = getOrCreateWallet(memberId);
        memWalletMapper.update(null, new LambdaUpdateWrapper<MemWallet>()
                .setSql("balance = IFNULL(balance, 0) + " + delta)
                .setSql("total_consume = IFNULL(total_consume, 0) + " + consumeDelta)
                .setSql("total_recharge = IFNULL(total_recharge, 0) + " + rechargeDelta)
                .set(MemWallet::getUpdateTime, LocalDateTime.now())
                .eq(MemWallet::getId, wallet.getId()));
        return (wallet.getBalance() == null ? 0L : wallet.getBalance()) + delta;
    }

    private MemWallet getOrCreateWallet(Long memberId) {
        MemWallet wallet = memWalletMapper.selectOne(new LambdaQueryWrapper<MemWallet>()
                .eq(MemWallet::getMemberId, memberId));
        if (wallet != null) {
            return wallet;
        }
        wallet = new MemWallet();
        wallet.setTenantId(TenantContext.getTenantId());
        wallet.setMemberId(memberId);
        wallet.setBalance(0L);
        wallet.setTotalRecharge(0L);
        wallet.setTotalConsume(0L);
        wallet.setUpdateTime(LocalDateTime.now());
        try {
            memWalletMapper.insert(wallet);
        } catch (DuplicateKeyException e) {
            // uk_wallet_member 并发开卡兜底：重查
            wallet = memWalletMapper.selectOne(new LambdaQueryWrapper<MemWallet>()
                    .eq(MemWallet::getMemberId, memberId));
        }
        return wallet;
    }

    private MemWallet requireWallet(Long memberId) {
        MemWallet wallet = memWalletMapper.selectOne(new LambdaQueryWrapper<MemWallet>()
                .eq(MemWallet::getMemberId, memberId));
        if (wallet == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "储值账户不存在");
        }
        return wallet;
    }

    private WalletVO toVO(MemWallet wallet) {
        WalletVO vo = new WalletVO();
        vo.setBalance(wallet.getBalance() == null ? 0L : wallet.getBalance());
        vo.setTotalRecharge(wallet.getTotalRecharge() == null ? 0L : wallet.getTotalRecharge());
        vo.setTotalConsume(wallet.getTotalConsume() == null ? 0L : wallet.getTotalConsume());
        return vo;
    }

    private int bonusPermille() {
        MemMemberConfig config = memMemberConfigMapper.selectOne(new LambdaQueryWrapper<>());
        return config == null || config.getRechargeBonusPermille() == null
                ? DEFAULT_RECHARGE_BONUS : config.getRechargeBonusPermille();
    }
}
