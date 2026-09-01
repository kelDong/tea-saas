package com.teasaas.common.constant;

/**
 * Redis Key 常量（工程规范红线 2：所有 Key 必须以 teasaa 前缀 `teasaas:` 开头）
 */
public final class RedisConstants {

    private RedisConstants() {
    }

    /** 统一前缀 */
    public static final String PREFIX = "teasaas:";

    /** 登录验证码：teasaas:captcha:{key} */
    public static final String CAPTCHA = PREFIX + "captcha:";

    /** 门店原料库存预扣：teasaas:stock:{storeId}:{materialId} */
    public static final String STOCK_PRE_DEDUCT = PREFIX + "stock:";

    /** 支付回调幂等：teasaas:pay:callback:{transactionId} */
    public static final String PAY_CALLBACK = PREFIX + "pay:callback:";

    /** 接口限流：teasaas:rate:{uri}:{ip} */
    public static final String RATE_LIMIT = PREFIX + "rate:";

    /** 取餐码日序列：teasaas:takecode:{storeId}:{yyyyMMdd} */
    public static final String TAKE_CODE = PREFIX + "takecode:";
}
