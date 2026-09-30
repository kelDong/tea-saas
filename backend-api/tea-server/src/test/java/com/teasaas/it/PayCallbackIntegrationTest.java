package com.teasaas.it;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.member.entity.MemMember;
import com.teasaas.member.mapper.MemMemberMapper;
import com.teasaas.member.service.MemPointsService;
import com.teasaas.order.entity.OrdOrder;
import com.teasaas.order.mapper.OrdOrderMapper;
import com.teasaas.payment.dto.PayCallbackDTO;
import com.teasaas.payment.entity.PayCallbackTask;
import com.teasaas.payment.entity.PayPayment;
import com.teasaas.payment.mapper.PayCallbackTaskMapper;
import com.teasaas.payment.mapper.PayPaymentMapper;
import com.teasaas.payment.service.PayPaymentService;
import com.teasaas.payment.vo.PaymentVO;
import com.teasaas.system.entity.Tenant;
import com.teasaas.system.mapper.TenantMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 支付回调端到端集成测试（Review §4-4 资金链路：下单→支付→积分→对账留痕）
 *
 * <p>覆盖：无登录态回调租户上下文注入（R1 修复回归）、回调幂等（重复回调不双记积分）、
 * 超时取消后款到关流水不复活订单（差异留对账）。</p>
 */
class PayCallbackIntegrationTest extends IntegrationTestBase {

    private final PayPaymentService payPaymentService;
    private final PayPaymentMapper payPaymentMapper;
    private final PayCallbackTaskMapper payCallbackTaskMapper;
    private final MemPointsService memPointsService;
    private final TenantMapper tenantMapper;

    PayCallbackIntegrationTest(PayPaymentService payPaymentService, PayPaymentMapper payPaymentMapper,
                               PayCallbackTaskMapper payCallbackTaskMapper, MemPointsService memPointsService,
                               TenantMapper tenantMapper,
                               MemMemberMapper memMemberMapper, OrdOrderMapper ordOrderMapper) {
        super(memMemberMapper, ordOrderMapper);
        this.payPaymentService = payPaymentService;
        this.payPaymentMapper = payPaymentMapper;
        this.payCallbackTaskMapper = payCallbackTaskMapper;
        this.memPointsService = memPointsService;
        this.tenantMapper = tenantMapper;
    }

    /** wxJsapi 建待支付流水（C 端接口无角色校验，走生产创建路径） */
    private PayPayment createPendingPayment(OrdOrder order) {
        PaymentVO vo = payPaymentService.wxJsapi(order.getId());
        return payPaymentMapper.selectOne(new LambdaQueryWrapper<PayPayment>()
                .eq(PayPayment::getPaymentNo, vo.getPaymentNo()));
    }

    private PayCallbackDTO callbackOf(PayPayment payment, String transactionId) {
        PayCallbackDTO dto = new PayCallbackDTO();
        dto.setPaymentNo(payment.getPaymentNo());
        dto.setTransactionId(transactionId);
        dto.setResultCode("SUCCESS");
        dto.setAmount(payment.getAmount());
        return dto;
    }

    /** handleCallback 会 clear 租户上下文（防线程复用串租户），调用后重绑 */
    private String callback(PayCallbackDTO dto) {
        String result = payPaymentService.handleCallback(dto);
        TenantContext.setTenantId(TENANT_ID);
        return result;
    }

    @Test
    void callbackMarksOrderPaidAndEarnsPointsOnce() {
        MemMember member = insertMember();
        OrdOrder order = insertOrder(member.getId(), 1, 1, 5_000L);
        PayPayment payment = createPendingPayment(order);

        assertEquals("SUCCESS", callback(callbackOf(payment, "IT-TX-1")));

        OrdOrder paid = ordOrderMapper.selectById(order.getId());
        assertEquals(2, paid.getStatus(), "订单 1→2");
        assertEquals(1, paid.getPayChannel(), "渠道=微信");
        assertNotNull(paid.getPayTime());
        assertEquals(2, payPaymentMapper.selectById(payment.getId()).getStatus(), "流水 1→2");
        assertEquals(2, taskOf(payment).getStatus(), "回调任务即时处理成功");
        assertEquals(50, memPointsService.availablePoints(member.getId()), "5000分=50积分");

        // 网关重推（微信重试语义）：仍应答 SUCCESS 且不双记
        assertEquals("SUCCESS", callback(callbackOf(payment, "IT-TX-1")));
        assertEquals(2, ordOrderMapper.selectById(order.getId()).getStatus());
        assertEquals(50, memPointsService.availablePoints(member.getId()), "重复回调不双发积分");
        assertEquals(2, payPaymentMapper.selectById(payment.getId()).getStatus(), "流水保持成功态");
    }

    @Test
    void callbackOnCancelledOrderClosesPaymentForManualRefund() {
        MemMember member = insertMember();
        OrdOrder order = insertOrder(member.getId(), 1, 1, 3_000L);
        PayPayment payment = createPendingPayment(order);

        // 模拟超时取消：1→6（绕过服务直接置态，取消服务另有链路）
        ordOrderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrdOrder>()
                .set(OrdOrder::getStatus, 6)
                .set(OrdOrder::getCancelReason, "IT超时取消")
                .eq(OrdOrder::getId, order.getId()));

        assertEquals("SUCCESS", callback(callbackOf(payment, "IT-TX-2")));

        assertEquals(6, ordOrderMapper.selectById(order.getId()).getStatus(), "取消单不得被回调复活");
        PayPayment closed = payPaymentMapper.selectById(payment.getId());
        assertEquals(4, closed.getStatus(), "流水关闭待人工退款");
        assertEquals(0, memPointsService.availablePoints(member.getId()), "取消单不得发积分");
        assertEquals(2, taskOf(payment).getStatus());
    }

    @Test
    void retryPendingTasksSkipsAlreadySucceededTask() {
        seedTenantRow(); // retryPendingTasks 按启用租户迭代，无租户行则扫描空转
        MemMember member = insertMember();
        OrdOrder order = insertOrder(member.getId(), 1, 1, 5_000L);
        PayPayment payment = createPendingPayment(order);
        assertEquals("SUCCESS", callback(callbackOf(payment, "IT-TX-3")));
        TenantContext.setTenantId(TENANT_ID);

        // 已成功任务不被重试调度触碰（状态守卫回归，Review §3.2）
        int processed = payPaymentService.retryPendingTasks();
        TenantContext.setTenantId(TENANT_ID);
        assertEquals(0, processed);
        assertEquals(2, taskOf(payment).getStatus());
    }

    /** 平台表 tenant（租户插件忽略清单），插一次供 retryPendingTasks 迭代 */
    private void seedTenantRow() {
        if (tenantMapper.selectById(TENANT_ID) != null) {
            return;
        }
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setTenantCode("IT-" + TENANT_ID);
        tenant.setTenantName("集成测试租户");
        tenant.setStatus(1);
        tenantMapper.insert(tenant);
    }

    private PayCallbackTask taskOf(PayPayment payment) {
        return payCallbackTaskMapper.selectOne(new LambdaQueryWrapper<PayCallbackTask>()
                .eq(PayCallbackTask::getBizNo, payment.getPaymentNo())
                .orderByDesc(PayCallbackTask::getId)
                .last("LIMIT 1"));
    }
}
