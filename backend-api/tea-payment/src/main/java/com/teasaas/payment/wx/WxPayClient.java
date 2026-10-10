package com.teasaas.payment.wx;

import com.teasaas.payment.config.PayProperties;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 微信支付 V3 真实网关客户端（官方 wechatpay-java SDK）
 *
 * <p>仅在 tea.pay.mock=false 时装配（资质 t100011 下发后启用）；
 * 构造即完成商户配置校验与平台证书自动下载，缺配置 fail-fast 启动报错。
 * 调用方一律经 ObjectProvider&lt;WxPayClient&gt; 注入（mock 模式下 bean 不存在）。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tea.pay.mock", havingValue = "false")
public class WxPayClient {

    private static final String PAY_NOTIFY_PATH = "/api/notify/wx-pay/callback";

    private final PayProperties props;
    private final JsapiServiceExtension jsapiService;
    private final NativePayService nativePayService;
    private final RefundService refundService;
    private final NotificationParser notificationParser;

    public WxPayClient(PayProperties props) {
        PayProperties.Wx wx = props.getWx();
        if (isBlank(wx.getAppId()) || isBlank(wx.getMchId()) || isBlank(wx.getMchSerialNo())
                || isBlank(wx.getPrivateKeyPath()) || isBlank(wx.getApiV3Key())) {
            throw new IllegalStateException(
                    "tea.pay.mock=false 但微信商户配置缺失：请补全 tea.pay.wx.* "
                            + "（appId/mchId/mchSerialNo/privateKeyPath/apiV3Key）后重启");
        }
        this.props = props;
        // 平台证书自动下载与轮换（依赖 APIv3 密钥解密证书）
        RSAAutoCertificateConfig config = new RSAAutoCertificateConfig.Builder()
                .merchantId(wx.getMchId())
                .privateKeyFromPath(wx.getPrivateKeyPath())
                .merchantSerialNumber(wx.getMchSerialNo())
                .apiV3Key(wx.getApiV3Key())
                .build();
        this.jsapiService = new JsapiServiceExtension.Builder().config(config).build();
        this.nativePayService = new NativePayService.Builder().config(config).build();
        this.refundService = new RefundService.Builder().config(config).build();
        this.notificationParser = new NotificationParser(config);
        log.info("微信支付 V3 真实网关已启用 mchId={} notifyBase={}", wx.getMchId(), wx.getNotifyUrlBase());
    }

    /**
     * JSAPI 统一下单（小程序拉起支付）。
     * 返回前端 wx.requestPayment 所需六键：appId/timeStamp/nonceStr/package/signType/paySign
     * ——paySign 由商户私钥对前四键签名生成（SDK prepayWithRequestPayment 一步完成）。
     */
    public Map<String, String> jsapiPrepay(String outTradeNo, long amountFen, String description, String openid) {
        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        amount.setTotal((int) amountFen);
        request.setAmount(amount);
        request.setAppid(props.getWx().getAppId());
        request.setMchid(props.getWx().getMchId());
        request.setDescription(description);
        request.setNotifyUrl(payNotifyUrl());
        request.setOutTradeNo(outTradeNo);
        Payer payer = new Payer();
        payer.setOpenid(openid);
        request.setPayer(payer);
        PrepayWithRequestPaymentResponse resp = jsapiService.prepayWithRequestPayment(request);
        return Map.of(
                "appId", resp.getAppId(),
                "timeStamp", resp.getTimeStamp(),
                "nonceStr", resp.getNonceStr(),
                "package", resp.getPackageVal(),
                "signType", resp.getSignType(),
                "paySign", resp.getPaySign());
    }

    /** Native 统一下单（POS 扫码），返回 code_url */
    public String nativePrepay(String outTradeNo, long amountFen, String description) {
        com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest request =
                new com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest();
        com.wechat.pay.java.service.payments.nativepay.model.Amount amount =
                new com.wechat.pay.java.service.payments.nativepay.model.Amount();
        amount.setTotal((int) amountFen);
        request.setAmount(amount);
        request.setAppid(props.getWx().getAppId());
        request.setMchid(props.getWx().getMchId());
        request.setDescription(description);
        request.setNotifyUrl(payNotifyUrl());
        request.setOutTradeNo(outTradeNo);
        PrepayResponse resp = nativePayService.prepay(request);
        return resp.getCodeUrl();
    }

    /**
     * 退款受理（整单退，refund=total=原支付金额）。
     * 受理失败抛异常由调用方整体回滚；受理成功即视为退款完成（一期口径，
     * 微信侧极罕见的退款失败由对账人工兜底）。
     */
    public void refund(String outTradeNo, String outRefundNo, long amountFen, String reason) {
        CreateRequest request = new CreateRequest();
        request.setOutTradeNo(outTradeNo);
        request.setOutRefundNo(outRefundNo);
        request.setReason(reason);
        AmountReq amount = new AmountReq();
        amount.setRefund(amountFen);
        amount.setTotal(amountFen);
        amount.setCurrency("CNY");
        request.setAmount(amount);
        refundService.create(request);
        log.info("微信退款已受理 outTradeNo={} outRefundNo={} amount={}分", outTradeNo, outRefundNo, amountFen);
    }

    /**
     * 支付回调验签 + AES-GCM 解密（微信 V3 协议：Wechatpay-* 四头 + 密文体）。
     * 验签失败/报文异常 SDK 抛异常，交由全局异常处理拒绝该次通知。
     */
    public Transaction parsePayNotify(String body, String serial, String nonce, String signature, String timestamp) {
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(serial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(body)
                .build();
        return notificationParser.parse(requestParam, Transaction.class);
    }

    private String payNotifyUrl() {
        String base = props.getWx().getNotifyUrlBase();
        if (isBlank(base)) {
            throw new IllegalStateException("tea.pay.wx.notify-url-base 未配置（微信回调公网地址）");
        }
        return base.endsWith("/") ? base + PAY_NOTIFY_PATH.substring(1) : base + PAY_NOTIFY_PATH;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
