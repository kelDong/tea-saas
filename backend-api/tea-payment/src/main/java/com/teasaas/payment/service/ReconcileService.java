package com.teasaas.payment.service;

import com.teasaas.payment.vo.ReconcileVO;

/**
 * 对账服务（F-PAY-03，接口需求 9.3，数据库设计 13.4）
 *
 * <p>无状态按日实时计算：当日已支付订单 vs 成功支付流水（笔数/金额/逐单匹配）
 * + 库存账实核对（SUM(inv_stock_log.change_qty) = inv_store_material_stock.stock_qty）。
 * 微信商户账单核对待正式资质下发（t100011）后接入。</p>
 */
public interface ReconcileService {

    /** 触发对账（R2，当前租户）：date 缺省为昨天 */
    ReconcileVO run(String date);

    /** 对账结果查询（R2，当前租户）：与 run 同源同果 */
    ReconcileVO get(String date);

    /** 每日对账（tea-job 定时，遍历租户，返回有差异的租户数） */
    int runDaily(String date);
}
