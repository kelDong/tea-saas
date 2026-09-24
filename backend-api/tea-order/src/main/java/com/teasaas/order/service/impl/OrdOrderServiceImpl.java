package com.teasaas.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.bom.entity.BomFormulaItem;
import com.teasaas.bom.mapper.BomFormulaItemMapper;
import com.teasaas.common.constant.RedisConstants;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.inventory.constant.StockBizType;
import com.teasaas.inventory.dto.StockItemDTO;
import com.teasaas.inventory.service.InvStockService;
import com.teasaas.marketing.service.MktCouponService;
import com.teasaas.member.service.MemPointsService;
import com.teasaas.member.service.MemWalletService;
import com.teasaas.order.dto.AppOrderCreateDTO;
import com.teasaas.order.dto.AppOrderPreviewDTO;
import com.teasaas.order.dto.OrderItemDTO;
import com.teasaas.order.dto.PosOrderCreateDTO;
import com.teasaas.order.entity.OrdOrder;
import com.teasaas.order.entity.OrdOrderItem;
import com.teasaas.order.mapper.OrdOrderItemMapper;
import com.teasaas.order.mapper.OrdOrderMapper;
import com.teasaas.order.service.OrdOrderService;
import com.teasaas.order.vo.OrderCreateVO;
import com.teasaas.order.vo.OrderDetailVO;
import com.teasaas.order.vo.OrderPreviewVO;
import com.teasaas.product.entity.PrdListing;
import com.teasaas.product.entity.PrdProduct;
import com.teasaas.product.entity.PrdProductSpec;
import com.teasaas.product.entity.PrdProductSpecOption;
import com.teasaas.product.mapper.PrdListingMapper;
import com.teasaas.product.mapper.PrdProductMapper;
import com.teasaas.product.mapper.PrdProductSpecMapper;
import com.teasaas.product.mapper.PrdProductSpecOptionMapper;
import com.teasaas.system.entity.SysUserStore;
import com.teasaas.system.entity.Tenant;
import com.teasaas.system.mapper.SysUserStoreMapper;
import com.teasaas.system.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务实现（F-ORD-01~04，接口需求 8.1-8.4）
 *
 * <p>下单即扣库存（数据库设计 13.1）：订单+明细+扣减同一事务，库存不足整单失败；
 * 取消/超时回补（bizType=2）、退款回补（bizType=3）均以 orderNo 幂等。
 * C 端方法无登录态：租户由 X-Tenant-Id 手动注入 TenantContext，会员由 X-Member-Id 传入
 * （F-MEM-01 微信登录落地前的开发桥接，批次 4 替换为会话身份）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrdOrderServiceImpl implements OrdOrderService {

    private static final String ROLE_R2 = "R2";
    private static final String ROLE_R6 = "R6";
    private static final String ROLE_R7 = "R7";
    private static final String ROLE_R8 = "R8";

    /** 状态机：1待支付/2已支付/3制作中/4待取餐/5已完成/6已取消/7已退款 */
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_PAID = 2;
    private static final int STATUS_MAKING = 3;
    private static final int STATUS_WAIT_TAKE = 4;
    private static final int STATUS_FINISHED = 5;
    private static final int STATUS_CANCELLED = 6;

    /** 来源：1小程序/2门店收银 */
    private static final int SOURCE_APP = 1;
    private static final int SOURCE_POS = 2;

    /** C 端支付渠道（订单表）：1微信/2储值（P0-4：储值独立通道，不与微信混合） */
    private static final int CHANNEL_WX = 1;
    private static final int CHANNEL_WALLET = 2;

    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrdOrderMapper ordOrderMapper;
    private final OrdOrderItemMapper ordOrderItemMapper;
    private final PrdListingMapper prdListingMapper;
    private final PrdProductMapper prdProductMapper;
    private final PrdProductSpecMapper prdProductSpecMapper;
    private final PrdProductSpecOptionMapper prdProductSpecOptionMapper;
    private final BomFormulaItemMapper bomFormulaItemMapper;
    private final InvStockService invStockService;
    private final MktCouponService mktCouponService;
    private final MemPointsService memPointsService;
    private final MemWalletService memWalletService;
    private final SysUserStoreMapper sysUserStoreMapper;
    private final TenantMapper tenantMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;

    /** 支付超时分钟数（P1-5 决策，配置 tea.order.pay-timeout-minutes） */
    @Value("${tea.order.pay-timeout-minutes:15}")
    private int payTimeoutMinutes;

    // ==================== B 端：查询 ====================

    @Override
    public PageResult<OrdOrder> page(int page, int size, Long storeId, Integer status) {
        LambdaQueryWrapper<OrdOrder> wrapper = new LambdaQueryWrapper<OrdOrder>()
                .eq(storeId != null, OrdOrder::getStoreId, storeId)
                .eq(status != null, OrdOrder::getStatus, status)
                .orderByDesc(OrdOrder::getId);
        // 数据权限：R2 读全租户；R6/R7/R8 按名下门店过滤
        if (!hasRole(ROLE_R2)) {
            List<Long> storeIds = listBoundStoreIds();
            if (storeIds.isEmpty()) {
                return PageResult.empty(page, size);
            }
            wrapper.in(OrdOrder::getStoreId, storeIds);
        }
        Page<OrdOrder> result = ordOrderMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public OrderDetailVO detail(Long id) {
        OrdOrder order = requireExists(id);
        checkOrderReadAccess(order);
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStoreId(order.getStoreId());
        vo.setMemberId(order.getMemberId());
        vo.setOrderSource(order.getOrderSource());
        vo.setStatus(order.getStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setPayChannel(order.getPayChannel());
        vo.setTakeCode(order.getTakeCode());
        vo.setPayTime(order.getPayTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(ordOrderItemMapper.selectList(
                        new LambdaQueryWrapper<OrdOrderItem>().eq(OrdOrderItem::getOrderId, id))
                .stream().map(item -> {
                    OrderDetailVO.OrderItemNode node = new OrderDetailVO.OrderItemNode();
                    node.setProductId(item.getProductId());
                    node.setListingId(item.getListingId());
                    node.setProductName(item.getProductName());
                    node.setSpecDesc(item.getSpecDesc());
                    node.setPrice(item.getPrice());
                    node.setQty(item.getQty());
                    node.setAmount(item.getAmount());
                    return node;
                }).toList());
        return vo;
    }

    // ==================== B 端：接单流转（F-ORD-03） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long id) {
        transition(id, STATUS_PAID, STATUS_MAKING, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void make(Long id) {
        transition(id, STATUS_MAKING, STATUS_WAIT_TAKE, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long id) {
        transition(id, STATUS_WAIT_TAKE, STATUS_FINISHED, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, String reason) {
        OrdOrder order = requireExists(id);
        checkCancelAccess(order.getStoreId());
        int affected = ordOrderMapper.update(null, new LambdaUpdateWrapper<OrdOrder>()
                .set(OrdOrder::getStatus, STATUS_CANCELLED)
                .set(OrdOrder::getCancelReason,
                        reason == null || reason.isBlank() ? "门店取消" : reason.trim())
                .eq(OrdOrder::getId, id)
                .eq(OrdOrder::getStatus, STATUS_PENDING));
        if (affected == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }
        restoreOrderStock(id, StockBizType.TIMEOUT_RESTORE);
        refundPointsQuietly(order); // 积分回补（bizNo=orderNo 幂等）；券不退还（PRD 一期）
    }

    // ==================== C 端（X-Tenant-Id / X-Member-Id 注入） ====================

    @Override
    public OrderPreviewVO preview(AppOrderPreviewDTO dto, Long memberId) {
        List<AssembledItem> assembled = assemble(dto.getStoreId(), dto.getItems());
        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setItems(assembled.stream().map(item -> {
            OrderPreviewVO.PreviewItemNode node = new OrderPreviewVO.PreviewItemNode();
            node.setProductName(item.productName());
            node.setSpecDesc(item.specDesc());
            node.setPrice(item.price());
            node.setQty(item.qty());
            node.setAmount(item.amount());
            return node;
        }).toList());
        long total = assembled.stream().mapToLong(AssembledItem::amount).sum();
        vo.setTotalAmount(total);

        // 券优惠（不可用直接抛错，C 端可感知换券）
        long couponAmount = 0L;
        if (dto.getCouponId() != null) {
            requireMemberForBenefit(memberId, "使用优惠券");
            couponAmount = mktCouponService.calcDiscount(dto.getCouponId(), memberId, total);
        }
        // 积分抵扣（超限按可抵上限截断，与 appCreate 同规则）
        int pointsUsed = 0;
        long pointsAmount = 0L;
        if (dto.getPointsUsed() != null && dto.getPointsUsed() > 0) {
            requireMemberForBenefit(memberId, "积分抵扣");
            int available = memPointsService.availablePoints(memberId);
            pointsUsed = Math.min(dto.getPointsUsed(), available);
            pointsUsed = (int) Math.min(pointsUsed, capPoints(total - couponAmount));
            pointsAmount = (long) pointsUsed * memPointsService.pointValue();
        }
        vo.setCouponAmount(couponAmount);
        vo.setPointsUsed(pointsUsed > 0 ? pointsUsed : null);
        vo.setPointsAmount(pointsAmount);
        vo.setDiscountAmount(couponAmount + pointsAmount);
        vo.setPayAmount(total - couponAmount - pointsAmount);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO appCreate(AppOrderCreateDTO dto, Long memberId) {
        if (dto.getPayChannel() == null
                || (dto.getPayChannel() != CHANNEL_WX && dto.getPayChannel() != CHANNEL_WALLET)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "支付渠道仅支持微信/储值");
        }
        List<AssembledItem> assembled = assemble(dto.getStoreId(), dto.getItems());
        long total = assembled.stream().mapToLong(AssembledItem::amount).sum();

        // 优惠试算（核销/扣减在落单后同事务执行，任一失败整单回滚）
        boolean useCoupon = dto.getCouponId() != null;
        long couponAmount = useCoupon
                ? mktCouponService.calcDiscount(dto.getCouponId(), memberId, total) : 0L;
        int pointsUsed = 0;
        long pointsAmount = 0L;
        if (dto.getPointsUsed() != null && dto.getPointsUsed() > 0) {
            // 超限按可抵上限截断（实付不为负）；可用余额由 useForOrder 校验
            pointsUsed = (int) Math.min(dto.getPointsUsed(), capPoints(total - couponAmount));
            pointsAmount = (long) pointsUsed * memPointsService.pointValue();
        }
        long payAmount = total - couponAmount - pointsAmount;

        OrdOrder order = new OrdOrder();
        order.setTenantId(TenantContext.getTenantId());
        order.setStoreId(dto.getStoreId());
        order.setMemberId(memberId);
        order.setOrderSource(SOURCE_APP);
        order.setStatus(STATUS_PENDING);
        order.setTotalAmount(total);
        order.setDiscountAmount(couponAmount + pointsAmount);
        order.setPayAmount(payAmount);
        order.setCouponId(useCoupon ? dto.getCouponId() : null);
        order.setCouponAmount(useCoupon ? couponAmount : null);
        order.setPointsUsed(pointsUsed > 0 ? pointsUsed : null);
        order.setPointsAmount(pointsUsed > 0 ? pointsAmount : null);
        order.setBalanceAmount(dto.getPayChannel() == CHANNEL_WALLET ? payAmount : null);
        order.setPayChannel(dto.getPayChannel());
        order.setTakeCode(nextTakeCode(dto.getStoreId()));
        insertWithOrderNoRetry(order);
        persistOrderItems(order, assembled);

        if (useCoupon) {
            mktCouponService.useCoupon(dto.getCouponId(), memberId, order.getId(), total);
        }
        if (pointsUsed > 0) {
            memPointsService.useForOrder(memberId, order.getOrderNo(), pointsUsed);
        }
        if (dto.getPayChannel() == CHANNEL_WALLET) {
            // 储值独立通道（P0-4）：下单即扣余额、即时支付，全额抵扣 0 元单免扣款
            if (payAmount > 0) {
                memWalletService.payForOrder(memberId, order.getOrderNo(), payAmount);
            }
            int paid = ordOrderMapper.update(null, new LambdaUpdateWrapper<OrdOrder>()
                    .set(OrdOrder::getStatus, STATUS_PAID)
                    .set(OrdOrder::getPayTime, LocalDateTime.now())
                    .eq(OrdOrder::getId, order.getId())
                    .eq(OrdOrder::getStatus, STATUS_PENDING));
            if (paid == 0) {
                throw new BizException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
            }
            // 储值单不经支付模块回调，订单侧直接发积分（bizNo=orderNo 幂等）
            memPointsService.earnOnPaid(memberId, order.getOrderNo(), payAmount);
        }

        // 下单即扣库存（13.1 条件更新；orderNo 幂等；不足整单回滚）
        deductStockByAssembled(order, dto.getStoreId(), assembled);

        OrderCreateVO vo = new OrderCreateVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTakeCode(order.getTakeCode());
        vo.setPayAmount(order.getPayAmount());
        return vo;
    }

    @Override
    public PageResult<OrdOrder> appPage(int page, int size, Long memberId) {
        Page<OrdOrder> result = ordOrderMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<OrdOrder>()
                        .eq(OrdOrder::getMemberId, memberId)
                        .orderByDesc(OrdOrder::getId));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appCancel(Long orderId, Long memberId) {
        OrdOrder order = ordOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!Objects.equals(order.getMemberId(), memberId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "非本人订单");
        }
        int affected = ordOrderMapper.update(null, new LambdaUpdateWrapper<OrdOrder>()
                .set(OrdOrder::getStatus, STATUS_CANCELLED)
                .set(OrdOrder::getCancelReason, "顾客取消")
                .eq(OrdOrder::getId, orderId)
                .eq(OrdOrder::getStatus, STATUS_PENDING));
        if (affected == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }
        restoreOrderStock(orderId, StockBizType.TIMEOUT_RESTORE);
        refundPointsQuietly(order); // 积分回补（bizNo=orderNo 幂等）；券不退还（PRD 一期）
    }

    // ==================== POS 手工收银（F-ORD-04，R7/R8） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO posCreate(PosOrderCreateDTO dto) {
        requirePosRole();
        checkStoreAccess(dto.getStoreId());
        return createOrder(dto.getStoreId(), dto.getItems(), null, SOURCE_POS, null);
    }

    // ==================== 供 tea-payment / tea-job 调用 ====================

    @Override
    public void restoreOrderStock(Long orderId, int bizType) {
        OrdOrder order = ordOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        Map<Long, Integer> formulaQty = aggregateFormulaQty(orderId);
        invStockService.restore(order.getOrderNo(), order.getStoreId(),
                toStockItems(aggregateMaterials(formulaQty)), bizType);
    }

    @Override
    public int cancelExpiredOrders() {
        int cancelled = 0;
        List<Tenant> tenants = tenantMapper.selectList(
                new LambdaQueryWrapper<Tenant>().eq(Tenant::getStatus, 1));
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(payTimeoutMinutes);
        for (Tenant tenant : tenants) {
            TenantContext.setTenantId(tenant.getId());
            try {
                List<OrdOrder> expired = ordOrderMapper.selectList(
                        new LambdaQueryWrapper<OrdOrder>()
                                .eq(OrdOrder::getStatus, STATUS_PENDING)
                                .lt(OrdOrder::getCreateTime, deadline));
                for (OrdOrder order : expired) {
                    // 逐单事务：条件更新 1→6 成功才回补，避免"已取消未回补"
                    Integer affected = transactionTemplate.execute(txStatus -> {
                        int rows = ordOrderMapper.update(null, new LambdaUpdateWrapper<OrdOrder>()
                                .set(OrdOrder::getStatus, STATUS_CANCELLED)
                                .set(OrdOrder::getCancelReason, "超时未支付自动取消")
                                .eq(OrdOrder::getId, order.getId())
                                .eq(OrdOrder::getStatus, STATUS_PENDING));
                        if (rows == 0) {
                            return 0;
                        }
                        restoreOrderStock(order.getId(), StockBizType.TIMEOUT_RESTORE);
                        refundPointsQuietly(order); // 积分回补（幂等）；券不退还
                        return rows;
                    });
                    if (affected != null && affected > 0) {
                        cancelled++;
                    }
                }
            } catch (Exception e) {
                log.error("超时取消处理失败 tenantId={}", tenant.getId(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return cancelled;
    }

    // ==================== 下单内核（C 端/POS 共用） ====================

    private OrderCreateVO createOrder(Long storeId, List<OrderItemDTO> items, Long memberId,
                                      Integer orderSource, Integer payChannel) {
        List<AssembledItem> assembled = assemble(storeId, items);
        long total = assembled.stream().mapToLong(AssembledItem::amount).sum();

        OrdOrder order = new OrdOrder();
        order.setTenantId(TenantContext.getTenantId());
        order.setStoreId(storeId);
        order.setMemberId(memberId);
        order.setOrderSource(orderSource);
        order.setStatus(STATUS_PENDING);
        order.setTotalAmount(total);
        order.setDiscountAmount(0L);
        order.setPayAmount(total);
        order.setPayChannel(payChannel);
        order.setTakeCode(nextTakeCode(storeId));
        insertWithOrderNoRetry(order);
        persistOrderItems(order, assembled);
        deductStockByAssembled(order, storeId, assembled);

        OrderCreateVO vo = new OrderCreateVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTakeCode(order.getTakeCode());
        vo.setPayAmount(order.getPayAmount());
        return vo;
    }

    /** 明细落库（快照定价） */
    private void persistOrderItems(OrdOrder order, List<AssembledItem> assembled) {
        for (AssembledItem item : assembled) {
            OrdOrderItem oi = new OrdOrderItem();
            oi.setTenantId(order.getTenantId());
            oi.setOrderId(order.getId());
            oi.setProductId(item.productId());
            oi.setListingId(item.listingId());
            oi.setProductName(item.productName());
            oi.setSpecDesc(item.specDesc());
            oi.setPrice(item.price());
            oi.setQty(item.qty());
            oi.setAmount(item.amount());
            oi.setFormulaId(item.formulaId());
            ordOrderItemMapper.insert(oi);
        }
    }

    /** 下单即扣库存（13.1 条件更新；orderNo 幂等；不足整单回滚） */
    private void deductStockByAssembled(OrdOrder order, Long storeId, List<AssembledItem> assembled) {
        Map<Long, Integer> formulaQty = new LinkedHashMap<>();
        for (AssembledItem item : assembled) {
            if (item.formulaId() != null) {
                formulaQty.merge(item.formulaId(), item.qty(), Integer::sum);
            }
        }
        invStockService.deduct(order.getOrderNo(), storeId, toStockItems(aggregateMaterials(formulaQty)));
    }

    /** 积分可抵上限（实付不为负）：按 1 积分 = pointValue 分向下取整 */
    private long capPoints(long payable) {
        long pointValue = memPointsService.pointValue();
        return pointValue <= 0 ? 0 : payable / pointValue;
    }

    /** 用券/积分须会员身份 */
    private void requireMemberForBenefit(Long memberId, String action) {
        if (memberId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, action + "需登录会员");
        }
    }

    /** 取消回补积分（bizType=3，bizNo=orderNo 幂等）；无积分单直接跳过 */
    private void refundPointsQuietly(OrdOrder order) {
        if (order.getPointsUsed() == null || order.getPointsUsed() <= 0
                || order.getMemberId() == null) {
            return;
        }
        memPointsService.refundOfOrder(order.getMemberId(), order.getOrderNo(), order.getPointsUsed());
    }

    /**
     * 明细装配与快照定价：校验上架/发布/规格归属，定价 = 本店售价 + 规格选项加价（全部 Integer/Long 分）
     */
    private List<AssembledItem> assemble(Long storeId, List<OrderItemDTO> items) {
        Map<Long, PrdListing> listingMap = prdListingMapper.selectBatchIds(
                        items.stream().map(OrderItemDTO::getListingId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(PrdListing::getId, Function.identity()));

        List<AssembledItem> result = new ArrayList<>();
        for (OrderItemDTO dto : items) {
            PrdListing listing = listingMap.get(dto.getListingId());
            if (listing == null || !Objects.equals(listing.getStoreId(), storeId) || listing.getStatus() != 1) {
                throw new BizException(ErrorCode.PARAM_ERROR, "商品不在本店上架或已下架");
            }
            PrdProduct product = prdProductMapper.selectById(listing.getProductId());
            if (product == null || product.getPublishStatus() == null || product.getPublishStatus() != 1) {
                throw new BizException(ErrorCode.PRODUCT_NOT_PUBLISHED);
            }
            if (product.getFormulaId() == null) {
                throw new BizException(ErrorCode.FORMULA_NOT_FOUND);
            }

            int extra = 0;
            Set<String> optionNames = new LinkedHashSet<>();
            if (dto.getSpecOptionIds() != null && !dto.getSpecOptionIds().isEmpty()) {
                Set<Long> validSpecIds = prdProductSpecMapper.selectList(
                                new LambdaQueryWrapper<PrdProductSpec>()
                                        .eq(PrdProductSpec::getProductId, product.getId()))
                        .stream().map(PrdProductSpec::getId).collect(Collectors.toSet());
                List<PrdProductSpecOption> options = prdProductSpecOptionMapper.selectBatchIds(dto.getSpecOptionIds());
                Set<Long> usedSpecs = new LinkedHashSet<>();
                for (Long optionId : dto.getSpecOptionIds()) {
                    PrdProductSpecOption option = options.stream()
                            .filter(o -> o.getId().equals(optionId)).findFirst()
                            .orElseThrow(() -> new BizException(ErrorCode.PARAM_ERROR, "规格选项无效"));
                    if (!validSpecIds.contains(option.getSpecId())) {
                        throw new BizException(ErrorCode.PARAM_ERROR, "规格选项不属于该商品");
                    }
                    if (!usedSpecs.add(option.getSpecId())) {
                        throw new BizException(ErrorCode.PARAM_ERROR, "每个规格组只能选择一个选项");
                    }
                    extra += option.getExtraPrice() == null ? 0 : option.getExtraPrice();
                    optionNames.add(option.getOptionName());
                }
            }
            int price = listing.getSalePrice() + extra;
            result.add(new AssembledItem(product.getId(), listing.getId(), product.getProductName(),
                    optionNames.isEmpty() ? null : String.join("/", optionNames),
                    price, dto.getQty(), (long) price * dto.getQty(), product.getFormulaId()));
        }
        return result;
    }

    /** 订单明细 → 配方用量汇总（orderId 持久化明细读取） */
    private Map<Long, Integer> aggregateFormulaQty(Long orderId) {
        List<OrdOrderItem> items = ordOrderItemMapper.selectList(
                new LambdaQueryWrapper<OrdOrderItem>().eq(OrdOrderItem::getOrderId, orderId));
        Map<Long, Integer> formulaQty = new LinkedHashMap<>();
        for (OrdOrderItem item : items) {
            if (item.getFormulaId() != null) {
                formulaQty.merge(item.getFormulaId(), item.getQty(), Integer::sum);
            }
        }
        return formulaQty;
    }

    /** 按配方反算原料汇总：materialId -> (配方单位用量 × 下单数量) 累加 */
    private Map<Long, BigDecimal> aggregateMaterials(Map<Long, Integer> formulaQty) {
        if (formulaQty.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<BomFormulaItem>> byFormula = bomFormulaItemMapper.selectList(
                        new LambdaQueryWrapper<BomFormulaItem>()
                                .in(BomFormulaItem::getFormulaId, formulaQty.keySet()))
                .stream().collect(Collectors.groupingBy(BomFormulaItem::getFormulaId));
        Map<Long, BigDecimal> need = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : formulaQty.entrySet()) {
            for (BomFormulaItem fi : byFormula.getOrDefault(entry.getKey(), List.of())) {
                need.merge(fi.getMaterialId(), fi.getQty().multiply(BigDecimal.valueOf(entry.getValue())),
                        BigDecimal::add);
            }
        }
        return need;
    }

    private List<StockItemDTO> toStockItems(Map<Long, BigDecimal> need) {
        return need.entrySet().stream().map(e -> {
            StockItemDTO item = new StockItemDTO();
            item.setMaterialId(e.getKey());
            item.setQty(e.getValue());
            return item;
        }).toList();
    }

    /** 取餐码（3 位循环序号）：Redis 日序列 teasаas:takecode:{storeId}:{yyyyMMdd} */
    private String nextTakeCode(Long storeId) {
        String key = RedisConstants.TAKE_CODE + storeId + ":" + LocalDate.now().format(NO_DATE);
        try {
            Long seq = stringRedisTemplate.opsForValue().increment(key);
            stringRedisTemplate.expire(key, Duration.ofDays(2));
            return String.valueOf(100 + (int) ((seq - 1) % 900));
        } catch (Exception e) {
            // Redis 不可用时退化为随机码，不阻断下单
            return String.valueOf(100 + ThreadLocalRandom.current().nextInt(900));
        }
    }

    /** 订单号 = "SO" + yyyyMMdd + 6 位随机；uk_order_no 冲突重生成，最多 3 次 */
    private void insertWithOrderNoRetry(OrdOrder order) {
        for (int i = 0; i < 3; i++) {
            order.setOrderNo("SO" + LocalDate.now().format(NO_DATE)
                    + ThreadLocalRandom.current().nextInt(100000, 1000000));
            try {
                ordOrderMapper.insert(order);
                return;
            } catch (DuplicateKeyException e) {
                if (i == 2) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "下单繁忙，请重试");
                }
            }
        }
    }

    // ==================== 状态流转 / 权限工具 ====================

    private void transition(Long id, int from, int to, boolean setFinishTime) {
        OrdOrder order = requireExists(id);
        checkStoreAccess(order.getStoreId());
        LambdaUpdateWrapper<OrdOrder> wrapper = new LambdaUpdateWrapper<OrdOrder>()
                .set(OrdOrder::getStatus, to)
                .eq(OrdOrder::getId, id)
                .eq(OrdOrder::getStatus, from);
        if (setFinishTime) {
            wrapper.set(OrdOrder::getFinishTime, LocalDateTime.now());
        }
        if (ordOrderMapper.update(null, wrapper) == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }
    }

    private OrdOrder requireExists(Long id) {
        OrdOrder order = ordOrderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /** 订单读权限：R2 全租户；R6/R7/R8 名下门店 */
    private void checkOrderReadAccess(OrdOrder order) {
        if (hasRole(ROLE_R2)) {
            return;
        }
        checkStoreAccess(order.getStoreId());
    }

    /** 门店操作权限：R6/R7/R8 且门店在名下（接单/制作/核销共用） */
    private void checkStoreAccess(Long storeId) {
        List<String> roles = SecurityUtils.getRolesFromSession();
        if (!roles.contains(ROLE_R6) && !roles.contains(ROLE_R7) && !roles.contains(ROLE_R8)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅门店角色可操作");
        }
        if (!listBoundStoreIds().contains(storeId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "非名下门店");
        }
    }

    /** POS 收银权限：仅 R7/R8（F-ORD-04） */
    private void requirePosRole() {
        List<String> roles = SecurityUtils.getRolesFromSession();
        if (!roles.contains(ROLE_R7) && !roles.contains(ROLE_R8)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅店长/店员可手工收银");
        }
    }

    /** B 端取消订单权限：R2 全租户；R6/R7 名下门店（R8 无取消权限，PRD 5.3） */
    private void checkCancelAccess(Long storeId) {
        if (hasRole(ROLE_R2)) {
            return;
        }
        List<String> roles = SecurityUtils.getRolesFromSession();
        if (!roles.contains(ROLE_R6) && !roles.contains(ROLE_R7)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无取消订单权限");
        }
        if (!listBoundStoreIds().contains(storeId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "非名下门店");
        }
    }

    private List<Long> listBoundStoreIds() {
        return sysUserStoreMapper.selectList(
                        new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, SecurityUtils.getUserId()))
                .stream().map(SysUserStore::getStoreId).distinct().toList();
    }

    private boolean hasRole(String roleCode) {
        return SecurityUtils.getRolesFromSession().contains(roleCode);
    }

    /** 装配快照明细（价格/金额全为分） */
    private record AssembledItem(Long productId, Long listingId, String productName, String specDesc,
                                 Integer price, Integer qty, Long amount, Long formulaId) {
    }
}
