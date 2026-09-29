package com.teasaas.marketing.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.marketing.dto.CouponScopeItemDTO;
import com.teasaas.marketing.vo.CouponVO;

import java.util.List;

/**
 * 用户券服务（F-MKT-01，接口需求 11.1；供 tea-order 订单结算联动）
 *
 * <p>核销 = 下单结算时 status 1→2 条件更新（防并发重复核销）；
 * 订单取消/退款不退还券（PRD 一期拍板）。有效期：模板 validType=1
 * 固定时段 / 2 领取后 N 天。</p>
 */
public interface MktCouponService {

    /** 领券（C 端本人）：模板启用+库存+限领校验，总量控制一期 count 近似（无剩余量字段） */
    CouponVO claim(Long templateId, Long memberId);

    /** 我的券包（C 端本人；status 1未用/2已用/3过期，null 全部） */
    PageResult<CouponVO> myCoupons(Long memberId, Integer status, int page, int size);

    /**
     * 试算优惠额（订单 preview 联动）：校验券可用（status=1、有效期内、满门槛），
     * 返回优惠金额（分）。不可用抛 COUPON_NOT_AVAILABLE。
     *
     * <p>指定商品券口径：门槛与优惠均按"适用商品小计 base"计算（items 中
     * productId 命中模板范围行的金额之和，未命中商品不计入）；全场券
     * （无范围行）base=整单金额，行为与一期一致。</p>
     */
    long calcDiscount(Long couponId, Long memberId, long orderAmount, List<CouponScopeItemDTO> items);

    /**
     * 核销（订单 appCreate 联动，同一事务内调用）：先 calcDiscount 校验，
     * 再条件更新 status 1→2 写 use_order_id/use_time；并发已核销抛 COUPON_NOT_AVAILABLE。
     */
    long useCoupon(Long couponId, Long memberId, Long orderId, long orderAmount,
                   List<CouponScopeItemDTO> items);
}
