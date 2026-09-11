package com.teasaas.order.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.order.dto.AppOrderCreateDTO;
import com.teasaas.order.dto.AppOrderPreviewDTO;
import com.teasaas.order.dto.PosOrderCreateDTO;
import com.teasaas.order.entity.OrdOrder;
import com.teasaas.order.vo.OrderCreateVO;
import com.teasaas.order.vo.OrderDetailVO;
import com.teasaas.order.vo.OrderPreviewVO;

/**
 * 订单服务（F-ORD-01~04，接口需求 8.1-8.4）
 *
 * <p>下单即扣库存（数据库设计 13.1 条件更新，InvStockService 幂等）；
 * 状态机：1待支付→2已支付→3制作中→4待取餐→5已完成；6已取消/7已退款为终态。
 * C 端方法运行在 X-Tenant-Id 手动租户注入上下文（多租户红线声明例外，同 FraStore）。</p>
 */
public interface OrdOrderService {

    // ==================== B 端（会话） ====================

    /** 订单列表：R2 全租户；R6/R7/R8 名下门店 */
    PageResult<OrdOrder> page(int page, int size, Long storeId, Integer status);

    /** 订单详情（含明细）：数据权限同上 */
    OrderDetailVO detail(Long id);

    /** 接单（已支付→制作中，R6/R7/R8 名下门店） */
    void accept(Long id);

    /** 标记制作完成（制作中→待取餐） */
    void make(Long id);

    /** 核销取餐（待取餐→已完成） */
    void finish(Long id);

    // ==================== C 端（X-Tenant-Id + X-Member-Id 注入） ====================

    /** 订单预览试算：只算钱，不落库不扣库存 */
    OrderPreviewVO preview(AppOrderPreviewDTO dto);

    /** C 端下单：服务端二次校验库存并即时扣减，生成取餐码 */
    OrderCreateVO appCreate(AppOrderCreateDTO dto, Long memberId);

    /** C 端我的订单列表 */
    PageResult<OrdOrder> appPage(int page, int size, Long memberId);

    /** C 端取消（仅本人待支付单），回补库存（bizType=2） */
    void appCancel(Long orderId, Long memberId);

    // ==================== POS（会话，R7/R8） ====================

    /** 手工下单（order_source=2，顾客免登录），下单即扣库存 */
    OrderCreateVO posCreate(PosOrderCreateDTO dto);

    // ==================== 供 tea-payment / tea-job 调用 ====================

    /**
     * 整单回补库存（退款/超时共用）：按明细 formula_id 快照反算原料汇总，
     * bizNo=orderNo 幂等。退款（tea-payment F-PAY-02）传 bizType=3。
     */
    void restoreOrderStock(Long orderId, int bizType);

    /**
     * 超时未支付批量取消（P1-5，tea-job 每分钟触发）：
     * 遍历租户，status=1 且 create_time 早于 now-pay-timeout-minutes 的单
     * 逐单（条件更新 1→6 + 回补 bizType=2，事务内）。返回取消数量。
     */
    int cancelExpiredOrders();
}
