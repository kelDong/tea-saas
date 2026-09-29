package com.teasaas.marketing.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.marketing.dto.CouponTemplateDTO;
import com.teasaas.marketing.entity.MktCouponTemplate;

/**
 * 券模板服务（F-MKT-01，接口需求 11.1）
 *
 * <p>模板无门店归属，R6 查看为全租户可见（契约"R6(本店)"依赖
 * mkt_coupon_scope 门店范围，二期落地）。</p>
 */
public interface MktCouponTemplateService {

    /** 列表（R2/R5/R6；status 过滤可选） */
    PageResult<MktCouponTemplate> page(int page, int size, Integer status);

    /** 详情（R2/R5/R6；编辑回填用） */
    MktCouponTemplate detail(Long id);

    /** 创建（R2/R5） */
    MktCouponTemplate create(CouponTemplateDTO dto);

    /** 编辑/停用（R2/R5；已领券不受影响，仅停止新领取） */
    void update(Long id, CouponTemplateDTO dto);
}
