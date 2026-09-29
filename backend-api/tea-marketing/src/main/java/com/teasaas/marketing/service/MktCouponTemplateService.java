package com.teasaas.marketing.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.marketing.dto.CouponTemplateDTO;
import com.teasaas.marketing.entity.MktCouponTemplate;
import com.teasaas.marketing.vo.CouponTemplateDetailVO;

/**
 * 券模板服务（F-MKT-01，接口需求 11.1）
 *
 * <p>模板无门店归属，R6 查看为全租户可见。适用范围（PRD 发放范围）：
 * scope_type=1 全场（无 scope 行）/2 指定商品（mkt_coupon_scope 按
 * coupon_id 存 target_id=商品ID；指定门店 scope_type=3 预留未用）。</p>
 */
public interface MktCouponTemplateService {

    /** 列表（R2/R5/R6；status 过滤可选） */
    PageResult<MktCouponTemplate> page(int page, int size, Integer status);

    /** 详情（R2/R5/R6；编辑回填用，含适用范围商品） */
    CouponTemplateDetailVO detail(Long id);

    /** 创建（R2/R5） */
    MktCouponTemplate create(CouponTemplateDTO dto);

    /** 编辑/停用（R2/R5；已领券不受影响，仅停止新领取） */
    void update(Long id, CouponTemplateDTO dto);
}
