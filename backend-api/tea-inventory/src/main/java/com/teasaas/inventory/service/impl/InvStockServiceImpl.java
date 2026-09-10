package com.teasaas.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.inventory.constant.StockBizType;
import com.teasaas.inventory.dto.StockItemDTO;
import com.teasaas.inventory.entity.InvStoreMaterialStock;
import com.teasaas.inventory.entity.InvStockLog;
import com.teasaas.inventory.mapper.InvStoreMaterialStockMapper;
import com.teasaas.inventory.mapper.InvStockLogMapper;
import com.teasaas.inventory.service.InvStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存变动核心服务实现（数据库设计 13.1 扣减 / 13.3 入库）
 *
 * <p>一致性方案：
 * ① 扣减 = 条件更新（WHERE stock_qty >= need）+ version 自增，天然防超卖，影响行数=0 即不足；
 * ② 入库/回补 = stock_qty + qty 的原子自增（无上限约束）；
 * ③ 所有变动写 inv_stock_log（before/after 从更新后回读推算，保证 SUM(log)=stock 对账成立）；
 * ④ 幂等（红线 3）：biz_no + biz_type 查重，重复调用直接返回。</p>
 *
 * <p>注意：本服务被 tea-order / tea-supply 跨模块调用，调用方必须在事务内。</p>
 */
@Service
@RequiredArgsConstructor
public class InvStockServiceImpl implements InvStockService {

    private final InvStoreMaterialStockMapper stockMapper;
    private final InvStockLogMapper stockLogMapper;

    // ==================== 入库（采购等） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inbound(Long storeId, List<StockItemDTO> items, int bizType, String bizNo) {
        if (alreadyLogged(bizNo, bizType)) {
            return;
        }
        for (StockItemDTO item : items) {
            if (item.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            upsertIncrease(storeId, item.getMaterialId(), item.getQty());
            writeLog(storeId, item.getMaterialId(), item.getQty(), bizType, bizNo, null);
        }
    }

    // ==================== 下单扣减（F-INV-02） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(String orderNo, Long storeId, List<StockItemDTO> items) {
        if (alreadyLogged(orderNo, StockBizType.DEDUCT)) {
            return;
        }
        for (StockItemDTO item : items) {
            if (item.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Long materialId = item.getMaterialId();
            // 条件更新（13.1）：影响行数=0 → 库存行不存在或不足，整批回滚
            int affected = stockMapper.update(null, new LambdaUpdateWrapper<InvStoreMaterialStock>()
                    .setSql("stock_qty = stock_qty - {0}", item.getQty())
                    .setSql("version = version + 1")
                    .set(InvStoreMaterialStock::getUpdateTime, LocalDateTime.now())
                    .set(InvStoreMaterialStock::getUpdateBy, currentUserIdOrNull())
                    .eq(InvStoreMaterialStock::getStoreId, storeId)
                    .eq(InvStoreMaterialStock::getMaterialId, materialId)
                    .apply("stock_qty >= {0}", item.getQty()));
            if (affected == 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "原料库存不足（materialId=" + materialId + "）");
            }
            writeLog(storeId, materialId, item.getQty().negate(), StockBizType.DEDUCT, orderNo, null);
        }
    }

    // ==================== 回补（超时/退款） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(String bizNo, Long storeId, List<StockItemDTO> items, int bizType) {
        if (alreadyLogged(bizNo, bizType)) {
            return;
        }
        for (StockItemDTO item : items) {
            if (item.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            upsertIncrease(storeId, item.getMaterialId(), item.getQty());
            writeLog(storeId, item.getMaterialId(), item.getQty(), bizType, bizNo, null);
        }
    }

    // ==================== 私有工具 ====================

    /** 幂等查重：同 biz_no + biz_type 的流水已存在则视为已处理 */
    private boolean alreadyLogged(String bizNo, int bizType) {
        if (bizNo == null || bizNo.isBlank()) {
            return false;
        }
        return stockLogMapper.selectCount(new LambdaQueryWrapper<InvStockLog>()
                .eq(InvStockLog::getBizNo, bizNo)
                .eq(InvStockLog::getBizType, bizType)) > 0;
    }

    /**
     * 库存自增（入库/回补）：行存在则 +qty；不存在则插入（uk_stock 并发兜底：插入冲突改为自增重试一次）
     */
    private void upsertIncrease(Long storeId, Long materialId, BigDecimal qty) {
        int affected = increase(storeId, materialId, qty);
        if (affected > 0) {
            return;
        }
        InvStoreMaterialStock stock = new InvStoreMaterialStock();
        stock.setTenantId(currentTenantId());
        stock.setStoreId(storeId);
        stock.setMaterialId(materialId);
        stock.setStockQty(qty);
        stock.setVersion(0);
        stock.setUpdateTime(LocalDateTime.now());
        stock.setUpdateBy(currentUserIdOrNull());
        try {
            stockMapper.insert(stock);
        } catch (DuplicateKeyException e) {
            // 并发首插冲突：退回自增
            increase(storeId, materialId, qty);
        }
    }

    private int increase(Long storeId, Long materialId, BigDecimal qty) {
        return stockMapper.update(null, new LambdaUpdateWrapper<InvStoreMaterialStock>()
                .setSql("stock_qty = stock_qty + {0}", qty)
                .setSql("version = version + 1")
                .set(InvStoreMaterialStock::getUpdateTime, LocalDateTime.now())
                .set(InvStoreMaterialStock::getUpdateBy, currentUserIdOrNull())
                .eq(InvStoreMaterialStock::getStoreId, storeId)
                .eq(InvStoreMaterialStock::getMaterialId, materialId));
    }

    /**
     * 写流水：更新后回读现值推算 after/before（保证 log 链与 SUM 对账成立）
     */
    private void writeLog(Long storeId, Long materialId, BigDecimal signedChange, int bizType, String bizNo, String remark) {
        InvStoreMaterialStock stock = stockMapper.selectOne(new LambdaQueryWrapper<InvStoreMaterialStock>()
                .eq(InvStoreMaterialStock::getStoreId, storeId)
                .eq(InvStoreMaterialStock::getMaterialId, materialId));
        BigDecimal after = stock == null ? BigDecimal.ZERO : stock.getStockQty();
        BigDecimal before = after.subtract(signedChange);

        InvStockLog log = new InvStockLog();
        log.setTenantId(currentTenantId());
        log.setStoreId(storeId);
        log.setMaterialId(materialId);
        log.setChangeQty(signedChange);
        log.setBeforeQty(before);
        log.setAfterQty(after);
        log.setBizType(bizType);
        log.setBizNo(bizNo);
        log.setRemark(remark);
        log.setCreateBy(currentUserIdOrNull());
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);
    }

    /** C 端/定时任务上下文无登录态，容忍 null */
    private Long currentUserIdOrNull() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? TenantContext.SUPER_TENANT : tenantId;
    }
}
