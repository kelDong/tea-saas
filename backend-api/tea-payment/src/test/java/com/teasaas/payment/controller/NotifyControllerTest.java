package com.teasaas.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teasaas.common.exception.BizException;
import com.teasaas.payment.config.PayProperties;
import com.teasaas.payment.dto.PayCallbackDTO;
import com.teasaas.payment.service.PayPaymentService;
import com.teasaas.payment.wx.WxPayClient;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付回调双协议分派单测（P1-3）：
 * mock=true 明文 JSON 直解；mock=false 经 WxPayClient 验签解密后
 * Transaction → PayCallbackDTO 映射（tradeState.name→resultCode，金额天然为分）。
 */
class NotifyControllerTest {

    private final PayPaymentService payPaymentService = mock(PayPaymentService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    @SuppressWarnings("unchecked")
    private final ObjectProvider<WxPayClient> wxPayClientProvider = mock(ObjectProvider.class);
    private final PayProperties payProperties = new PayProperties();

    private NotifyController controller;

    @BeforeEach
    void setUp() {
        controller = new NotifyController(payPaymentService, payProperties, objectMapper, wxPayClientProvider);
    }

    @Test
    void mockMode_plainJsonParsedAndRelayed() {
        payProperties.setMock(true);
        when(payPaymentService.handleCallback(any())).thenReturn("SUCCESS");

        String resp = controller.wxPayCallback(
                "{\"paymentNo\":\"PAY20261010123456\",\"transactionId\":\"4200001234\","
                        + "\"resultCode\":\"SUCCESS\",\"amount\":1600}",
                null, null, null, null);

        assertEquals("SUCCESS", resp);
        ArgumentCaptor<PayCallbackDTO> captor = ArgumentCaptor.forClass(PayCallbackDTO.class);
        verify(payPaymentService).handleCallback(captor.capture());
        PayCallbackDTO dto = captor.getValue();
        assertEquals("PAY20261010123456", dto.getPaymentNo());
        assertEquals("4200001234", dto.getTransactionId());
        assertEquals("SUCCESS", dto.getResultCode());
        assertEquals(Long.valueOf(1600), dto.getAmount());
    }

    @Test
    void mockMode_malformedBodyRejected() {
        payProperties.setMock(true);
        assertThrows(BizException.class,
                () -> controller.wxPayCallback("not-a-json", null, null, null, null));
    }

    @Test
    void v3Mode_transactionMappedToCallbackDto() {
        payProperties.setMock(false);
        WxPayClient client = mock(WxPayClient.class);
        when(wxPayClientProvider.getIfAvailable()).thenReturn(client);
        when(client.parsePayNotify(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(buildTransaction(Transaction.TradeStateEnum.SUCCESS, 1600));
        when(payPaymentService.handleCallback(any())).thenReturn("SUCCESS");

        String resp = controller.wxPayCallback("encrypted", "serial", "nonce", "signature", "1234567890");

        assertEquals("SUCCESS", resp);
        ArgumentCaptor<PayCallbackDTO> captor = ArgumentCaptor.forClass(PayCallbackDTO.class);
        verify(payPaymentService).handleCallback(captor.capture());
        PayCallbackDTO dto = captor.getValue();
        assertEquals("PAY20261010654321", dto.getPaymentNo());
        assertEquals("4200005678", dto.getTransactionId());
        assertEquals("SUCCESS", dto.getResultCode());
        assertEquals(Long.valueOf(1600), dto.getAmount());
    }

    @Test
    void v3Mode_failedTradeStateMappedAsIs() {
        payProperties.setMock(false);
        WxPayClient client = mock(WxPayClient.class);
        when(wxPayClientProvider.getIfAvailable()).thenReturn(client);
        when(client.parsePayNotify(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(buildTransaction(Transaction.TradeStateEnum.PAYERROR, 1600));
        when(payPaymentService.handleCallback(any())).thenReturn("SUCCESS");

        controller.wxPayCallback("encrypted", "serial", "nonce", "signature", "1234567890");

        ArgumentCaptor<PayCallbackDTO> captor = ArgumentCaptor.forClass(PayCallbackDTO.class);
        verify(payPaymentService).handleCallback(captor.capture());
        assertEquals("PAYERROR", captor.getValue().getResultCode());
    }

    @Test
    void v3Mode_clientMissingFailsFast() {
        payProperties.setMock(false);
        when(wxPayClientProvider.getIfAvailable()).thenReturn(null);
        assertThrows(BizException.class,
                () -> controller.wxPayCallback("{}", "serial", "nonce", "signature", "1234567890"));
    }

    private Transaction buildTransaction(Transaction.TradeStateEnum state, int totalFen) {
        Transaction tx = new Transaction();
        tx.setOutTradeNo("PAY20261010654321");
        tx.setTransactionId("4200005678");
        tx.setTradeState(state);
        TransactionAmount amount = new TransactionAmount();
        amount.setTotal(totalFen);
        tx.setAmount(amount);
        return tx;
    }
}
