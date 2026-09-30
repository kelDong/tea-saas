package com.teasaas.it;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.common.exception.BizException;
import com.teasaas.inventory.constant.StockBizType;
import com.teasaas.inventory.dto.StockItemDTO;
import com.teasaas.inventory.entity.InvStoreMaterialStock;
import com.teasaas.inventory.entity.InvStockLog;
import com.teasaas.inventory.mapper.InvStoreMaterialStockMapper;
import com.teasaas.inventory.mapper.InvStockLogMapper;
import com.teasaas.inventory.service.InvStockService;
import com.teasaas.member.mapper.MemMemberMapper;
import com.teasaas.order.mapper.OrdOrderMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 库存扣减/入库并发幂等集成测试（Review §3.1 验证清单 + §4-4）
 *
 * <p>不变量：同 bizNo+bizType 仅一次生效（uk_stock_log_biz DB 兜底）；
 * 条件更新防超卖；不足整批回滚（SUM(log)=stock 对账口径不被破坏）。</p>
 */
class InvStockIntegrationTest extends IntegrationTestBase {

    private final InvStockService invStockService;
    private final InvStoreMaterialStockMapper stockMapper;
    private final InvStockLogMapper stockLogMapper;

    InvStockIntegrationTest(InvStockService invStockService, InvStoreMaterialStockMapper stockMapper,
                            InvStockLogMapper stockLogMapper,
                            MemMemberMapper memMemberMapper, OrdOrderMapper ordOrderMapper) {
        super(memMemberMapper, ordOrderMapper);
        this.invStockService = invStockService;
        this.stockMapper = stockMapper;
        this.stockLogMapper = stockLogMapper;
    }

    private static StockItemDTO item(long materialId, String qty) {
        StockItemDTO dto = new StockItemDTO();
        dto.setMaterialId(materialId);
        dto.setQty(new BigDecimal(qty));
        return dto;
    }

    private BigDecimal stockQty(long storeId, long materialId) {
        InvStoreMaterialStock stock = stockMapper.selectOne(new LambdaQueryWrapper<InvStoreMaterialStock>()
                .eq(InvStoreMaterialStock::getStoreId, storeId)
                .eq(InvStoreMaterialStock::getMaterialId, materialId));
        return stock == null ? BigDecimal.ZERO : stock.getStockQty();
    }

    private long logCount(String bizNo, int bizType) {
        return stockLogMapper.selectCount(new LambdaQueryWrapper<InvStockLog>()
                .eq(InvStockLog::getBizNo, bizNo)
                .eq(InvStockLog::getBizType, bizType));
    }

    /** 采购入库种库存（bizNo 唯一，复用生产链路而非直插表） */
    private void seedStock(long storeId, long materialId, String qty) {
        invStockService.inbound(storeId, List.of(item(materialId, qty)),
                StockBizType.PURCHASE_IN, "IT-IN-" + uid());
    }

    @Test
    void deductIsIdempotentWhenReplayedSequentially() {
        long storeId = nextId(), materialId = nextId();
        seedStock(storeId, materialId, "1000");
        String orderNo = "IT-DED-" + uid();

        invStockService.deduct(orderNo, storeId, List.of(item(materialId, "100")));
        invStockService.deduct(orderNo, storeId, List.of(item(materialId, "100"))); // 重放

        assertEquals(0, new BigDecimal("900").compareTo(stockQty(storeId, materialId)), "重放同单号只扣一次");
        assertEquals(1, logCount(orderNo, StockBizType.DEDUCT));
    }

    @Test
    void concurrentDeductSameOrderNoTakesEffectExactlyOnce() throws Exception {
        long storeId = nextId(), materialId = nextId();
        seedStock(storeId, materialId, "1000");
        String orderNo = "IT-DED-" + uid();

        List<Object> outcomes = runConcurrently(20, i -> {
            invStockService.deduct(orderNo, storeId, List.of(item(materialId, "100")));
            return "ok";
        });

        // 20 线程同 orderNo：1 个生效，其余幂等跳过或被 uk 兜底回滚（含偶发死锁回滚）
        assertEquals(0, new BigDecimal("900").compareTo(stockQty(storeId, materialId)),
                "净扣减必须恰好一次，实际出口=" + outcomes);
        assertEquals(1, logCount(orderNo, StockBizType.DEDUCT));
        assertTrue(countOk(outcomes) >= 1);
    }

    @Test
    void deductRollsBackWholeBatchWhenAnyMaterialInsufficient() {
        long storeId = nextId(), enough = nextId(), insufficient = nextId();
        seedStock(storeId, enough, "500");
        seedStock(storeId, insufficient, "10");
        String orderNo = "IT-DED-" + uid();

        assertThrows(BizException.class, () -> invStockService.deduct(orderNo, storeId,
                List.of(item(enough, "50"), item(insufficient, "9999"))));

        assertEquals(0, new BigDecimal("500").compareTo(stockQty(storeId, enough)), "整批回滚：足量原料不得被扣");
        assertEquals(0, logCount(orderNo, StockBizType.DEDUCT), "整批回滚：不残留流水");
    }

    @Test
    void concurrentInboundOnMissingRowKeepsSingleStockRow() throws Exception {
        long storeId = nextId(), materialId = nextId(); // 不预建库存行，触发 upsert 并发首插

        List<Object> outcomes = runConcurrently(10, i -> {
            invStockService.inbound(storeId, List.of(item(materialId, "100")),
                    StockBizType.PURCHASE_IN, "IT-IN-" + uid());
            return "ok";
        });

        long rows = stockMapper.selectCount(new LambdaQueryWrapper<InvStoreMaterialStock>()
                .eq(InvStoreMaterialStock::getStoreId, storeId)
                .eq(InvStoreMaterialStock::getMaterialId, materialId));
        assertEquals(1, rows, "uk_stock 并发开行兜底：仅一行库存");
        // 并发首插同 uk 可能出 InnoDB 死锁牺牲者（整单回滚、调用方可重试，非数据损坏）——
        // 正确不变量：落地量 = 成功提交笔数 × 单笔量，绝无半提交/丢失更新
        BigDecimal expected = new BigDecimal("100").multiply(BigDecimal.valueOf(countOk(outcomes)));
        assertEquals(0, expected.compareTo(stockQty(storeId, materialId)),
                "落地量必须等于成功笔数×单笔量，实际出口=" + outcomes);
    }
}
