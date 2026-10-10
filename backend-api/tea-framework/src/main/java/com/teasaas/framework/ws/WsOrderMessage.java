package com.teasaas.framework.ws;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单 WS 推送消息（接口需求 8.3，服务端→门店端单向推送）
 *
 * <p>轻量事件通知：客户端收到后按需重查列表/详情，不做全量明细推送。
 * 字段与契约文档 §8.3 消息格式一一对应（雪花 Long 经全局 Jackson 配置
 * 序列化为 string，防 JS 精度丢失）。</p>
 */
@Data
public class WsOrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 新支付单（订单进入已支付：微信回调/储值/POS 收款），门店应提醒接单 */
    public static final String TYPE_NEW_ORDER = "NEW_ORDER";

    /** 状态变更（接单/制作完成/核销/退款等流转） */
    public static final String TYPE_STATUS_CHANGED = "STATUS_CHANGED";

    private String type;
    private Long tenantId;
    private Long storeId;
    private Long orderId;
    private String orderNo;

    /** 变更后订单状态：1待支付/2已支付/3制作中/4待取餐/5已完成/6已取消/7已退款 */
    private Integer status;

    /** 取餐码（新单叫号提示用） */
    private String takeCode;

    /** 事件时间戳(ms) */
    private Long time;

    public static WsOrderMessage of(String type, Long tenantId, Long storeId, Long orderId,
                                    String orderNo, Integer status, String takeCode) {
        WsOrderMessage msg = new WsOrderMessage();
        msg.setType(type);
        msg.setTenantId(tenantId);
        msg.setStoreId(storeId);
        msg.setOrderId(orderId);
        msg.setOrderNo(orderNo);
        msg.setStatus(status);
        msg.setTakeCode(takeCode);
        return msg;
    }
}
