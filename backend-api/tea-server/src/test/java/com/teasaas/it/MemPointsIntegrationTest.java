package com.teasaas.it;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.member.entity.MemMember;
import com.teasaas.member.entity.MemPointsLog;
import com.teasaas.member.mapper.MemMemberMapper;
import com.teasaas.member.mapper.MemPointsLogMapper;
import com.teasaas.member.service.MemPointsService;
import com.teasaas.order.mapper.OrdOrderMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 积分链路并发幂等集成测试（Review §3.1/§3.3 验证清单 + §4-4）
 *
 * <p>不变量：bizNo+bizType 仅一条流水（uk_points_log_biz 兜底）；
 * 悲观锁串行化下可用积分 SUM(log) 永不为负（并发双单超抵必有一笔拒绝）。</p>
 */
class MemPointsIntegrationTest extends IntegrationTestBase {

    private final MemPointsService memPointsService;
    private final MemPointsLogMapper memPointsLogMapper;

    MemPointsIntegrationTest(MemPointsService memPointsService, MemPointsLogMapper memPointsLogMapper,
                             MemMemberMapper memMemberMapper, OrdOrderMapper ordOrderMapper) {
        super(memMemberMapper, ordOrderMapper);
        this.memPointsService = memPointsService;
        this.memPointsLogMapper = memPointsLogMapper;
    }

    private long logCount(String orderNo, int bizType) {
        return memPointsLogMapper.selectCount(new LambdaQueryWrapper<MemPointsLog>()
                .eq(MemPointsLog::getBizNo, orderNo)
                .eq(MemPointsLog::getBizType, bizType));
    }

    /** 种积分：消费得 100 分（payAmount 100 元 × 1 分/元，缺省配置） */
    private MemMember memberWith100Points() {
        MemMember member = insertMember();
        memPointsService.earnOnPaid(member.getId(), "IT-EARN-" + uid(), 10_000L);
        assertEquals(100, memPointsService.availablePoints(member.getId()));
        return member;
    }

    @Test
    void earnOnPaidIsIdempotentWhenReplayed() {
        MemMember member = insertMember();
        String orderNo = "IT-EARN-" + uid();

        memPointsService.earnOnPaid(member.getId(), orderNo, 5_000L);
        memPointsService.earnOnPaid(member.getId(), orderNo, 5_000L); // 重放

        assertEquals(50, memPointsService.availablePoints(member.getId()));
        assertEquals(1, logCount(orderNo, 1));
    }

    @Test
    void concurrentUseSameOrderNoDeductsExactlyOnce() throws Exception {
        MemMember member = memberWith100Points();
        String orderNo = "IT-USE-" + uid();

        List<Object> outcomes = runConcurrently(20, i -> {
            memPointsService.useForOrder(member.getId(), orderNo, 30);
            return "ok";
        });

        assertEquals(70, memPointsService.availablePoints(member.getId()),
                "同单重放只抵一次，实际出口=" + outcomes);
        assertEquals(1, logCount(orderNo, 2));
        assertTrue(countOk(outcomes) >= 1);
    }

    @Test
    void concurrentOversubscribedUseLeavesSumNonNegative() throws Exception {
        MemMember member = memberWith100Points(); // 可用 100
        String orderA = "IT-USE-A-" + uid();
        String orderB = "IT-USE-B-" + uid();

        // 两单各抵 90，悲观锁（Review §3.3 方案A）串行化：第一笔过、第二笔必拒
        List<Object> outcomes = runConcurrently(2, i -> {
            memPointsService.useForOrder(member.getId(), i == 0 ? orderA : orderB, 90);
            return "ok";
        });

        assertEquals(1, countOk(outcomes), "恰好一笔成功");
        assertEquals(1, countBiz(outcomes, ErrorCode.POINTS_NOT_ENOUGH), "另一笔必须按不足拒绝");
        assertEquals(10, memPointsService.availablePoints(member.getId()), "SUM 绝不为负");
    }

    @Test
    void refundOfOrderIsIdempotent() {
        MemMember member = memberWith100Points();
        String orderNo = "IT-USE-" + uid();
        memPointsService.useForOrder(member.getId(), orderNo, 30);

        memPointsService.refundOfOrder(member.getId(), orderNo, 30);
        memPointsService.refundOfOrder(member.getId(), orderNo, 30); // 重放

        assertEquals(100, memPointsService.availablePoints(member.getId()));
        assertEquals(1, logCount(orderNo, 3));
    }
}
