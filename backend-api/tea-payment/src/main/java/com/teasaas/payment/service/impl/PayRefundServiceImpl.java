package com.teasaas.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.inventory.constant.StockBizType;
import com.teasaas.member.service.MemPointsService;
import com.teasaas.member.service.MemWalletService;
import com.teasaas.order.entity.OrdOrder;
import com.teasaas.order.mapper.OrdOrderMapper;
import com.teasaas.order.service.OrdOrderService;
import com.teasaas.payment.dto.RefundApplyDTO;
import com.teasaas.payment.entity.PayPayment;
import com.teasaas.payment.entity.PayRefund;
import com.teasaas.payment.mapper.PayPaymentMapper;
import com.teasaas.payment.mapper.PayRefundMapper;
import com.teasaas.payment.service.PayRefundService;
import com.teasaas.payment.vo.RefundVO;
import com.teasaas.system.entity.SysUserStore;
import com.teasaas.system.mapper.SysUserStoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 退款服务实现（F-PAY-02，接口需求 9.2，P0-5 决策）
 *
 * <p>一期仅整单退（amount=实付）：微信原路退（mock 即时成功），
 * 储值单（pay_channel=2）即时退回钱包（refundType=2）；已核销券不退还
 * （coupon_returned=0），积分全额回补（bizType=3，bizNo=orderNo 幂等）。
 * 退款成功 = 退款单置 2 + 订单条件更新 →7（已退款）+ 库存回补
 * （inv_stock_log bizType=3，bizNo=orderNo 幂等）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayRefundServiceImpl implements PayRefundService {

    private static final String ROLE_R2 = "R2";
    private static final String ROLE_R6 = "R6";
    private static final String ROLE_R7 = "R7";

    /** 订单状态：2已支付/3制作中/4待取餐 可退；7已退款 */
    private static final int ORDER_STATUS_PAID = 2;
    private static final int ORDER_STATUS_MAKING = 3;
    private static final int ORDER_STATUS_READY = 4;
    private static final int ORDER_STATUS_REFUNDED = 7;

    /** 流水/退款状态 */
    private static final int PAY_STATUS_SUCCESS = 2;
    private static final int REFUND_PENDING = 1;
    private static final int REFUND_SUCCESS = 2;

    /** 支付渠道：1微信/2储值 */
    private static final int CHANNEL_BALANCE = 2;

    /** 退款类型：1原路（微信线上/现金线下）/2退储值 */
    private static final int REFUND_TYPE_ORIGIN = 1;
    private static final int REFUND_TYPE_BALANCE = 2;

    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PayRefundMapper payRefundMapper;
    private final PayPaymentMapper payPaymentMapper;
    private final OrdOrderMapper ordOrderMapper;
    private final OrdOrderService ordOrderService;
    private final MemPointsService memPointsService;
    private final MemWalletService memWalletService;
    private final SysUserStoreMapper sysUserStoreMapper;

    /** mock 网关开关：true 退款即时成功；正式模式退款单置 1 处理中待微信回调 */
    @Value("${tea.pay.mock:true}")
    private boolean mock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO apply(Long orderId, RefundApplyDTO dto) {
        List<String> roles = SecurityUtils.getRolesFromSession();
        boolean hq = roles.contains(ROLE_R2);
        if (!hq && !roles.contains(ROLE_R6) && !roles.contains(ROLE_R7)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部/加盟商/店长可操作退款");
        }

        OrdOrder order = ordOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!hq && !listBoundStoreIds().contains(order.getStoreId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "非名下门店订单");
        }
        Integer status = order.getStatus();
        if (status == null || (status != ORDER_STATUS_PAID && status != ORDER_STATUS_MAKING
                && status != ORDER_STATUS_READY)) {
            throw new BizException(ErrorCode.REFUND_NOT_ALLOWED, "当前订单状态不可退款");
        }

        // 防重：已有处理中/成功的退款单
        Long dup = payRefundMapper.selectCount(new LambdaQueryWrapper<PayRefund>()
                .eq(PayRefund::getOrderId, orderId)
                .in(PayRefund::getStatus, REFUND_PENDING, REFUND_SUCCESS));
        if (dup != null && dup > 0) {
            throw new BizException(ErrorCode.REFUND_NOT_ALLOWED, "该订单已申请退款");
        }

        PayPayment payment = payPaymentMapper.selectOne(new LambdaQueryWrapper<PayPayment>()
                .eq(PayPayment::getOrderId, orderId)
                .eq(PayPayment::getStatus, PAY_STATUS_SUCCESS)
                .last("LIMIT 1"));
        // 储值单（pay_channel=2）无微信流水：扣款在钱包侧，退款退回储值（P0-4）
        boolean balanceRefund = payment == null;
        if (payment != null && payment.getChannel() != null && payment.getChannel() == CHANNEL_BALANCE) {
            balanceRefund = true;
            payment = null;
        }
        if (balanceRefund && (order.getPayChannel() == null
                || order.getPayChannel() != CHANNEL_BALANCE || order.getMemberId() == null)) {
            throw new BizException(ErrorCode.REFUND_NOT_ALLOWED, "无成功支付流水");
        }

        // 退款单落库（1处理中；mock 即时置 2）
        PayRefund refund = new PayRefund();
        refund.setTenantId(order.getTenantId());
        refund.setRefundNo(genNo("RF"));
        refund.setOrderId(order.getId());
        refund.setPaymentId(payment == null ? null : payment.getId());
        refund.setAmount(order.getPayAmount());
        refund.setRefundType(balanceRefund ? REFUND_TYPE_BALANCE : REFUND_TYPE_ORIGIN);
        refund.setCouponReturned(0);
        refund.setReason(dto.getReason());
        refund.setStatus(REFUND_PENDING);
        refund.setCreateTime(LocalDateTime.now());
        try {
            payRefundMapper.insert(refund);
        } catch (DuplicateKeyException e) {
            refund.setRefundNo(genNo("RF"));
            payRefundMapper.insert(refund);
        }
        if (mock) {
            payRefundMapper.update(null, new LambdaUpdateWrapper<PayRefund>()
                    .set(PayRefund::getStatus, REFUND_SUCCESS)
                    .set(PayRefund::getRefundTime, LocalDateTime.now())
                    .eq(PayRefund::getId, refund.getId())
                    .eq(PayRefund::getStatus, REFUND_PENDING));
            refund.setStatus(REFUND_SUCCESS);
            refund.setRefundTime(LocalDateTime.now());
        }

        // 订单 →7 已退款（条件更新防并发重复退）
        int affected = ordOrderMapper.update(null, new LambdaUpdateWrapper<OrdOrder>()
                .set(OrdOrder::getStatus, ORDER_STATUS_REFUNDED)
                .eq(OrdOrder::getId, orderId)
                .in(OrdOrder::getStatus, ORDER_STATUS_PAID, ORDER_STATUS_MAKING, ORDER_STATUS_READY));
        if (affected == 0) {
            throw new BizException(ErrorCode.REFUND_NOT_ALLOWED, "订单状态已变更，请刷新重试");
        }

        // 资金退回：储值即时回钱包（无外部通道）；微信 mock 即时，正式走退款回调
        if (balanceRefund) {
            memWalletService.refundOfOrder(order.getMemberId(), order.getOrderNo(), order.getPayAmount());
        }
        // 积分全额回补（F-MEM-02：bizType=3，bizNo=orderNo 幂等）；券不退还（PRD 一期）
        if (order.getMemberId() != null && order.getPointsUsed() != null && order.getPointsUsed() > 0) {
            memPointsService.refundOfOrder(order.getMemberId(), order.getOrderNo(), order.getPointsUsed());
        }

        // 库存回补（bizNo=orderNo + bizType=3 幂等）
        ordOrderService.restoreOrderStock(orderId, StockBizType.REFUND_RESTORE);
        log.info("退款完成 refundNo={} orderNo={} amount={}分 type={} mock={}",
                refund.getRefundNo(), order.getOrderNo(), refund.getAmount(),
                balanceRefund ? "退储值" : "微信原路", mock);

        return toVO(refund, order.getOrderNo());
    }

    @Override
    public PageResult<PayRefund> page(int page, int size) {
        List<String> roles = SecurityUtils.getRolesFromSession();
        if (!roles.contains(ROLE_R2) && !roles.contains(ROLE_R6) && !roles.contains(ROLE_R7)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部/加盟商/店长可查看退款");
        }
        LambdaQueryWrapper<PayRefund> wrapper = new LambdaQueryWrapper<PayRefund>();
        if (!roles.contains(ROLE_R2)) {
            List<Long> orderIds = ordOrderMapper.selectList(new LambdaQueryWrapper<OrdOrder>()
                            .in(OrdOrder::getStoreId, listBoundStoreIds()))
                    .stream().map(OrdOrder::getId).toList();
            if (orderIds.isEmpty()) {
                return PageResult.of(List.of(), 0L, page, size);
            }
            wrapper.in(PayRefund::getOrderId, orderIds);
        }
        wrapper.orderByDesc(PayRefund::getId);
        Page<PayRefund> result = payRefundMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    // ==================== 私有工具 ====================

    private RefundVO toVO(PayRefund refund, String orderNo) {
        RefundVO vo = new RefundVO();
        vo.setRefundNo(refund.getRefundNo());
        vo.setOrderId(refund.getOrderId());
        vo.setOrderNo(orderNo);
        vo.setPaymentId(refund.getPaymentId());
        vo.setAmount(refund.getAmount());
        vo.setRefundType(refund.getRefundType());
        vo.setCouponReturned(refund.getCouponReturned());
        vo.setReason(refund.getReason());
        vo.setStatus(refund.getStatus());
        vo.setRefundTime(refund.getRefundTime());
        vo.setCreateTime(refund.getCreateTime());
        return vo;
    }

    /** 单号 = 前缀 + yyyyMMdd + 6 位随机 */
    private String genNo(String prefix) {
        return prefix + LocalDate.now().format(NO_DATE)
                + ThreadLocalRandom.current().nextInt(100000, 1000000);
    }

    private List<Long> listBoundStoreIds() {
        return sysUserStoreMapper.selectList(
                        new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, SecurityUtils.getUserId()))
                .stream().map(SysUserStore::getStoreId).distinct().toList();
    }
}
