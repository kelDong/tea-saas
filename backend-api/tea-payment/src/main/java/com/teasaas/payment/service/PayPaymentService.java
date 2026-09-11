package com.teasaas.payment.service;

import com.teasaas.order.dto.PosPayDTO;
import com.teasaas.payment.dto.PayCallbackDTO;
import com.teasaas.payment.vo.PaymentVO;
import com.teasaas.payment.vo.PosPayVO;

/**
 * 支付服务（F-PAY-01，接口需求 9.1；一期 mock 网关，tea.pay.mock=true）
 *
 * <p>回调链路（P1-3 决策）：验签 → 幂等（同流水只处理一次）→ 落
 * pay_callback_task → 处理（支付成功 = 流水 1→2 + 订单 1→2）；
 * 失败任务由 tea-job 定时重试（指数退避，不引 MQ）。</p>
 */
public interface PayPaymentService {

    /** C 端微信 JSAPI 统一下单（订单须为待支付小程序单）；复用未支付流水 */
    PaymentVO wxJsapi(Long orderId);

    /** B 端微信 Native 收款码（POS 单，R7/R8 名下门店） */
    PaymentVO wxNative(Long orderId);

    /** POS 收款确认（1现金含找零/2微信Native/3储值未开放）：即时记账，订单 1→2 */
    PosPayVO posPay(Long orderId, PosPayDTO dto);

    /** 支付回调（mock 验签）：幂等落表 + 即时处理，返回应答码 SUCCESS/FAIL */
    String handleCallback(PayCallbackDTO dto);

    /** 流水查询（相关角色） */
    PaymentVO getByPaymentNo(String paymentNo);

    /**
     * 重试待处理回调任务（tea-job 定时触发，遍历租户）：
     * 成功 → status=2；失败 → retry_count+1 指数退避，超 max_retry 置 3 终态。返回处理数。
     */
    int retryPendingTasks();
}
