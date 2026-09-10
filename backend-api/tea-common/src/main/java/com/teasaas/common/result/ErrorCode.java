package com.teasaas.common.result;

import lombok.Getter;

/**
 * 全局错误码（接口需求文档 1.4）
 * 格式：模块(2) + 场景(2) + 序号(2)（文档表述含前导零，实际存储为整数）
 * 模块：00 通用 / 01 系统 / 02 产品 / 03 订单 / 04 支付 / 05 会员 / 06 供应链 / 07 库存 / 08 加盟 / 09 营销 / 10 报表
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),

    /* 00 通用 */
    PARAM_ERROR(1, "参数错误"),
    UNAUTHORIZED(2, "未登录或会话过期"),
    FORBIDDEN(3, "无权限"),
    SYSTEM_ERROR(999999, "系统繁忙，请稍后重试"),

    /* 01 系统 */
    TENANT_NOT_FOUND(10001, "品牌不存在或已停用"),

    /* 02 产品（原料/配方/商品） */
    MATERIAL_NOT_FOUND(20101, "原料不存在"),
    FORMULA_NOT_FOUND(20201, "配方不存在"),
    PRODUCT_NOT_FOUND(20301, "商品不存在"),
    PRODUCT_NOT_PUBLISHED(20302, "商品未发布，不可上架"),
    CATEGORY_NOT_FOUND(20303, "商品分类不存在"),
    LISTING_NOT_FOUND(20401, "上架记录不存在"),
    LISTING_DUPLICATED(20402, "该商品已在本店上架"),
    SALE_PRICE_EXCEED_LIMIT(20403, "本店售价不得高于总部建议售价"),

    /* 03 订单 */
    ORDER_NOT_FOUND(30101, "订单不存在"),
    ORDER_STATUS_NOT_ALLOWED(30102, "订单状态不允许该操作"),

    /* 04 支付 */
    CALLBACK_SIGN_FAIL(40201, "支付回调签名校验失败"),
    REFUND_AMOUNT_EXCEED(40202, "退款金额超限"),

    /* 05 会员 */
    MEMBER_NOT_FOUND(50101, "会员不存在"),
    WALLET_BALANCE_NOT_ENOUGH(50201, "储值余额不足"),

    /* 06 供应链 */
    PURCHASE_NOT_FOUND(60101, "采购单不存在"),
    PURCHASE_STATUS_NOT_ALLOWED(60102, "采购单状态不允许该操作"),

    /* 07 库存 */
    STOCK_NOT_ENOUGH(70101, "库存不足"),
    STOCK_CONCURRENT_CONFLICT(70102, "库存操作并发冲突，请重试"),

    /* 08 加盟 */
    FRANCHISEE_NOT_FOUND(80101, "加盟商不存在"),
    STORE_NOT_FOUND(80201, "门店不存在"),

    /* 09 营销 */
    COUPON_NOT_AVAILABLE(90101, "优惠券不可用");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
