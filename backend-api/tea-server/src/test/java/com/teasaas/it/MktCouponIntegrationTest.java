package com.teasaas.it;

import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.marketing.dto.CouponScopeItemDTO;
import com.teasaas.marketing.entity.MktCoupon;
import com.teasaas.marketing.entity.MktCouponScope;
import com.teasaas.marketing.entity.MktCouponTemplate;
import com.teasaas.marketing.mapper.MktCouponMapper;
import com.teasaas.marketing.mapper.MktCouponScopeMapper;
import com.teasaas.marketing.mapper.MktCouponTemplateMapper;
import com.teasaas.marketing.service.MktCouponService;
import com.teasaas.marketing.vo.CouponVO;
import com.teasaas.member.mapper.MemMemberMapper;
import com.teasaas.order.mapper.OrdOrderMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 券核销并发与指定商品券计价集成测试（Review §4-4 资金链路；M11 scope 口径回归）
 *
 * <p>不变量：核销条件更新 status 1→2，并发仅一笔成功；
 * 指定商品券门槛/优惠均以适用商品小计 base 计价，满减优惠 min(amount, base) 封顶。</p>
 */
class MktCouponIntegrationTest extends IntegrationTestBase {

    private static final long PRODUCT_MATCH = 880_001L;
    private static final long PRODUCT_OTHER = 880_002L;

    private final MktCouponService mktCouponService;
    private final MktCouponTemplateMapper templateMapper;
    private final MktCouponScopeMapper scopeMapper;
    private final MktCouponMapper couponMapper;

    MktCouponIntegrationTest(MktCouponService mktCouponService, MktCouponTemplateMapper templateMapper,
                             MktCouponScopeMapper scopeMapper, MktCouponMapper couponMapper,
                             MemMemberMapper memMemberMapper, OrdOrderMapper ordOrderMapper) {
        super(memMemberMapper, ordOrderMapper);
        this.mktCouponService = mktCouponService;
        this.templateMapper = templateMapper;
        this.scopeMapper = scopeMapper;
        this.couponMapper = couponMapper;
    }

    /** 满减券模板（type=1；threshold/amount 由用例给），返回已领券对（memberId, couponId） */
    private ClaimedCoupon fullCutCoupon(long threshold, long amount) {
        MktCouponTemplate template = new MktCouponTemplate();
        template.setTenantId(TENANT_ID);
        template.setName("IT满减券");
        template.setType(1);
        template.setThreshold(threshold);
        template.setAmount(amount);
        template.setTotalQty(-1);
        template.setPerLimit(5);
        template.setValidType(1);
        template.setValidStart(LocalDateTime.now().minusDays(1));
        template.setValidEnd(LocalDateTime.now().plusDays(1));
        template.setStatus(1);
        template.setCreateTime(LocalDateTime.now());
        templateMapper.insert(template);
        return claim(template.getId());
    }

    private ClaimedCoupon claim(Long templateId) {
        Long memberId = insertMember().getId();
        CouponVO vo = mktCouponService.claim(templateId, memberId);
        return new ClaimedCoupon(memberId, vo.getId());
    }

    private void bindProductScope(long templateId, long productId) {
        MktCouponScope scope = new MktCouponScope();
        scope.setTenantId(TENANT_ID);
        scope.setCouponId(templateId); // 沿用生产语义：scope.coupon_id 存模板ID
        scope.setScopeType(2);
        scope.setTargetId(productId);
        scopeMapper.insert(scope);
    }

    @Test
    void fullCutDiscountEqualsFaceValueAboveThreshold() {
        ClaimedCoupon coupon = fullCutCoupon(3_000L, 500L);
        assertEquals(500L, mktCouponService.calcDiscount(coupon.couponId(), coupon.memberId(), 5_000L, null));
    }

    @Test
    void scopedCouponPricesOnMatchedSubtotalOnly() {
        // 场景1：整单 5000 但适用商品小计 1000 < 门槛 3000 → 拒
        ClaimedCoupon coupon = fullCutCoupon(3_000L, 500L);
        bindProductScope(templateIdOf(coupon.couponId()), PRODUCT_MATCH);
        BizException rejected = assertThrows(BizException.class, () -> mktCouponService.calcDiscount(
                coupon.couponId(), coupon.memberId(), 5_000L,
                List.of(new CouponScopeItemDTO(PRODUCT_MATCH, 1_000L),
                        new CouponScopeItemDTO(PRODUCT_OTHER, 4_000L))));
        assertEquals(ErrorCode.COUPON_NOT_AVAILABLE.getCode(), rejected.getCode());

        // 场景2：无门槛模板，base=300 < 面额 500 → 优惠按 base 封顶 = 300
        ClaimedCoupon capped = fullCutCoupon(0L, 500L);
        bindProductScope(templateIdOf(capped.couponId()), PRODUCT_MATCH);
        assertEquals(300L, mktCouponService.calcDiscount(capped.couponId(), capped.memberId(), 5_000L,
                List.of(new CouponScopeItemDTO(PRODUCT_MATCH, 300L),
                        new CouponScopeItemDTO(PRODUCT_OTHER, 4_700L))));
    }

    @Test
    void concurrentUseSameCouponExactlyOneWins() throws Exception {
        ClaimedCoupon coupon = fullCutCoupon(3_000L, 500L);
        long orderId = nextId();

        List<Object> outcomes = runConcurrently(2, i ->
                mktCouponService.useCoupon(coupon.couponId(), coupon.memberId(), orderId, 5_000L, null));

        assertEquals(1, countOk(outcomes), "核销仅一笔成功，实际出口=" + outcomes);
        assertEquals(1, countBiz(outcomes, ErrorCode.COUPON_NOT_AVAILABLE));
        MktCoupon used = couponMapper.selectById(coupon.couponId());
        assertEquals(2, used.getStatus(), "券终态=已用");
        assertEquals(orderId, used.getUseOrderId());
    }

    @Test
    void useCouponBelowThresholdIsRejected() {
        ClaimedCoupon coupon = fullCutCoupon(3_000L, 500L);

        BizException rejected = assertThrows(BizException.class, () -> mktCouponService.useCoupon(
                coupon.couponId(), coupon.memberId(), nextId(), 2_000L, null));
        assertEquals(ErrorCode.COUPON_NOT_AVAILABLE.getCode(), rejected.getCode());
        assertEquals(1, couponMapper.selectById(coupon.couponId()).getStatus(), "拒单后券仍为未用");
    }

    private long templateIdOf(long couponId) {
        return couponMapper.selectById(couponId).getTemplateId();
    }

    private record ClaimedCoupon(Long memberId, Long couponId) {
    }
}
