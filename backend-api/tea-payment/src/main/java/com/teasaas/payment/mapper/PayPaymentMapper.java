package com.teasaas.payment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teasaas.payment.entity.PayPayment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 支付流水 Mapper（生成器骨架）
 */
@Mapper
public interface PayPaymentMapper extends BaseMapper<PayPayment> {

    /**
     * 支付网关回调专用查询。
     * 【多租户绕过声明（工程规范 6.1）】/api/notify/** 在 SaToken 白名单内，微信网关回调
     * 无登录态，拦截器不会注入 TenantContext，租户插件对 null 上下文兜底 tenant_id=0——
     * 普通查询会被改写成 tenant_id=0 导致真实流水永远查不到。
     * 此处按全局唯一 payment_no 绕过租户插件反查，拿到流水租户后由调用方显式注入
     * TenantContext 再执行后续处理（对齐 SysUserMapper.selectByUsernameForLogin 惯例，
     * 已过评审；业务查询禁止模仿本方法）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id, tenant_id, payment_no, order_id, channel, amount, status, "
            + "transaction_id, callback_time, create_time "
            + "FROM pay_payment WHERE payment_no = #{paymentNo} LIMIT 1")
    PayPayment selectByPaymentNoForCallback(@Param("paymentNo") String paymentNo);
}
