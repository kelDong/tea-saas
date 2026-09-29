package com.teasaas.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.member.service.MemPointsService;
import com.teasaas.member.service.MemWalletService;
import com.teasaas.order.dto.PosPayDTO;
import com.teasaas.order.entity.OrdOrder;
import com.teasaas.order.mapper.OrdOrderMapper;
import com.teasaas.payment.dto.PayCallbackDTO;
import com.teasaas.payment.entity.PayCallbackTask;
import com.teasaas.payment.entity.PayPayment;
import com.teasaas.payment.mapper.PayCallbackTaskMapper;
import com.teasaas.payment.mapper.PayPaymentMapper;
import com.teasaas.payment.service.PayPaymentService;
import com.teasaas.payment.vo.PaymentVO;
import com.teasaas.payment.vo.PosPayVO;
import com.teasaas.system.entity.SysUserStore;
import com.teasaas.system.entity.Tenant;
import com.teasaas.system.mapper.SysUserStoreMapper;
import com.teasaas.system.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付服务实现（F-PAY-01，接口需求 9.1；一期 mock 网关）
 *
 * <p>回调可靠性（P1-3）：验签(mock 跳过) → 幂等（流水已成功直接应答 SUCCESS）→
 * 落 pay_callback_task → 即时处理；失败由 tea-job 指数退避重试（不引 MQ）。
 * 支付成功 = 流水 1→2 + 订单 1→2（条件更新防并发重复置位）；
 * 超时取消后回调到达 → 流水关闭（4），差异对账兜底。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPaymentServiceImpl implements PayPaymentService {

    private static final String ROLE_R7 = "R7";
    private static final String ROLE_R8 = "R8";

    /** 流水状态：1待支付/2成功/3失败/4已关闭 */
    private static final int PAY_STATUS_PENDING = 1;
    private static final int PAY_STATUS_SUCCESS = 2;
    private static final int PAY_STATUS_CLOSED = 4;

    /** 订单状态：1待支付/2已支付；渠道（订单/流水同编号）：1微信/2储值/3现金 */
    private static final int ORDER_STATUS_PENDING = 1;
    private static final int ORDER_STATUS_PAID = 2;
    private static final int ORDER_STATUS_CANCELLED = 6;
    private static final int CHANNEL_WX = 1;
    private static final int CHANNEL_WALLET = 2;
    private static final int CHANNEL_CASH = 3;

    /** 回调任务状态：1待处理/2成功/3失败终态 */
    private static final int TASK_PENDING = 1;
    private static final int TASK_SUCCESS = 2;
    private static final int TASK_FAILED = 3;

    /** POS 收款渠道（入参编号，接口需求 8.4）：1现金/2微信Native/3储值 */
    private static final int POS_CHANNEL_CASH = 1;
    private static final int POS_CHANNEL_NATIVE = 2;
    private static final int POS_CHANNEL_BALANCE = 3;

    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PayPaymentMapper payPaymentMapper;
    private final PayCallbackTaskMapper payCallbackTaskMapper;
    private final OrdOrderMapper ordOrderMapper;
    private final MemPointsService memPointsService;
    private final MemWalletService memWalletService;
    private final SysUserStoreMapper sysUserStoreMapper;
    private final TenantMapper tenantMapper;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    /** mock 网关开关（微信商户资质 t100011 下发后置 false 接正式 V3） */
    @Value("${tea.pay.mock:true}")
    private boolean mock;

    // ==================== 统一下单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO wxJsapi(Long orderId) {
        OrdOrder order = requireOrder(orderId, 1, ORDER_STATUS_PENDING, "仅待支付小程序单可发起支付");
        PayPayment payment = reuseOrCreate(order, CHANNEL_WX);

        PaymentVO vo = toVO(payment);
        // mock 拉起参数（正式模式替换为微信 JSAPI prepay 签名包）
        vo.setPayParams(Map.of(
                "appId", "mock-appid",
                "timeStamp", String.valueOf(System.currentTimeMillis() / 1000),
                "nonceStr", UUID.randomUUID().toString().replace("-", ""),
                "package", "prepay_id=mock_" + payment.getPaymentNo()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO wxNative(Long orderId) {
        requirePosRole();
        OrdOrder order = requireOrder(orderId, 2, ORDER_STATUS_PENDING, "仅待支付收银单可发起收款");
        checkStoreAccess(order.getStoreId());
        PayPayment payment = reuseOrCreate(order, CHANNEL_WX);

        PaymentVO vo = toVO(payment);
        vo.setCodeUrl(mock ? "weixin://wxpay/mock/" + payment.getPaymentNo() : null);
        return vo;
    }

    // ==================== POS 收款确认（F-ORD-04） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PosPayVO posPay(Long orderId, PosPayDTO dto) {
        requirePosRole();
        OrdOrder order = ordOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        checkStoreAccess(order.getStoreId());
        if (order.getOrderSource() == null || order.getOrderSource() != 2) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非门店收银单");
        }
        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_PENDING) {
            throw new BizException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }

        // 会员绑定（二期）：为空则条件更新绑定（防并发置位）；已绑不同会员则拒绝
        if (dto.getMemberId() != null) {
            if (order.getMemberId() == null) {
                int bound = ordOrderMapper.update(null, new LambdaUpdateWrapper<OrdOrder>()
                        .set(OrdOrder::getMemberId, dto.getMemberId())
                        .eq(OrdOrder::getId, order.getId())
                        .eq(OrdOrder::getStatus, ORDER_STATUS_PENDING));
                if (bound == 0) {
                    throw new BizException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
                }
            } else if (!order.getMemberId().equals(dto.getMemberId())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "订单已绑定其他会员");
            }
        }

        if (dto.getPayChannel() == POS_CHANNEL_BALANCE) {
            // 储值独立通道（P0-4）：镜像 C 端 appCreate 储值分支，不写 pay_payment
            // （对账口径：payments 侧查全部成功流水，储值单落表必出 PAYMENT_NO_ORDER 误报）
            if (dto.getMemberId() == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "储值收款必须选择会员");
            }
            long payAmount = order.getPayAmount() == null ? 0L : order.getPayAmount();
            if (payAmount > 0) {
                memWalletService.payForOrder(dto.getMemberId(), order.getOrderNo(), payAmount);
            }
            int paid = ordOrderMapper.update(null, new LambdaUpdateWrapper<OrdOrder>()
                    .set(OrdOrder::getStatus, ORDER_STATUS_PAID)
                    .set(OrdOrder::getPayTime, LocalDateTime.now())
                    .set(OrdOrder::getPayChannel, CHANNEL_WALLET)
                    .set(OrdOrder::getBalanceAmount, payAmount)
                    .eq(OrdOrder::getId, order.getId())
                    .eq(OrdOrder::getStatus, ORDER_STATUS_PENDING));
            if (paid == 0) {
                throw new BizException(ErrorCode.ORDER_STATUS_NOT_ALLOWED);
            }
            // 储值单不经支付模块，订单侧直接发积分（bizNo=orderNo 幂等）
            memPointsService.earnOnPaid(dto.getMemberId(), order.getOrderNo(), payAmount);

            PosPayVO walletVo = new PosPayVO();
            walletVo.setOrderId(order.getId());
            walletVo.setOrderNo(order.getOrderNo());
            walletVo.setPayAmount(payAmount);
            walletVo.setReceiveAmount(payAmount);
            walletVo.setChangeAmount(0L);
            walletVo.setPaymentNo(order.getOrderNo());
            return walletVo;
        }

        int orderChannel;
        String transactionId;
        long receive;
        if (dto.getPayChannel() == POS_CHANNEL_CASH) {
            if (dto.getReceiveAmount() == null || dto.getReceiveAmount() < order.getPayAmount()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "实收金额不得小于应收");
            }
            orderChannel = CHANNEL_CASH;
            transactionId = null; // 收款成功后生成
            receive = dto.getReceiveAmount();
        } else if (dto.getPayChannel() == POS_CHANNEL_NATIVE) {
            orderChannel = CHANNEL_WX;
            transactionId = null;
            receive = order.getPayAmount();
        } else {
            throw new BizException(ErrorCode.PARAM_ERROR, "收款渠道不合法");
        }

        PayPayment payment = reuseOrCreate(order, orderChannel);
        if (transactionId == null) {
            transactionId = (orderChannel == CHANNEL_CASH ? "CASH" : "MOCKN") + payment.getPaymentNo();
        }
        markPaymentSuccess(payment, transactionId);

        PosPayVO vo = new PosPayVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPayAmount(order.getPayAmount());
        vo.setReceiveAmount(receive);
        vo.setChangeAmount(orderChannel == CHANNEL_CASH ? receive - order.getPayAmount() : 0L);
        vo.setPaymentNo(payment.getPaymentNo());
        return vo;
    }

    // ==================== 回调（P1-3：验签→幂等→落表→处理） ====================

    @Override
    public String handleCallback(PayCallbackDTO dto) {
        // 验签：mock 模式跳过；正式模式校验微信 V3 签名
        if (!mock && (dto.getSign() == null || dto.getSign().isBlank())) {
            throw new BizException(ErrorCode.CALLBACK_SIGN_FAIL);
        }
        // 回调无登录态（白名单）：先绕过租户插件按全局唯一单号反查流水，
        // 再显式注入流水租户——后续即时处理（落表/订单置位/积分发放）全部落在正确租户上下文
        PayPayment payment = payPaymentMapper.selectByPaymentNoForCallback(dto.getPaymentNo());
        if (payment == null) {
            throw new BizException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        TenantContext.setTenantId(payment.getTenantId());
        try {
            return processCallback(dto, payment);
        } finally {
            // 防线程复用串租户（多租户红线；与 TenantCleanupInterceptor 双保险）
            TenantContext.clear();
        }
    }

    /** 回调主体：金额核对 → 落 pay_callback_task → 即时处理（需已注入正确租户上下文） */
    private String processCallback(PayCallbackDTO dto, PayPayment payment) {
        if (!Objects.equals(payment.getAmount(), dto.getAmount())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "回调金额与流水不符");
        }

        PayCallbackTask task = new PayCallbackTask();
        task.setTenantId(payment.getTenantId());
        task.setTaskNo(genNo("CB"));
        task.setBizType(1);
        task.setBizNo(payment.getPaymentNo());
        task.setRetryCount(0);
        task.setMaxRetry(5);
        task.setStatus(TASK_PENDING);
        task.setNextRetryTime(LocalDateTime.now());
        task.setCreateTime(LocalDateTime.now());
        try {
            task.setPayload(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            task.setPayload("{}");
        }
        try {
            payCallbackTaskMapper.insert(task);
        } catch (DuplicateKeyException e) {
            // uk_cbtask_no 撞号重生成一次
            task.setTaskNo(genNo("CB"));
            payCallbackTaskMapper.insert(task);
        }

        try {
            // 显式事务（本类内自调用会绕过 @Transactional 代理）
            transactionTemplate.executeWithoutResult(tx -> processTask(task, payment));
            return "SUCCESS";
        } catch (Exception e) {
            log.warn("回调即时处理失败，转定时重试 paymentNo={}", dto.getPaymentNo(), e);
            scheduleRetry(task, e.getMessage());
            // 处理待重试也向网关应答成功，避免微信侧持续重推（本地表已兜底）
            return "SUCCESS";
        }
    }

    @Override
    public int retryPendingTasks() {
        int processed = 0;
        List<Tenant> tenants = tenantMapper.selectList(
                new LambdaQueryWrapper<Tenant>().eq(Tenant::getStatus, 1));
        for (Tenant tenant : tenants) {
            TenantContext.setTenantId(tenant.getId());
            try {
                List<PayCallbackTask> tasks = payCallbackTaskMapper.selectList(
                        new LambdaQueryWrapper<PayCallbackTask>()
                                .eq(PayCallbackTask::getStatus, TASK_PENDING)
                                .le(PayCallbackTask::getNextRetryTime, LocalDateTime.now()));
                for (PayCallbackTask task : tasks) {
                    try {
                        PayPayment payment = payPaymentMapper.selectOne(new LambdaQueryWrapper<PayPayment>()
                                .eq(PayPayment::getPaymentNo, task.getBizNo()));
                        if (payment == null) {
                            throw new BizException(ErrorCode.PAYMENT_NOT_FOUND);
                        }
                        transactionTemplate.executeWithoutResult(tx -> processTask(task, payment));
                        processed++;
                    } catch (Exception e) {
                        scheduleRetry(task, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("回调重试任务扫描失败 tenantId={}", tenant.getId(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return processed;
    }

    /**
     * 任务处理：成功置 2；幂等（流水已成功直接成功收尾）。
     * 事务由调用方提供（handleCallback/retryPendingTasks 经 TransactionTemplate）——
     * 本类内自调用绕过代理，勿在本方法再加 @Transactional。
     */
    public void processTask(PayCallbackTask task, PayPayment payment) {
        markPaymentSuccess(payment, extractTransactionId(task));
        payCallbackTaskMapper.update(null, new LambdaUpdateWrapper<PayCallbackTask>()
                .set(PayCallbackTask::getStatus, TASK_SUCCESS)
                .eq(PayCallbackTask::getId, task.getId())
                .eq(PayCallbackTask::getStatus, TASK_PENDING));
    }

    /**
     * 支付成功落账（幂等）：订单 1→2（payTime/payChannel）后流水 1→2；
     * 订单已取消（超时）→ 流水关闭 4，由对账/人工处理退款。
     * 事务由调用方提供（posPay 的 @Transactional / processTask 的 TransactionTemplate）——
     * 本类内自调用绕过代理，勿在本方法再加 @Transactional。
     */
    public void markPaymentSuccess(PayPayment payment, String transactionId) {
        PayPayment fresh = payPaymentMapper.selectById(payment.getId());
        if (fresh == null) {
            throw new BizException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        if (fresh.getStatus() != null && fresh.getStatus() == PAY_STATUS_SUCCESS) {
            return; // 幂等：同流水只处理一次
        }
        if (fresh.getStatus() != null && fresh.getStatus() != PAY_STATUS_PENDING) {
            throw new BizException(ErrorCode.PAYMENT_STATUS_NOT_ALLOWED);
        }

        // 先推订单（条件更新防重复置位/防取消单被复活）
        int orderAffected = ordOrderMapper.update(null, new LambdaUpdateWrapper<OrdOrder>()
                .set(OrdOrder::getStatus, ORDER_STATUS_PAID)
                .set(OrdOrder::getPayTime, LocalDateTime.now())
                .set(OrdOrder::getPayChannel, fresh.getChannel())
                .eq(OrdOrder::getId, fresh.getOrderId())
                .eq(OrdOrder::getStatus, ORDER_STATUS_PENDING));
        if (orderAffected == 0) {
            OrdOrder order = ordOrderMapper.selectById(fresh.getOrderId());
            if (order == null) {
                throw new BizException(ErrorCode.ORDER_NOT_FOUND);
            }
            if (order.getStatus() != null && order.getStatus() == ORDER_STATUS_CANCELLED) {
                // 超时取消后款到：关闭流水，差异留对账处理
                payPaymentMapper.update(null, new LambdaUpdateWrapper<PayPayment>()
                        .set(PayPayment::getStatus, PAY_STATUS_CLOSED)
                        .set(PayPayment::getTransactionId, transactionId)
                        .set(PayPayment::getCallbackTime, LocalDateTime.now())
                        .eq(PayPayment::getId, fresh.getId())
                        .eq(PayPayment::getStatus, PAY_STATUS_PENDING));
                log.warn("订单已超时取消，支付流水关闭待人工退款 paymentNo={} orderNo={}",
                        fresh.getPaymentNo(), order.getOrderNo());
                return;
            }
            // 订单已支付（并发回调）：按幂等成功收尾
        }

        payPaymentMapper.update(null, new LambdaUpdateWrapper<PayPayment>()
                .set(PayPayment::getStatus, PAY_STATUS_SUCCESS)
                .set(PayPayment::getTransactionId, transactionId)
                .set(PayPayment::getCallbackTime, LocalDateTime.now())
                .eq(PayPayment::getId, fresh.getId())
                .eq(PayPayment::getStatus, PAY_STATUS_PENDING));

        // 支付成功得积分（F-MEM-02：bizNo=orderNo 幂等；POS 单无会员跳过）
        OrdOrder paidOrder = ordOrderMapper.selectById(fresh.getOrderId());
        if (paidOrder != null && paidOrder.getMemberId() != null) {
            memPointsService.earnOnPaid(paidOrder.getMemberId(), paidOrder.getOrderNo(),
                    paidOrder.getPayAmount() == null ? 0L : paidOrder.getPayAmount());
        }
    }

    // ==================== 查询 ====================

    @Override
    public PaymentVO getByPaymentNo(String paymentNo) {
        PayPayment payment = payPaymentMapper.selectOne(new LambdaQueryWrapper<PayPayment>()
                .eq(PayPayment::getPaymentNo, paymentNo));
        if (payment == null) {
            throw new BizException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        return toVO(payment);
    }

    // ==================== 私有工具 ====================

    /** 复用待支付流水，否则新建（uk_payment_no 撞号重生成） */
    private PayPayment reuseOrCreate(OrdOrder order, int channel) {
        PayPayment pending = payPaymentMapper.selectOne(new LambdaQueryWrapper<PayPayment>()
                .eq(PayPayment::getOrderId, order.getId())
                .eq(PayPayment::getChannel, channel)
                .eq(PayPayment::getStatus, PAY_STATUS_PENDING)
                .last("LIMIT 1"));
        if (pending != null) {
            return pending;
        }
        PayPayment payment = new PayPayment();
        payment.setTenantId(order.getTenantId());
        payment.setPaymentNo(genNo("PAY"));
        payment.setOrderId(order.getId());
        payment.setChannel(channel);
        payment.setAmount(order.getPayAmount());
        payment.setStatus(PAY_STATUS_PENDING);
        payment.setCreateTime(LocalDateTime.now());
        try {
            payPaymentMapper.insert(payment);
        } catch (DuplicateKeyException e) {
            payment.setPaymentNo(genNo("PAY"));
            payPaymentMapper.insert(payment);
        }
        return payment;
    }

    private OrdOrder requireOrder(Long orderId, int expectSource, int expectStatus, String message) {
        OrdOrder order = ordOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getOrderSource() == null || order.getOrderSource() != expectSource
                || order.getStatus() == null || order.getStatus() != expectStatus) {
            throw new BizException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, message);
        }
        return order;
    }

    /** 失败重试调度（P1-3 指数退避）：2^n 分钟后重试，超 max_retry 置终态 */
    private void scheduleRetry(PayCallbackTask task, String error) {
        int retryCount = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
        LocalDateTime next = LocalDateTime.now().plusMinutes(1L << Math.min(retryCount, 10));
        LambdaUpdateWrapper<PayCallbackTask> wrapper = new LambdaUpdateWrapper<PayCallbackTask>()
                .set(PayCallbackTask::getRetryCount, retryCount)
                .set(PayCallbackTask::getNextRetryTime, next)
                .set(PayCallbackTask::getLastError, abbreviate(error))
                .eq(PayCallbackTask::getId, task.getId())
                // 状态守卫：仅待处理任务可调度重试，防止并发下覆盖已成功/已失败终态
                .eq(PayCallbackTask::getStatus, TASK_PENDING);
        if (retryCount >= (task.getMaxRetry() == null ? 5 : task.getMaxRetry())) {
            wrapper.set(PayCallbackTask::getStatus, TASK_FAILED);
        }
        payCallbackTaskMapper.update(null, wrapper);
    }

    private String extractTransactionId(PayCallbackTask task) {
        try {
            PayCallbackDTO dto = objectMapper.readValue(task.getPayload(), PayCallbackDTO.class);
            return dto.getTransactionId();
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "回调报文损坏");
        }
    }

    private PaymentVO toVO(PayPayment payment) {
        PaymentVO vo = new PaymentVO();
        vo.setPaymentNo(payment.getPaymentNo());
        vo.setOrderId(payment.getOrderId());
        vo.setChannel(payment.getChannel());
        vo.setAmount(payment.getAmount());
        vo.setStatus(payment.getStatus());
        vo.setTransactionId(payment.getTransactionId());
        vo.setCallbackTime(payment.getCallbackTime());
        vo.setCreateTime(payment.getCreateTime());
        return vo;
    }

    /** 单号 = 前缀 + yyyyMMdd + 6 位随机 */
    private String genNo(String prefix) {
        return prefix + LocalDate.now().format(NO_DATE)
                + ThreadLocalRandom.current().nextInt(100000, 1000000);
    }

    private String abbreviate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 250 ? message.substring(0, 250) : message;
    }

    private void requirePosRole() {
        List<String> roles = SecurityUtils.getRolesFromSession();
        if (!roles.contains(ROLE_R7) && !roles.contains(ROLE_R8)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅店长/店员可操作");
        }
    }

    private void checkStoreAccess(Long storeId) {
        if (!listBoundStoreIds().contains(storeId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "非名下门店");
        }
    }

    private List<Long> listBoundStoreIds() {
        return sysUserStoreMapper.selectList(
                        new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, SecurityUtils.getUserId()))
                .stream().map(SysUserStore::getStoreId).distinct().toList();
    }
}
