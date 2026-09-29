package com.teasaas.member.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.member.dto.PhoneBindDTO;
import com.teasaas.member.dto.PointsConfigDTO;
import com.teasaas.member.dto.WxLoginDTO;
import com.teasaas.member.entity.MemMember;
import com.teasaas.member.entity.MemPointsLog;
import com.teasaas.member.entity.MemRechargeRecord;
import com.teasaas.member.vo.MemberAssetsVO;
import com.teasaas.member.vo.MemberVO;
import com.teasaas.member.vo.PointsConfigVO;
import com.teasaas.member.vo.WxLoginVO;

/**
 * 会员服务（F-MEM-01，接口需求 10.1）
 *
 * <p>C 端方法运行在 X-Tenant-Id 手动租户注入上下文（多租户红线声明例外，同 AppOrder）。
 * wx-login 一期 mock：code 即模拟 openid；正式接入（小程序 appid 下发）后替换
 * jscode2session，并升级 C 端会话（当前 X-Member-Id 桥接保留）。</p>
 */
public interface MemMemberService {

    /** 微信登录建档：openid 查有则登录，无则建档（uk_member_openid 兜底并发） */
    WxLoginVO wxLogin(WxLoginDTO dto);

    /** 会员信息（C 端本人，含可用积分/储值余额实时汇总） */
    MemberVO me(Long memberId);

    /** 绑定手机号（C 端本人） */
    void bindPhone(Long memberId, PhoneBindDTO dto);

    /** 会员列表（B 端，R2/R5/R6；keyword 匹配昵称/手机号） */
    PageResult<MemMember> page(int page, int size, String keyword);

    /** 会员资产汇总（B 端 10.1 二期，R2/R5/R6：余额/累计充值消费/可用累计积分） */
    MemberAssetsVO assets(Long memberId);

    /** 会员积分流水（B 端 10.1 二期，R2/R5/R6） */
    PageResult<MemPointsLog> pagePointsLogs(Long memberId, int page, int size);

    /** 会员储值充值记录（B 端 10.1 二期，R2/R5/R6；一期钱包流水=充值记录） */
    PageResult<MemRechargeRecord> pageWalletLogs(Long memberId, int page, int size);

    /** 积分/储值规则配置查询（R2/R5；无记录返回默认值） */
    PointsConfigVO getPointsConfig();

    /** 积分/储值规则配置更新（R2/R5；无记录则插入） */
    void updatePointsConfig(PointsConfigDTO dto);
}
