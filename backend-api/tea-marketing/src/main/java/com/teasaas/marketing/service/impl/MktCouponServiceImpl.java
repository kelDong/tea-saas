package com.teasaas.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.marketing.dto.CouponScopeItemDTO;
import com.teasaas.marketing.entity.MktCoupon;
import com.teasaas.marketing.entity.MktCouponScope;
import com.teasaas.marketing.entity.MktCouponTemplate;
import com.teasaas.marketing.mapper.MktCouponMapper;
import com.teasaas.marketing.mapper.MktCouponScopeMapper;
import com.teasaas.marketing.mapper.MktCouponTemplateMapper;
import com.teasaas.marketing.service.MktCouponService;
import com.teasaas.marketing.vo.CouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户券服务实现（F-MKT-01，接口需求 11.1）
 *
 * <p>核销走 status 1→2 条件更新防并发重复核销；总量控制一期以
 * count(mkt_coupon) 近似（模板无剩余量字段，极端并发少量超发可接受）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MktCouponServiceImpl implements MktCouponService {

    private static final int STATUS_UNUSED = 1;
    private static final int STATUS_USED = 2;
    private static final int TYPE_FULL_CUT = 1;
    private static final int VALID_TYPE_FIXED = 1;
    private static final int UNLIMITED_QTY = -1;

    private static final int SCOPE_PRODUCT = 2;

    private final MktCouponMapper mktCouponMapper;
    private final MktCouponTemplateMapper mktCouponTemplateMapper;
    private final MktCouponScopeMapper mktCouponScopeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponVO claim(Long templateId, Long memberId) {
        MktCouponTemplate template = mktCouponTemplateMapper.selectById(templateId);
        if (template == null || template.getStatus() == null || template.getStatus() != 1) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND, "券模板不存在或已停用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (template.getValidType() == VALID_TYPE_FIXED && now.isAfter(template.getValidEnd())) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE, "券已过领取截止时间");
        }
        if (template.getTotalQty() != null && template.getTotalQty() != UNLIMITED_QTY
                && mktCouponMapper.selectCount(new LambdaQueryWrapper<MktCoupon>()
                        .eq(MktCoupon::getTemplateId, templateId))
                        >= template.getTotalQty()) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE, "券已抢完");
        }
        if (template.getPerLimit() != null
                && mktCouponMapper.selectCount(new LambdaQueryWrapper<MktCoupon>()
                        .eq(MktCoupon::getTemplateId, templateId)
                        .eq(MktCoupon::getMemberId, memberId))
                        >= template.getPerLimit()) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE, "已达每人限领数量");
        }

        MktCoupon coupon = new MktCoupon();
        coupon.setTenantId(TenantContext.getTenantId());
        coupon.setTemplateId(templateId);
        coupon.setMemberId(memberId);
        coupon.setStatus(STATUS_UNUSED);
        coupon.setReceiveTime(now);
        if (template.getValidType() == VALID_TYPE_FIXED) {
            coupon.setValidStart(template.getValidStart());
            coupon.setValidEnd(template.getValidEnd());
        } else {
            coupon.setValidStart(now);
            coupon.setValidEnd(now.plusDays(template.getValidDays()));
        }
        mktCouponMapper.insert(coupon);
        CouponVO vo = toVO(coupon, template);
        vo.setScopeType(resolveScopeType(templateId));
        return vo;
    }

    @Override
    public PageResult<CouponVO> myCoupons(Long memberId, Integer status, int page, int size) {
        Page<MktCoupon> result = mktCouponMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<MktCoupon>()
                        .eq(MktCoupon::getMemberId, memberId)
                        .eq(status != null, MktCoupon::getStatus, status)
                        .orderByDesc(MktCoupon::getId));
        List<MktCoupon> records = result.getRecords();
        Map<Long, MktCouponTemplate> templates = records.isEmpty()
                ? Map.of()
                : mktCouponTemplateMapper.selectBatchIds(records.stream()
                        .map(MktCoupon::getTemplateId).distinct().toList())
                .stream().collect(Collectors.toMap(MktCouponTemplate::getId, Function.identity()));
        // 指定商品模板集合（scope_type=2 存在行即指定；批量一次查询防 N+1）
        Set<Long> productScoped = records.isEmpty() ? Set.of()
                : mktCouponScopeMapper.selectList(new LambdaQueryWrapper<MktCouponScope>()
                        .in(MktCouponScope::getCouponId, templates.keySet())
                        .eq(MktCouponScope::getScopeType, SCOPE_PRODUCT))
                .stream().map(MktCouponScope::getCouponId).collect(Collectors.toSet());
        return PageResult.of(records.stream()
                .map(c -> {
                    CouponVO vo = toVO(c, templates.get(c.getTemplateId()));
                    vo.setScopeType(productScoped.contains(c.getTemplateId())
                            ? SCOPE_PRODUCT : 1);
                    return vo;
                }).toList(),
                result.getTotal(), page, size);
    }

    @Override
    public long calcDiscount(Long couponId, Long memberId, long orderAmount,
                             List<CouponScopeItemDTO> items) {
        MktCoupon coupon = requireCoupon(couponId, memberId);
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStatus() != STATUS_UNUSED
                || now.isBefore(coupon.getValidStart()) || now.isAfter(coupon.getValidEnd())) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE, "券不在可用有效期");
        }
        MktCouponTemplate template = mktCouponTemplateMapper.selectById(coupon.getTemplateId());
        if (template == null) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        // 计价基数：指定商品券 = 适用商品小计（base），全场券 = 整单金额
        List<Long> targets = mktCouponScopeMapper.selectTargetIds(template.getId());
        long base = orderAmount;
        if (targets != null && !targets.isEmpty()) {
            Set<Long> targetSet = new HashSet<>(targets);
            base = items == null ? 0L : items.stream()
                    .filter(i -> i.getProductId() != null && targetSet.contains(i.getProductId()))
                    .mapToLong(i -> i.getAmount() == null ? 0L : i.getAmount()).sum();
        }
        if (base <= 0) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE, "订单不含券适用商品");
        }
        if (template.getThreshold() != null && template.getThreshold() > 0
                && base < template.getThreshold()) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE, "未满足券使用门槛");
        }
        if (template.getType() == TYPE_FULL_CUT) {
            // 满减：优惠=面额，且不超过计价基数（保护极端小额单）
            return Math.min(template.getAmount(), base);
        }
        // 折扣：基数部分实付 = base × 折扣率（向下取整），优惠 = 差额（≤base≤整单）
        long payable = BigDecimal.valueOf(base)
                .multiply(template.getDiscount())
                .setScale(0, RoundingMode.FLOOR).longValueExact();
        return base - payable;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long useCoupon(Long couponId, Long memberId, Long orderId, long orderAmount,
                          List<CouponScopeItemDTO> items) {
        long discount = calcDiscount(couponId, memberId, orderAmount, items); // 校验 + 试算
        int rows = mktCouponMapper.update(null, new LambdaUpdateWrapper<MktCoupon>()
                .set(MktCoupon::getStatus, STATUS_USED)
                .set(MktCoupon::getUseOrderId, orderId)
                .set(MktCoupon::getUseTime, LocalDateTime.now())
                .eq(MktCoupon::getId, couponId)
                .eq(MktCoupon::getMemberId, memberId)
                .eq(MktCoupon::getStatus, STATUS_UNUSED)); // 条件更新防并发重复核销
        if (rows == 0) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE, "券已被使用或不可用");
        }
        return discount;
    }

    // ==================== 私有工具 ====================

    /** 模板范围类型：存在 scope_type=2 行 = 2 指定商品，否则 1 全场 */
    private int resolveScopeType(Long templateId) {
        List<Long> targets = mktCouponScopeMapper.selectTargetIds(templateId);
        return targets == null || targets.isEmpty() ? 1 : SCOPE_PRODUCT;
    }

    private MktCoupon requireCoupon(Long couponId, Long memberId) {
        MktCoupon coupon = mktCouponMapper.selectById(couponId);
        if (coupon == null || !coupon.getMemberId().equals(memberId)) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE, "券不存在");
        }
        return coupon;
    }

    private CouponVO toVO(MktCoupon coupon, MktCouponTemplate template) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setTemplateId(coupon.getTemplateId());
        vo.setStatus(coupon.getStatus());
        vo.setValidStart(coupon.getValidStart());
        vo.setValidEnd(coupon.getValidEnd());
        if (template != null) {
            vo.setName(template.getName());
            vo.setType(template.getType());
            vo.setThreshold(template.getThreshold());
            vo.setAmount(template.getAmount());
            vo.setDiscount(template.getDiscount());
        }
        return vo;
    }
}
