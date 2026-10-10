package com.teasaas.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付配置（application.yml tea.pay 前缀）
 *
 * <p>mock=true（默认，资质 t100011 未下发期间）：mock 网关，WxPayClient 不加载；
 * mock=false：启用微信支付 V3 真实网关，tea.pay.wx.* 五项商户配置必填
 * （WxPayClient 构造 fail-fast，缺一启动即报错）。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "tea.pay")
public class PayProperties {

    /** mock 网关开关（微信商户资质下发后置 false 接正式 V3） */
    private boolean mock = true;

    private final Wx wx = new Wx();

    @Data
    public static class Wx {

        /** 小程序 appId */
        private String appId;

        /** 商户号 mchid */
        private String mchId;

        /** 商户 API 证书序列号 */
        private String mchSerialNo;

        /** 商户 API 私钥文件路径（apiclient_key.pem） */
        private String privateKeyPath;

        /** APIv3 密钥（平台证书解密用，微信商户平台设置） */
        private String apiV3Key;

        /** 回调公网基础地址（如 https://api.example.com），支付回调拼 /api/notify/wx-pay/callback */
        private String notifyUrlBase;
    }
}
