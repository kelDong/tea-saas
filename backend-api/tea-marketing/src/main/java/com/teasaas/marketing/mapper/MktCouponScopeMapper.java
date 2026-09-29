package com.teasaas.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teasaas.marketing.entity.MktCouponScope;
import com.teasaas.marketing.vo.CouponTemplateDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 券适用范围 Mapper（F-MKT-01 指定商品券）
 *
 * <p>手写 SQL 见 MktCouponScopeMapper.xml；租户条件由租户插件自动追加，
 * 与 ReportMapper 同口径（勿显式写 tenant_id）。</p>
 */
@Mapper
public interface MktCouponScopeMapper extends BaseMapper<MktCouponScope> {

    /** 模板范围商品（LEFT JOIN prd_product 取名称；商品已删时名称为空） */
    List<CouponTemplateDetailVO.ScopeNode> selectScopeWithProduct(@Param("couponId") Long couponId);

    /** 模板指定商品 target_id 列表（无行 = 全场券） */
    List<Long> selectTargetIds(@Param("couponId") Long couponId);
}
