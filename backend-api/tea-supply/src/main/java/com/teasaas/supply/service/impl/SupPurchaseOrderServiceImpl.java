package com.teasaas.supply.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.franchise.entity.FraFranchisee;
import com.teasaas.franchise.entity.FraStore;
import com.teasaas.franchise.mapper.FraFranchiseeMapper;
import com.teasaas.franchise.mapper.FraStoreMapper;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.inventory.constant.StockBizType;
import com.teasaas.inventory.dto.StockItemDTO;
import com.teasaas.inventory.service.InvStockService;
import com.teasaas.supply.dto.PurchaseOrderCreateDTO;
import com.teasaas.supply.dto.PurchaseOrderItemDTO;
import com.teasaas.supply.entity.SupPurchaseOrder;
import com.teasaas.supply.entity.SupPurchaseOrderItem;
import com.teasaas.supply.entity.SupTierPrice;
import com.teasaas.supply.mapper.SupPurchaseOrderItemMapper;
import com.teasaas.supply.mapper.SupPurchaseOrderMapper;
import com.teasaas.supply.mapper.SupTierPriceMapper;
import com.teasaas.supply.service.SupPurchaseOrderService;
import com.teasaas.supply.vo.PurchaseOrderDetailVO;
import com.teasaas.system.entity.SysUserStore;
import com.teasaas.system.mapper.SysUserStoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 采购单服务实现（F-SUP-03/04，接口需求 4.3）
 *
 * <p>状态机：1待确认 → 2已确认 → 3已发货 → 4已入库；5已取消。
 * 所有状态转移均用条件更新（where status = 期望态）防并发，affected==0 时按当前状态抛
 * PURCHASE_STATUS_NOT_ALLOWED / PURCHASE_NOT_FOUND。</p>
 *
 * <p>数据权限：R4 查全租户；R6（及 R7 收货）按 sys_user_store 名下门店过滤。</p>
 */
@Service
@RequiredArgsConstructor
public class SupPurchaseOrderServiceImpl implements SupPurchaseOrderService {

    /** 状态：1待确认 */
    private static final int STATUS_PENDING = 1;
    /** 状态：2已确认 */
    private static final int STATUS_CONFIRMED = 2;
    /** 状态：3已发货 */
    private static final int STATUS_SHIPPED = 3;
    /** 状态：4已入库 */
    private static final int STATUS_RECEIVED = 4;
    /** 状态：5已取消 */
    private static final int STATUS_CANCELLED = 5;

    private static final String ROLE_R4 = "R4";
    private static final String ROLE_R6 = "R6";
    private static final String ROLE_R7 = "R7";

    private static final DateTimeFormatter PO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SupPurchaseOrderMapper supPurchaseOrderMapper;
    private final SupPurchaseOrderItemMapper supPurchaseOrderItemMapper;
    private final SupTierPriceMapper supTierPriceMapper;
    private final SysUserStoreMapper sysUserStoreMapper;
    private final FraStoreMapper fraStoreMapper;
    private final FraFranchiseeMapper fraFranchiseeMapper;
    private final InvStockService invStockService;

    // ==================== 查询 ====================

    @Override
    public PageResult<SupPurchaseOrder> page(int page, int size, Integer status, Long storeId) {
        LambdaQueryWrapper<SupPurchaseOrder> wrapper = new LambdaQueryWrapper<SupPurchaseOrder>()
                .eq(status != null, SupPurchaseOrder::getStatus, status)
                .eq(storeId != null, SupPurchaseOrder::getStoreId, storeId)
                .orderByDesc(SupPurchaseOrder::getId);
        // 数据权限：R4 查全租户（租户插件兜底）；否则（R6/R7）过滤名下门店
        if (!hasRole(ROLE_R4)) {
            List<Long> storeIds = listBoundStoreIds(SecurityUtils.getUserId());
            if (storeIds.isEmpty()) {
                return PageResult.empty(page, size);
            }
            wrapper.in(SupPurchaseOrder::getStoreId, storeIds);
        }
        Page<SupPurchaseOrder> result = supPurchaseOrderMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PurchaseOrderDetailVO detail(Long id) {
        SupPurchaseOrder order = requireExists(id);
        checkStoreAccess(order.getStoreId());
        List<SupPurchaseOrderItem> items = supPurchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<SupPurchaseOrderItem>()
                        .eq(SupPurchaseOrderItem::getOrderId, id)
                        .orderByAsc(SupPurchaseOrderItem::getId));
        return PurchaseOrderDetailVO.of(order, items);
    }

    // ==================== 发起（仅 R6，P1-1 决策） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PurchaseOrderCreateDTO dto) {
        // ① 角色校验：采购单仅加盟商老板（R6）发起
        if (!hasRole(ROLE_R6)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅加盟商可发起采购单");
        }
        Long userId = SecurityUtils.getUserId();
        Long tenantId = SecurityUtils.getTenantIdFromSession();

