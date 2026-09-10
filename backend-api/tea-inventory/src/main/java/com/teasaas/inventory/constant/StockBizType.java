package com.teasaas.inventory.constant;

/**
 * 库存流水业务类型（inv_stock_log.biz_type，数据库设计 13 章）
 */
public final class StockBizType {

    private StockBizType() {
    }

    /** 下单扣减 */
    public static final int DEDUCT = 1;

    /** 超时回补 */
    public static final int TIMEOUT_RESTORE = 2;

    /** 退款回补 */
    public static final int REFUND_RESTORE = 3;

    /** 采购入库 */
    public static final int PURCHASE_IN = 4;

    /** 盘点修正 */
    public static final int ADJUST = 5;
}
