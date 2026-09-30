package com.teasaas.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.member.entity.MemMember;
import com.teasaas.member.entity.MemMemberConfig;
import com.teasaas.member.entity.MemPointsLog;
import com.teasaas.member.mapper.MemMemberConfigMapper;
import com.teasaas.member.mapper.MemMemberMapper;
import com.teasaas.member.mapper.MemPointsLogMapper;
import com.teasaas.member.service.MemPointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 积分服务实现（F-MEM-02，接口需求 10.2）
 *
 * <p>可用积分 = SUM(mem_points_log.change_points) 实时汇总（不落冗余余额字段）；
 * mem_member.total_points 为累计获得（会员等级自动化二期消费），SQL 自增防读改写竞态。
 * 全部联动方法 bizNo(orderNo)+bizType 查流水幂等（红线 3）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemPointsServiceImpl implements MemPointsService {

    /** 积分流水 biz_type：1消费得/2抵现扣/3赠送(含取消退款回补)/4过期扣 */
    private static final int BIZ_EARN = 1;
    private static final int BIZ_USE = 2;
    private static final int BIZ_REFUND = 3;

    private static final int DEFAULT_POINTS_PER_YUAN = 1;
    private static final int DEFAULT_POINT_VALUE = 1;

    private final MemPointsLogMapper memPointsLogMapper;
    private final MemMemberMapper memMemberMapper;
    private final MemMemberConfigMapper memMemberConfigMapper;

    @Override
    public int availablePoints(Long memberId) {
        return sumPoints(memberId);
    }

    @Override
    public PageResult<MemPointsLog> pageLogs(Long memberId, int page, int size) {
        Page<MemPointsLog> result = memPointsLogMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<MemPointsLog>()
                        .eq(MemPointsLog::getMemberId, memberId)
                        .orderByDesc(MemPointsLog::getId));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public int pointValue() {
        MemMemberConfig config = memMemberConfigMapper.selectOne(new LambdaQueryWrapper<>());
        return config == null || config.getPointValue() == null ? DEFAULT_POINT_VALUE : config.getPointValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useForOrder(Long memberId, String orderNo, int points) {
        if (points <= 0) {
            return;
        }
        // 悲观锁串行化（Review §3.3 方案 A，时序修正）：锁必须先于本事务任何一致性读——
        // RR 隔离下读视图在首条普通 SELECT 建立，若 alreadyLogged 先执行，锁等待后
        // sumPoints 仍沿用旧快照读不到并发方已提交的扣减，双单可双双通过把 SUM 刷负（集成测试暴露）
        memMemberMapper.lockById(memberId);
        if (alreadyLogged(orderNo, BIZ_USE)) {
            return; // 幂等
        }
        int before = sumPoints(memberId);
        if (before < points) {
            throw new BizException(ErrorCode.POINTS_NOT_ENOUGH);
        }
        writeLog(memberId, -points, before, BIZ_USE, orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void earnOnPaid(Long memberId, String orderNo, long payAmount) {
        if (alreadyLogged(orderNo, BIZ_EARN)) {
            return; // 幂等
        }
        int earned = (int) (payAmount / 100 * pointsPerYuan());
        if (earned <= 0) {
            return;
        }
        int before = sumPoints(memberId);
        writeLog(memberId, earned, before, BIZ_EARN, orderNo);
        accumulateTotal(memberId, earned);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOfOrder(Long memberId, String orderNo, int points) {
        if (points <= 0 || alreadyLogged(orderNo, BIZ_REFUND)) {
            return; // 幂等
        }
        int before = sumPoints(memberId);
        writeLog(memberId, points, before, BIZ_REFUND, orderNo);
        accumulateTotal(memberId, points);
    }

    // ==================== 私有工具 ====================

    private boolean alreadyLogged(String orderNo, int bizType) {
        return memPointsLogMapper.selectCount(new LambdaQueryWrapper<MemPointsLog>()
                .eq(MemPointsLog::getBizNo, orderNo)
                .eq(MemPointsLog::getBizType, bizType)) > 0;
    }

    private void writeLog(Long memberId, int change, int before, int bizType, String orderNo) {
        MemPointsLog entry = new MemPointsLog();
        entry.setTenantId(TenantContext.getTenantId());
        entry.setMemberId(memberId);
        entry.setChangePoints(change);
        entry.setBeforePoints(before);
        entry.setAfterPoints(before + change);
        entry.setBizType(bizType);
        entry.setBizNo(orderNo);
        entry.setCreateTime(LocalDateTime.now());
        try {
            memPointsLogMapper.insert(entry);
        } catch (DuplicateKeyException e) {
            // uk_points_log_biz 冲突=并发重放同单同类型（Review §3.1）：抛异常回滚本轮，防双记
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "积分变动重复提交（bizNo=" + orderNo + ", bizType=" + bizType + "），已回滚");
        }
    }

    /** 可用积分 = SUM(change_points)（走 idx_points_log 索引；聚合列名用 QueryWrapper 字符串式） */
    private int sumPoints(Long memberId) {
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

    /** 累计获得 SQL 自增（total_points = total_points + N，防读改写竞态；{0} 占位对齐 InvStock 风格） */
    private void accumulateTotal(Long memberId, int earned) {
        memMemberMapper.update(null, new LambdaUpdateWrapper<MemMember>()
                .setSql("total_points = IFNULL(total_points, 0) + {0}", earned)
                .eq(MemMember::getId, memberId));
    }

    private int pointsPerYuan() {
        MemMemberConfig config = memMemberConfigMapper.selectOne(new LambdaQueryWrapper<>());
        return config == null || config.getPointsPerYuan() == null
                ? DEFAULT_POINTS_PER_YUAN : config.getPointsPerYuan();
    }
}