        // ② 门店归属校验：storeId 必须在当前用户 sys_user_store 绑定内
        if (!listBoundStoreIds(userId).contains(dto.getStoreId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "非名下门店");
        }

        // ③ 门店 → 加盟商 → 等级：直营店（franchisee_id 为空）由总部采购，R6 不可发起
        FraStore store = fraStoreMapper.selectById(dto.getStoreId());
        if (store == null) {
            throw new BizException(ErrorCode.STORE_NOT_FOUND);
        }
        if (store.getFranchiseeId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "直营门店由总部采购");
        }
        FraFranchisee franchisee = fraFranchiseeMapper.selectById(store.getFranchiseeId());
        if (franchisee == null) {
            throw new BizException(ErrorCode.FRANCHISEE_NOT_FOUND);
        }
        String levelCode = franchisee.getLevelCode();

        // ④ 按等级批量取分级价格并计价快照：unit_price = tier.price；amount = qty × unit_price（分）
        List<Long> materialIds = dto.getItems().stream().map(PurchaseOrderItemDTO::getMaterialId).distinct().toList();
        Map<Long, SupTierPrice> priceMap = supTierPriceMapper.selectList(
                        new LambdaQueryWrapper<SupTierPrice>()
                                .in(SupTierPrice::getMaterialId, materialIds)
                                .eq(SupTierPrice::getLevelCode, levelCode))
                .stream()
                .collect(Collectors.toMap(SupTierPrice::getMaterialId, Function.identity(), (a, b) -> a));

        long totalAmount = 0L;
        SupPurchaseOrder order = new SupPurchaseOrder();
        order.setStoreId(dto.getStoreId());
        order.setFranchiseeId(store.getFranchiseeId());
        order.setLevelCode(levelCode);
        order.setStatus(STATUS_PENDING);
        order.setRemark(dto.getRemark());
        order.setTotalAmount(0L);

        // ⑤/⑥ 单号生成 + 主表落库（uk_purchase_no 冲突重试 3 次）
        insertOrderWithUniqueNo(order);
        for (PurchaseOrderItemDTO item : dto.getItems()) {
            SupTierPrice tier = priceMap.get(item.getMaterialId());
            if (tier == null) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "原料[id=" + item.getMaterialId() + "]未设置等级价格");
            }
            // 金额红线：中间过程 BigDecimal，四舍五入取整回 long（分）
            long amount = item.getQty()
                    .multiply(BigDecimal.valueOf(tier.getPrice()))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
            totalAmount += amount;

            SupPurchaseOrderItem entity = new SupPurchaseOrderItem();
            entity.setTenantId(tenantId);
            entity.setOrderId(order.getId());
            entity.setMaterialId(item.getMaterialId());
            entity.setQty(item.getQty());
            entity.setUnitPrice(tier.getPrice());
            entity.setAmount(amount);
            supPurchaseOrderItemMapper.insert(entity);
        }
        // 回写总额
        order.setTotalAmount(totalAmount);
        supPurchaseOrderMapper.updateById(order);
        return order.getId();
    }

    // ==================== 状态机 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        requireRole(ROLE_R4, "仅总部供应链可确认计价");
        requireExists(id);
        int affected = supPurchaseOrderMapper.update(null, new LambdaUpdateWrapper<SupPurchaseOrder>()
                .set(SupPurchaseOrder::getStatus, STATUS_CONFIRMED)
                .set(SupPurchaseOrder::getConfirmBy, SecurityUtils.getUserId())
                .set(SupPurchaseOrder::getConfirmTime, LocalDateTime.now())
                .eq(SupPurchaseOrder::getId, id)
                .eq(SupPurchaseOrder::getStatus, STATUS_PENDING));
        checkTransition(affected);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id) {
        requireRole(ROLE_R4, "仅总部供应链可发货");
        requireExists(id);
        int affected = supPurchaseOrderMapper.update(null, new LambdaUpdateWrapper<SupPurchaseOrder>()
                .set(SupPurchaseOrder::getStatus, STATUS_SHIPPED)
                .set(SupPurchaseOrder::getShipBy, SecurityUtils.getUserId())
                .set(SupPurchaseOrder::getShipTime, LocalDateTime.now())
                .eq(SupPurchaseOrder::getId, id)
                .eq(SupPurchaseOrder::getStatus, STATUS_CONFIRMED));
        checkTransition(affected);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long id) {
        // R6/R7 均可确认入库（名下门店）
        if (!hasRole(ROLE_R6) && !hasRole(ROLE_R7)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅加盟商或店长可确认入库");
        }
        SupPurchaseOrder order = requireExists(id);
        checkStoreAccess(order.getStoreId());
        int affected = supPurchaseOrderMapper.update(null, new LambdaUpdateWrapper<SupPurchaseOrder>()
                .set(SupPurchaseOrder::getStatus, STATUS_RECEIVED)
                .set(SupPurchaseOrder::getReceiveBy, SecurityUtils.getUserId())
                .set(SupPurchaseOrder::getReceiveTime, LocalDateTime.now())
                .eq(SupPurchaseOrder::getId, id)
                .eq(SupPurchaseOrder::getStatus, STATUS_SHIPPED));
        checkTransition(affected);
        // F-SUP-04 入库联动（数据库设计 13.3）：库存 +qty 并写流水 bizType=4（bizNo 幂等）
        List<SupPurchaseOrderItem> items = supPurchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<SupPurchaseOrderItem>()
                        .eq(SupPurchaseOrderItem::getOrderId, id));
        invStockService.inbound(order.getStoreId(),
                items.stream().map(item -> {
                    StockItemDTO stockItem = new StockItemDTO();
                    stockItem.setMaterialId(item.getMaterialId());
                    stockItem.setQty(item.getQty());
                    return stockItem;
                }).toList(),
                StockBizType.PURCHASE_IN, order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        SupPurchaseOrder order = requireExists(id);
        int affected;
        if (hasRole(ROLE_R4)) {
            // R4：未完成（待确认/已确认/已发货）均可取消
            affected = supPurchaseOrderMapper.update(null, new LambdaUpdateWrapper<SupPurchaseOrder>()
                    .set(SupPurchaseOrder::getStatus, STATUS_CANCELLED)
                    .eq(SupPurchaseOrder::getId, id)
                    .in(SupPurchaseOrder::getStatus,
                            Set.of(STATUS_PENDING, STATUS_CONFIRMED, STATUS_SHIPPED)));
        } else if (hasRole(ROLE_R6)) {
            // R6：仅待确认可取消，且须为名下门店
            checkStoreAccess(order.getStoreId());
            affected = supPurchaseOrderMapper.update(null, new LambdaUpdateWrapper<SupPurchaseOrder>()
                    .set(SupPurchaseOrder::getStatus, STATUS_CANCELLED)
                    .eq(SupPurchaseOrder::getId, id)
                    .eq(SupPurchaseOrder::getStatus, STATUS_PENDING));
        } else {
            throw new BizException(ErrorCode.FORBIDDEN, "无取消采购单权限");
        }
        checkTransition(affected);
    }

    // ==================== 私有工具 ====================

    /**
     * 单号 = "PO" + yyyyMMdd + 6 位随机数字；uk_purchase_no 冲突则重生成，最多重试 3 次
     */
    private void insertOrderWithUniqueNo(SupPurchaseOrder order) {
        for (int i = 0; i < 3; i++) {
            String orderNo = "PO" + LocalDate.now().format(PO_DATE)
                    + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            order.setOrderNo(orderNo);
            try {
                supPurchaseOrderMapper.insert(order);
                return;
            } catch (DuplicateKeyException ignored) {
                // uk_purchase_no 冲突：换号重试
            }
        }
        throw new BizException(ErrorCode.SYSTEM_ERROR, "采购单号生成冲突，请重试");
    }

    private SupPurchaseOrder requireExists(Long id) {
        // 租户插件自动拼 tenant_id 防跨租户访问
        SupPurchaseOrder order = supPurchaseOrderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ErrorCode.PURCHASE_NOT_FOUND);
        }
        return order;
    }

    /**
     * 条件更新结果检查：affected==0 表示状态竞争失败（存在性已前置校验）
     */
    private void checkTransition(int affected) {
        if (affected == 0) {
            throw new BizException(ErrorCode.PURCHASE_STATUS_NOT_ALLOWED);
        }
    }

    /**
     * 门店数据权限：R4 不限制；其他角色要求 storeId 在当前用户 sys_user_store 绑定内
     */
    private void checkStoreAccess(Long storeId) {
        if (hasRole(ROLE_R4)) {
            return;
        }
        if (!listBoundStoreIds(SecurityUtils.getUserId()).contains(storeId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "非名下门店");
        }
    }

    private List<Long> listBoundStoreIds(Long userId) {
        return sysUserStoreMapper.selectList(new LambdaQueryWrapper<SysUserStore>()
                        .eq(SysUserStore::getUserId, userId))
                .stream()
                .map(SysUserStore::getStoreId)
                .distinct()
                .toList();
    }

    private boolean hasRole(String roleCode) {
        return SecurityUtils.getRolesFromSession().contains(roleCode);
    }

    private void requireRole(String roleCode, String message) {
        if (!hasRole(roleCode)) {
            throw new BizException(ErrorCode.FORBIDDEN, message);
        }
    }
}
