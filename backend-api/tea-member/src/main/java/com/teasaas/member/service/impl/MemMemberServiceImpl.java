package com.teasaas.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.member.dto.PhoneBindDTO;
import com.teasaas.member.dto.PointsConfigDTO;
import com.teasaas.member.dto.WxLoginDTO;
import com.teasaas.member.entity.MemMember;
import com.teasaas.member.entity.MemMemberConfig;
import com.teasaas.member.entity.MemMemberLevel;
import com.teasaas.member.entity.MemPointsLog;
import com.teasaas.member.entity.MemRechargeRecord;
import com.teasaas.member.entity.MemWallet;
import com.teasaas.member.mapper.MemMemberConfigMapper;
import com.teasaas.member.mapper.MemMemberLevelMapper;
import com.teasaas.member.mapper.MemMemberMapper;
import com.teasaas.member.mapper.MemPointsLogMapper;
import com.teasaas.member.mapper.MemRechargeRecordMapper;
import com.teasaas.member.mapper.MemWalletMapper;
import com.teasaas.member.service.MemMemberService;
import com.teasaas.member.vo.MemberAssetsVO;
import com.teasaas.member.vo.MemberVO;
import com.teasaas.member.vo.PointsConfigVO;
import com.teasaas.member.vo.PosMemberVO;
import com.teasaas.member.vo.WxLoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 会员服务实现（F-MEM-01，接口需求 10.1）
 *
 * <p>C 端方法运行在 X-Tenant-Id 手动租户注入上下文；wx-login 一期 mock
 * （code 即 openid），正式 jscode2session 待小程序 appid 下发。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemMemberServiceImpl implements MemMemberService {

    private static final String ROLE_R2 = "R2";
    private static final String ROLE_R5 = "R5";
    private static final String ROLE_R6 = "R6";
    private static final String ROLE_R7 = "R7";
    private static final String ROLE_R8 = "R8";

    /** 规则默认值（无配置记录时） */
    private static final int DEFAULT_POINTS_PER_YUAN = 1;
    private static final int DEFAULT_POINT_VALUE = 1;
    private static final int DEFAULT_RECHARGE_BONUS = 0;

    private final MemMemberMapper memMemberMapper;
    private final MemMemberLevelMapper memMemberLevelMapper;
    private final MemMemberConfigMapper memMemberConfigMapper;
    private final MemPointsLogMapper memPointsLogMapper;
    private final MemRechargeRecordMapper memRechargeRecordMapper;
    private final MemWalletMapper memWalletMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WxLoginVO wxLogin(WxLoginDTO dto) {
        // 一期 mock：code 即模拟 openid（正式接入替换为 jscode2session）
        String openid = dto.getCode();
        MemMember member = memMemberMapper.selectOne(new LambdaQueryWrapper<MemMember>()
                .eq(MemMember::getOpenid, openid));
        boolean isNew = false;
        if (member == null) {
            member = new MemMember();
            member.setTenantId(TenantContext.getTenantId());
            member.setOpenid(openid);
            member.setNickname("茶友" + ThreadLocalRandom.current().nextInt(1000, 10000));
            member.setTotalPoints(0);
            member.setStatus(1);
            member.setCreateTime(LocalDateTime.now());
            member.setUpdateTime(LocalDateTime.now());
            try {
                memMemberMapper.insert(member);
            } catch (DuplicateKeyException e) {
                // uk_member_openid 并发建档兜底：重查
                member = memMemberMapper.selectOne(new LambdaQueryWrapper<MemMember>()
                        .eq(MemMember::getOpenid, openid));
                if (member == null) {
                    throw new BizException(ErrorCode.SYSTEM_ERROR, "登录并发异常，请重试");
                }
                return toLoginVO(member, false);
            }
            isNew = true;
            log.info("新会员建档 memberId={} tenantId={}", member.getId(), member.getTenantId());
        }
        return toLoginVO(member, isNew);
    }

    @Override
    public MemberVO me(Long memberId) {
        MemMember member = requireMember(memberId);
        MemberVO vo = new MemberVO();
        vo.setId(member.getId());
        vo.setNickname(member.getNickname());
        vo.setAvatar(member.getAvatar());
        vo.setPhone(member.getPhone());
        if (member.getLevelId() != null) {
            MemMemberLevel level = memMemberLevelMapper.selectById(member.getLevelId());
            vo.setLevelName(level == null ? null : level.getLevelName());
        }
        vo.setTotalPoints(member.getTotalPoints() == null ? 0 : member.getTotalPoints());
        vo.setAvailablePoints(sumAvailablePoints(memberId));
        MemWallet wallet = memWalletMapper.selectOne(new LambdaQueryWrapper<MemWallet>()
                .eq(MemWallet::getMemberId, memberId));
        vo.setWalletBalance(wallet == null || wallet.getBalance() == null ? 0L : wallet.getBalance());
        vo.setCreateTime(member.getCreateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindPhone(Long memberId, PhoneBindDTO dto) {
        MemMember member = requireMember(memberId);
        member.setPhone(dto.getPhone());
        member.setUpdateTime(LocalDateTime.now());
        memMemberMapper.update(member, new LambdaQueryWrapper<MemMember>()
                .eq(MemMember::getId, member.getId()));
    }

    @Override
    public PageResult<MemMember> page(int page, int size, String keyword) {
        requireManageRole();
        LambdaQueryWrapper<MemMember> wrapper = new LambdaQueryWrapper<MemMember>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(MemMember::getNickname, keyword)
                        .or()
                        .like(MemMember::getPhone, keyword))
                .orderByDesc(MemMember::getId);
        Page<MemMember> result = memMemberMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PageResult<PosMemberVO> searchForPos(String keyword, int page, int size) {
        requirePosRole();
        LambdaQueryWrapper<MemMember> wrapper = new LambdaQueryWrapper<MemMember>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(MemMember::getNickname, keyword)
                        .or()
                        .like(MemMember::getPhone, keyword))
                .orderByDesc(MemMember::getId);
        Page<MemMember> result = memMemberMapper.selectPage(Page.of(page, size), wrapper);
        List<PosMemberVO> records = result.getRecords().stream().map(m -> {
            PosMemberVO vo = new PosMemberVO();
            vo.setId(m.getId());
            vo.setNickname(m.getNickname());
            vo.setPhone(m.getPhone());
            return vo;
        }).toList();
        // 页内批量组储值余额（无钱包行=0），供收银员预判可扣
        if (!records.isEmpty()) {
            Map<Long, Long> balanceById = memWalletMapper.selectList(
                            new LambdaQueryWrapper<MemWallet>()
                                    .in(MemWallet::getMemberId,
                                            records.stream().map(PosMemberVO::getId).toList()))
                    .stream().collect(Collectors.toMap(MemWallet::getMemberId,
                            w -> w.getBalance() == null ? 0L : w.getBalance(), (a, b) -> a));
            records.forEach(vo -> vo.setBalance(balanceById.getOrDefault(vo.getId(), 0L)));
        }
        return PageResult.of(records, result.getTotal(), page, size);
    }

    @Override
    public MemberAssetsVO assets(Long memberId) {
        requireManageRole();
        MemMember member = requireMember(memberId);
        MemberAssetsVO vo = new MemberAssetsVO();
        MemWallet wallet = memWalletMapper.selectOne(new LambdaQueryWrapper<MemWallet>()
                .eq(MemWallet::getMemberId, memberId));
        vo.setBalance(wallet == null || wallet.getBalance() == null ? 0L : wallet.getBalance());
        vo.setTotalRecharge(wallet == null || wallet.getTotalRecharge() == null ? 0L : wallet.getTotalRecharge());
        vo.setTotalConsume(wallet == null || wallet.getTotalConsume() == null ? 0L : wallet.getTotalConsume());
        vo.setAvailablePoints(sumAvailablePoints(memberId));
        vo.setTotalPoints(member.getTotalPoints() == null ? 0 : member.getTotalPoints());
        return vo;
    }

    @Override
    public PageResult<MemPointsLog> pagePointsLogs(Long memberId, int page, int size) {
        requireManageRole();
        requireMember(memberId);
        Page<MemPointsLog> result = memPointsLogMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<MemPointsLog>()
                        .eq(MemPointsLog::getMemberId, memberId)
                        .orderByDesc(MemPointsLog::getId));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PageResult<MemRechargeRecord> pageWalletLogs(Long memberId, int page, int size) {
        requireManageRole();
        requireMember(memberId);
        Page<MemRechargeRecord> result = memRechargeRecordMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<MemRechargeRecord>()
                        .eq(MemRechargeRecord::getMemberId, memberId)
                        .orderByDesc(MemRechargeRecord::getId));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PointsConfigVO getPointsConfig() {
        requireConfigRole();
        return toConfigVO(loadConfig());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePointsConfig(PointsConfigDTO dto) {
        requireConfigRole();
        MemMemberConfig config = loadConfig();
        if (config.getId() == null) {
            config = new MemMemberConfig();
            config.setTenantId(TenantContext.getTenantId());
            config.setPointsPerYuan(dto.getPointsPerYuan());
            config.setPointValue(dto.getPointValue());
            config.setRechargeBonusPermille(dto.getRechargeBonusPermille());
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            try {
                memMemberConfigMapper.insert(config);
            } catch (DuplicateKeyException e) {
                // uk_member_config 并发兜底：转更新
                MemMemberConfig exists = loadConfig();
                updateConfigFields(exists, dto);
            }
            return;
        }
        updateConfigFields(config, dto);
    }

    // ==================== 私有工具 ====================

    private MemMember requireMember(Long memberId) {
        MemMember member = memMemberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }

    /** 无配置记录时返回填默认值的未落库对象（id=null 标识插入分支） */
    private MemMemberConfig loadConfig() {
        MemMemberConfig config = memMemberConfigMapper.selectOne(new LambdaQueryWrapper<>());
        if (config == null) {
            config = new MemMemberConfig();
            config.setPointsPerYuan(DEFAULT_POINTS_PER_YUAN);
            config.setPointValue(DEFAULT_POINT_VALUE);
            config.setRechargeBonusPermille(DEFAULT_RECHARGE_BONUS);
        }
        return config;
    }

    private void updateConfigFields(MemMemberConfig config, PointsConfigDTO dto) {
        config.setPointsPerYuan(dto.getPointsPerYuan());
        config.setPointValue(dto.getPointValue());
        config.setRechargeBonusPermille(dto.getRechargeBonusPermille());
        config.setUpdateTime(LocalDateTime.now());
        memMemberConfigMapper.updateById(config);
    }

    private PointsConfigVO toConfigVO(MemMemberConfig config) {
        PointsConfigVO vo = new PointsConfigVO();
        vo.setPointsPerYuan(config.getPointsPerYuan());
        vo.setPointValue(config.getPointValue());
        vo.setRechargeBonusPermille(config.getRechargeBonusPermille());
        return vo;
    }

    /** 可用积分 = SUM(change_points)，跨服务共用口径（MemPointsServiceImpl 同源） */
    private int sumAvailablePoints(Long memberId) {
        List<Map<String, Object>> maps = memPointsLogMapper.selectMaps(
                new QueryWrapper<MemPointsLog>()
                        .select("IFNULL(SUM(change_points), 0) AS total")
                        .eq("member_id", memberId));
        if (maps == null || maps.isEmpty() || maps.get(0) == null) {
            return 0;
        }
        Object total = maps.get(0).get("total");
        return total == null ? 0 : ((Number) total).intValue();
    }

    private WxLoginVO toLoginVO(MemMember member, boolean isNew) {
        WxLoginVO vo = new WxLoginVO();
        vo.setMemberId(member.getId());
        vo.setIsNew(isNew);
        vo.setNickname(member.getNickname());
        vo.setAvatar(member.getAvatar());
        return vo;
    }

    /** 会员列表（10.1）：R2/R5 全量、R6 名下 */
    private void requireManageRole() {
        List<String> roles = SecurityUtils.getRolesFromSession();
        if (!roles.contains(ROLE_R2) && !roles.contains(ROLE_R5) && !roles.contains(ROLE_R6)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部/运营/加盟商可操作");
        }
    }

    /** POS 会员搜索（8.4 储值收款）：R7/R8，对齐 posPay 口径 */
    private void requirePosRole() {
        List<String> roles = SecurityUtils.getRolesFromSession();
        if (!roles.contains(ROLE_R7) && !roles.contains(ROLE_R8)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅店长/店员可操作");
        }
    }

    /** 积分/储值规则（10.2/10.3）：R2/R5 */
    private void requireConfigRole() {
        List<String> roles = SecurityUtils.getRolesFromSession();
        if (!roles.contains(ROLE_R2) && !roles.contains(ROLE_R5)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部/运营可配置规则");
        }
    }
}
