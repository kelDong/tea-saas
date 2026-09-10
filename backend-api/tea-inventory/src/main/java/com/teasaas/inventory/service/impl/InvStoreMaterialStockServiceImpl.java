package com.teasaas.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.inventory.constant.StockBizType;
import com.teasaas.inventory.dto.StockAdjustDTO;
import com.teasaas.inventory.entity.InvStoreMaterialStock;
import com.teasaas.inventory.entity.InvStockLog;
import com.teasaas.inventory.mapper.InvStoreMaterialStockMapper;
import com.teasaas.inventory.mapper.InvStockLogMapper;
import com.teasaas.inventory.service.InvStoreMaterialStockService;
import com.teasaas.inventory.vo.InventorySummaryVO;
import com.teasaas.inventory.vo.StockWarningVO;
import com.teasaas.system.entity.SysUserStore;
import com.teasaas.system.mapper.SysUserStoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存台账服务实现（F-INV-01/03，接口需求 7.1/7.2）
 *
 * <p>数据权限：盘点仅 R6/R7 且限名下门店（接口契约"R6/R7 仅"）；
 * 台账/流水 R6/R7 名下门店、R2 全租户读；总览 R2/R5 读。</p>
 */
@Service
@RequiredArgsConstructor
public class InvStoreMaterialStockServiceImpl implements InvStoreMaterialStockService {

    private static final String ROLE_R2 = "R2";

    private final InvStoreMaterialStockMapper stockMapper;
    private final InvStockLogMapper stockLogMapper;
    private final SysUserStoreMapper sysUserStoreMapper;

    // ==================== 查询 ====================

    @Override
    public PageResult<InvStoreMaterialStock> page(int page, int size, Long storeId, Long materialId) {
        LambdaQueryWrapper<InvStoreMaterialStock> wrapper = new LambdaQueryWrapper<InvStoreMaterialStock>()
                .eq(storeId != null, InvStoreMaterialStock::getStoreId, storeId)
                .eq(materialId != null, InvStoreMaterialStock::getMaterialId, materialId)
                .orderByAsc(InvStoreMaterialStock::getStoreId)
                .orderByAsc(InvStoreMaterialStock::getMaterialId);
        // 数据权限：R2 读全租户（租户插件兜底）；其他角色（R6/R7）按名下门店过滤
        if (!hasRole(ROLE_R2)) {
            List<Long> storeIds = listBoundStoreIds();
            if (storeIds.isEmpty()) {
                return PageResult.empty(page, size);
            }
            wrapper.in(InvStoreMaterialStock::getStoreId, storeIds);
        }
        Page<InvStoreMaterialStock> result = stockMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PageResult<InvStockLog> logs(int page, int size, Long storeId, Long materialId) {
        checkStoreAccess(storeId);
        LambdaQueryWrapper<InvStockLog> wrapper = new LambdaQueryWrapper<InvStockLog>()
                .eq(InvStockLog::getStoreId, storeId)
                .eq(materialId != null, InvStockLog::getMaterialId, materialId)
                .orderByDesc(InvStockLog::getId);
        Page<InvStockLog> result = stockLogMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public List<StockWarningVO> warnings(Long storeId) {
        // R2 看全部；R6/R7 名下门店（storeId 必传且须名下）
        if (!hasRole(ROLE_R2)) {
            if (storeId == null) {
                List<Long> storeIds = listBoundStoreIds();
                if (storeIds.isEmpty()) {
                    return List.of();
                }
                // 名下多门店逐一查（MVP 数据量小；量大后改 XML in 查询）
                return storeIds.stream()
                        .flatMap(id -> stockMapper.selectWarnings(id).stream())
                        .toList();
            }
            checkStoreAccess(storeId);
        }
        return stockMapper.selectWarnings(storeId);
    }

    @Override
    public List<InventorySummaryVO> summary() {
        return stockMapper.selectSummary();
    }

    // ==================== 盘点修正（F-INV-01，仅 R6/R7） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(StockAdjustDTO dto) {
        checkStoreAccess(dto.getStoreId());
        Long userId = SecurityUtils.getUserId();

        InvStoreMaterialStock stock = stockMapper.selectOne(new LambdaQueryWrapper<InvStoreMaterialStock>()
                .eq(InvStoreMaterialStock::getStoreId, dto.getStoreId())
                .eq(InvStoreMaterialStock::getMaterialId, dto.getMaterialId()));
        BigDecimal before = stock == null ? BigDecimal.ZERO : stock.getStockQty();
        BigDecimal change = dto.getActualQty().subtract(before);
        if (change.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        if (stock == null) {
            InvStoreMaterialStock insert = new InvStoreMaterialStock();
            insert.setTenantId(SecurityUtils.getTenantIdFromSession());
            insert.setStoreId(dto.getStoreId());
            insert.setMaterialId(dto.getMaterialId());
            insert.setStockQty(dto.getActualQty());
            insert.setVersion(0);
            insert.setUpdateBy(userId);
            insert.setUpdateTime(LocalDateTime.now());
            stockMapper.insert(insert);
        } else {
            // 盘点以实盘为准（最新值覆盖），version+1 留痕
            stockMapper.update(null, new LambdaUpdateWrapper<InvStoreMaterialStock>()
                    .set(InvStoreMaterialStock::getStockQty, dto.getActualQty())
                    .setSql("version = version + 1")
                    .set(InvStoreMaterialStock::getUpdateBy, userId)
                    .set(InvStoreMaterialStock::getUpdateTime, LocalDateTime.now())
                    .eq(InvStoreMaterialStock::getId, stock.getId()));
        }

        InvStockLog log = new InvStockLog();
        log.setTenantId(SecurityUtils.getTenantIdFromSession());
        log.setStoreId(dto.getStoreId());
        log.setMaterialId(dto.getMaterialId());
        log.setChangeQty(change);
        log.setBeforeQty(before);
        log.setAfterQty(dto.getActualQty());
        log.setBizType(StockBizType.ADJUST);
        log.setRemark(dto.getRemark());
        log.setCreateBy(userId);
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);
    }

    // ==================== 私有工具 ====================

    private void checkStoreAccess(Long storeId) {
        if (!listBoundStoreIds().contains(storeId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "非名下门店");
        }
    }

    private List<Long> listBoundStoreIds() {
        return sysUserStoreMapper.selectList(
                        new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, SecurityUtils.getUserId()))
                .stream().map(SysUserStore::getStoreId).distinct().toList();
    }

    private boolean hasRole(String roleCode) {
        return SecurityUtils.getRolesFromSession().contains(roleCode);
    }
}
