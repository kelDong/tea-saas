package com.teasaas.it;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.member.dto.RechargeDTO;
import com.teasaas.member.entity.MemMember;
import com.teasaas.member.entity.MemRechargeRecord;
import com.teasaas.member.entity.MemWallet;
import com.teasaas.member.mapper.MemMemberMapper;
import com.teasaas.member.mapper.MemRechargeRecordMapper;
import com.teasaas.member.mapper.MemWalletMapper;
import com.teasaas.member.service.MemWalletService;
import com.teasaas.member.vo.WalletVO;
import com.teasaas.order.mapper.OrdOrderMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 储值钱包并发集成测试（Review §4-3 验证 + §4-4 资金链路）
 *
 * <p>不变量：balance 条件更新/SQL 自增防超扣防丢失更新；
 * 充值流水 balance_after = 更新后回读真实值（§4-3 修复回归）；
 * 并发首开卡仅一行钱包（uk_wallet_member 兜底）。</p>
 */
class MemWalletIntegrationTest extends IntegrationTestBase {

    private final MemWalletService memWalletService;
    private final MemWalletMapper memWalletMapper;
    private final MemRechargeRecordMapper memRechargeRecordMapper;

    MemWalletIntegrationTest(MemWalletService memWalletService, MemWalletMapper memWalletMapper,
                             MemRechargeRecordMapper memRechargeRecordMapper,
                             MemMemberMapper memMemberMapper, OrdOrderMapper ordOrderMapper) {
        super(memMemberMapper, ordOrderMapper);
        this.memWalletService = memWalletService;
        this.memWalletMapper = memWalletMapper;
        this.memRechargeRecordMapper = memRechargeRecordMapper;
    }

    private void recharge(Long memberId, long amountFen) {
        RechargeDTO dto = new RechargeDTO();
        dto.setAmount(amountFen);
        memWalletService.recharge(memberId, dto);
    }

    private long balance(Long memberId) {
        return memWalletService.getWallet(memberId).getBalance();
    }

    private List<Long> balanceAfters(Long memberId) {
        return memRechargeRecordMapper.selectList(new LambdaQueryWrapper<MemRechargeRecord>()
                        .eq(MemRechargeRecord::getMemberId, memberId))
                .stream().map(MemRechargeRecord::getBalanceAfter).sorted().toList();
    }

    @Test
    void rechargeBalanceAfterMatchesDbSequentially() {
        MemMember member = insertMember();

        recharge(member.getId(), 10_000L);
        recharge(member.getId(), 25_000L);

        WalletVO wallet = memWalletService.getWallet(member.getId());
        assertEquals(35_000L, wallet.getBalance());
        assertEquals(35_000L, wallet.getTotalRecharge());
        assertEquals(List.of(10_000L, 35_000L), balanceAfters(member.getId()),
                "balance_after 必须是逐笔真实余额（Review §4-3）");
    }

    @Test
    void concurrentRechargeLosesNoUpdateAndReadsBackRealBalance() throws Exception {
        MemMember member = insertMember();

        // 并发首插 uk_wallet 可能出 InnoDB 死锁牺牲者（整单回滚可重试，非数据损坏），
        // 正确不变量：落地余额 = 成功笔数×单笔额，且回读 balance_after 链与之吻合
        List<Object> outcomes = runConcurrently(10, i -> {
            recharge(member.getId(), 10_000L);
            return "ok";
        });
        long okCount = countOk(outcomes);
        assertTrue(okCount >= 2, "至少两笔成功才有校验意义，实际出口=" + outcomes);

        assertEquals(10_000L * okCount, balance(member.getId()), "无丢失更新");
        Set<Long> afters = balanceAfters(member.getId()).stream().collect(Collectors.toSet());
        assertEquals(okCount, afters.size(), "回读真实值在行锁串行下两两互异");
        assertTrue(afters.contains(balance(member.getId())), "终态余额必须出现在流水链上");
        assertEquals(afters.stream().max(Long::compareTo).orElse(0L), balance(member.getId()));
    }

    @Test
    void concurrentOversubscribedPayNeverDropsBalanceNegative() throws Exception {
        MemMember member = insertMember();
        recharge(member.getId(), 100_000L); // 余额 1000 元

        // 10 单各 200 元（合计 2000 > 1000）：条件更新 balance>=amount 必须恰好放行 5 笔
        List<Object> outcomes = runConcurrently(10, i -> {
            memWalletService.payForOrder(member.getId(), "IT-PAY-" + i + "-" + member.getId(), 20_000L);
            return "ok";
        });

        assertEquals(5, countOk(outcomes), "恰好 5 笔成功，实际出口=" + outcomes);
        assertEquals(5, countBiz(outcomes, ErrorCode.WALLET_BALANCE_NOT_ENOUGH));
        WalletVO wallet = memWalletService.getWallet(member.getId());
        assertEquals(0L, wallet.getBalance(), "余额精确扣至 0，绝不为负");
        assertEquals(100_000L, wallet.getTotalConsume());
    }

    @Test
    void concurrentFirstRechargeCreatesSingleWalletRow() throws Exception {
        MemMember member = insertMember(); // 未开卡

        List<Object> outcomes = runConcurrently(10, i -> {
            recharge(member.getId(), 10_000L);
            return "ok";
        });

        long rows = memWalletMapper.selectCount(new LambdaQueryWrapper<MemWallet>()
                .eq(MemWallet::getMemberId, member.getId()));
        assertEquals(1, rows, "uk_wallet 并发开卡兜底");
        assertEquals(10_000L * countOk(outcomes), balance(member.getId()),
                "落地余额=成功笔数×单笔额（死锁牺牲者整单回滚可重试）");
    }
}
