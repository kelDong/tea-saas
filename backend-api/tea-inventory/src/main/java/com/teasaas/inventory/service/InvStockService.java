package com.teasaas.inventory.service;

import com.teasaas.inventory.dto.StockItemDTO;

import java.util.List;

/**
 * 库存变动核心服务（数据库设计 13.1/13.3；供 tea-order 下单扣减、tea-supply 采购入库调用）
 *
 * <p>幂等（红线 3）：扣减/回补/入库均以 biz_no + biz_type 在 inv_stock_log 查重，
 * 重复调用直接跳过；扣减用条件更新（stock_qty >= need）防超卖，任一原料不足整批回滚。</p>
 */
public interface InvStockService {

    /**
     * 入库（采购入库等，bizType 见 {@link com.teasaas.inventory.constant.StockBizType}）
     * 库存行不存在则插入（uk_stock 兜底），写流水；bizNo+bizType 已存在则幂等跳过
     *
     * @param storeId  门店
     * @param items    明细（qty 正数）
     * @param bizType  业务类型
     * @param bizNo    业务单号（如采购单号）
     */
    void inbound(Long storeId, List<StockItemDTO> items, int bizType, String bizNo);

    /**
     * 下单扣减（F-INV-02，事务内调用）
     * 条件更新 stock_qty = stock_qty - qty AND stock_qty >= qty（version+1），
     * 影响行数=0 视为库存不足抛 STOCK_NOT_ENOUGH，整批回滚；bizNo 幂等
     *
     * @param orderNo 订单号（幂等键）
     * @param storeId 门店
     * @param items   明细（qty 正数，调用方按配方快照反算汇总）
     */
    void deduct(String orderNo, Long storeId, List<StockItemDTO> items);

    /**
     * 回补（超时/退款，bizType=2/3），条件更新 +qty，写流水；bizNo+bizType 幂等
     */
    void restore(String bizNo, Long storeId, List<StockItemDTO> items, int bizType);
}
