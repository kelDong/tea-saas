package com.teasaas.member.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.member.entity.MemPointsLog;

/**
 * 积分服务（F-MEM-02，接口需求 10.2；供 tea-order/tea-payment 跨模块调用）
 *
 * <p>可用积分 = SUM(mem_points_log.change_points)（1得/3赠为正，2抵扣/4过期为负），
 * 实时汇总不落冗余字段；mem_member.total_points 为累计获得（等级自动化二期用）。
 * 所有联动方法以 bizNo(orderNo)+bizType 查流水幂等。</p>
 */
public interface MemPointsService {

    /** 可用积分（C 端/试算用） */
    int availablePoints(Long memberId);

    /** 积分明细（C 端本人） */
    PageResult<MemPointsLog> pageLogs(Long memberId, int page, int size);

    /** 1积分抵N分（订单试算联动用；缺省配置 1:1） */
    int pointValue();

    /**
     * 下单抵扣（bizType=2，bizNo=orderNo 幂等）：校验可用余额，
     * 不足抛 POINTS_NOT_ENOUGH。调用方须在同一事务内。
     */
    void useForOrder(Long memberId, String orderNo, int points);

    /**
     * 支付成功得积分（bizType=1，bizNo=orderNo 幂等）：
     * points = payAmount(分)/100 * pointsPerYuan 向下取整。
     */
    void earnOnPaid(Long memberId, String orderNo, long payAmount);

    /**
     * 取消/退款回补积分（bizType=3 赠送类，bizNo=orderNo 幂等，
     * PRD 一期全额退还）。与 useForOrder 同 orderNo 不同 bizType 天然不冲突。
     */
    void refundOfOrder(Long memberId, String orderNo, int points);
}
