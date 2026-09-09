package com.teasaas.franchise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.franchise.dto.StoreBusinessStatusReq;
import com.teasaas.franchise.dto.StoreCreateReq;
import com.teasaas.franchise.dto.StoreUpdateReq;
import com.teasaas.franchise.entity.FraStore;
import com.teasaas.franchise.mapper.FraFranchiseeMapper;
import com.teasaas.franchise.mapper.FraStoreMapper;
import com.teasaas.franchise.service.FraStoreService;
import com.teasaas.franchise.vo.StoreNearbyVO;
import com.teasaas.system.entity.SysUserStore;
import com.teasaas.system.mapper.SysUserStoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * 门店服务实现（F-FRA-02）
 * 数据权限（服务层按会话角色判断）：R6/R7 仅名下门店（sys_user_store 绑定）；R2/R5 本租户全量。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraStoreServiceImpl implements FraStoreService {

    /** 加盟商角色编码 */
    private static final String ROLE_FRANCHISEE = "R6";
    /** 店长角色编码 */
    private static final String ROLE_STORE_MANAGER = "R7";

    /** 附近门店最多返回条数 */
    private static final int NEARBY_LIMIT = 20;

    /** 地球半径（米，Haversine 公式） */
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private final FraStoreMapper fraStoreMapper;
    private final FraFranchiseeMapper fraFranchiseeMapper;
    private final SysUserStoreMapper sysUserStoreMapper;

    @Override
    public PageResult<FraStore> page(int page, int size, String keyword) {
        LambdaQueryWrapper<FraStore> wrapper = new LambdaQueryWrapper<FraStore>()
                .like(keyword != null && !keyword.isBlank(), FraStore::getStoreName, keyword)
                .orderByDesc(FraStore::getId);

        // 数据权限：R6/R7 只看 sys_user_store 绑定的名下门店；其余（R2/R5 等总部角色）看全部
        List<Long> scopeStoreIds = currentScopeStoreIds();
        if (scopeStoreIds != null) {
            if (scopeStoreIds.isEmpty()) {
                return PageResult.empty(page, size);
            }
            wrapper.in(FraStore::getId, scopeStoreIds);
        }

        Page<FraStore> result = fraStoreMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public FraStore getById(Long id) {
        // R6/R7 操作绑定外门店直接拒绝
        checkStorePermission(id);
        FraStore entity = fraStoreMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.STORE_NOT_FOUND);
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StoreCreateReq req) {
        checkStoreCodeDuplicate(req.getStoreCode(), null);
        validateFranchisee(req.getFranchiseeId());
        FraStore entity = new FraStore();
        copyCreateFields(entity, req);
        entity.setBusinessStatus(1);
        entity.setStatus(1);
        entity.setTenantId(SecurityUtils.getTenantIdFromSession());
        fraStoreMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, StoreUpdateReq req) {
        requireExists(id);
        checkStoreCodeDuplicate(req.getStoreCode(), id);
        validateFranchisee(req.getFranchiseeId());
        FraStore entity = new FraStore();
        entity.setId(id);
        entity.setStoreCode(req.getStoreCode());
        entity.setStoreName(req.getStoreName());
        entity.setFranchiseeId(req.getFranchiseeId());
        entity.setAddress(req.getAddress());
        entity.setLongitude(req.getLongitude());
        entity.setLatitude(req.getLatitude());
        entity.setOpenTime(req.getOpenTime());
        entity.setCloseTime(req.getCloseTime());
        fraStoreMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void switchBusinessStatus(Long id, StoreBusinessStatusReq req) {
        if (req.getBusinessStatus() != 0 && req.getBusinessStatus() != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "businessStatus 仅支持 0/1");
        }
        List<String> roles = SecurityUtils.getRolesFromSession();
        boolean franchisee = roles.contains(ROLE_FRANCHISEE);
        boolean storeManager = roles.contains(ROLE_STORE_MANAGER);
        // 契约角色列无 R6：加盟商无营业状态切换权限
        if (franchisee && !storeManager) {
            throw new BizException(ErrorCode.FORBIDDEN, "无该操作权限");
        }
        if (storeManager && !franchisee) {
            // R7 仅本店（绑定校验）；R2/R5 等总部角色任意本租户门店
            checkStorePermission(id);
        }
        requireExists(id);
        FraStore entity = new FraStore();
        entity.setId(id);
        entity.setBusinessStatus(req.getBusinessStatus());
        fraStoreMapper.updateById(entity);
    }

    @Override
    public List<StoreNearbyVO> nearby(BigDecimal lng, BigDecimal lat) {
        // 仅在营且营业中的门店（租户隔离依赖下方手动注入的 TenantContext，由租户插件自动拼 tenant_id）
        List<FraStore> stores = fraStoreMapper.selectList(new LambdaQueryWrapper<FraStore>()
                .eq(FraStore::getStatus, 1)
                .eq(FraStore::getBusinessStatus, 1)
                .isNotNull(FraStore::getLongitude)
                .isNotNull(FraStore::getLatitude));

        return stores.stream()
                .map(s -> {
                    StoreNearbyVO vo = new StoreNearbyVO();
                    vo.setId(s.getId());
                    vo.setStoreName(s.getStoreName());
                    vo.setAddress(s.getAddress());
                    vo.setLongitude(s.getLongitude());
                    vo.setLatitude(s.getLatitude());
                    vo.setOpenTime(s.getOpenTime() == null ? null : s.getOpenTime().toString());
                    vo.setCloseTime(s.getCloseTime() == null ? null : s.getCloseTime().toString());
                    vo.setDistanceMeters(haversineMeters(lng.doubleValue(), lat.doubleValue(),
                            s.getLongitude().doubleValue(), s.getLatitude().doubleValue()));
                    return vo;
                })
                .sorted(Comparator.comparing(StoreNearbyVO::getDistanceMeters))
                .limit(NEARBY_LIMIT)
                .toList();
    }

    // ---------------- 内部 ----------------

    /** null=总部视角（本租户全量）；非 null=限定绑定门店范围 */
    private List<Long> currentScopeStoreIds() {
        List<String> roles = SecurityUtils.getRolesFromSession();
        if (!roles.contains(ROLE_FRANCHISEE) && !roles.contains(ROLE_STORE_MANAGER)) {
            return null;
        }
        return sysUserStoreMapper.selectList(
                        new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, SecurityUtils.getUserId()))
                .stream().map(SysUserStore::getStoreId).distinct().toList();
    }

    /** R6/R7 校验门店是否绑定当前用户；总部角色直接放行 */
    private void checkStorePermission(Long storeId) {
        List<String> roles = SecurityUtils.getRolesFromSession();
        if (!roles.contains(ROLE_FRANCHISEE) && !roles.contains(ROLE_STORE_MANAGER)) {
            return;
        }
        Long count = sysUserStoreMapper.selectCount(new LambdaQueryWrapper<SysUserStore>()
                .eq(SysUserStore::getUserId, SecurityUtils.getUserId())
                .eq(SysUserStore::getStoreId, storeId));
        if (count == null || count == 0) {
            throw new BizException(ErrorCode.FORBIDDEN, "无该门店权限");
        }
    }

    private FraStore requireExists(Long id) {
        FraStore entity = fraStoreMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.STORE_NOT_FOUND);
        }
        return entity;
    }

    /** franchiseeId 非空时必须存在且属于本租户（租户插件自动限定） */
    private void validateFranchisee(Long franchiseeId) {
        if (franchiseeId == null) {
            return;
        }
        if (fraFranchiseeMapper.selectById(franchiseeId) == null) {
            throw new BizException(ErrorCode.FRANCHISEE_NOT_FOUND);
        }
    }

    /** storeCode 租户内唯一；excludeId 用于编辑时排除自身 */
    private void checkStoreCodeDuplicate(String storeCode, Long excludeId) {
        Long count = fraStoreMapper.selectCount(new LambdaQueryWrapper<FraStore>()
                .eq(FraStore::getStoreCode, storeCode)
                .ne(excludeId != null, FraStore::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "门店编码已存在");
        }
    }

    private void copyCreateFields(FraStore entity, StoreCreateReq req) {
        entity.setStoreCode(req.getStoreCode());
        entity.setStoreName(req.getStoreName());
        entity.setFranchiseeId(req.getFranchiseeId());
        entity.setAddress(req.getAddress());
        entity.setLongitude(req.getLongitude());
        entity.setLatitude(req.getLatitude());
        entity.setOpenTime(req.getOpenTime());
        entity.setCloseTime(req.getCloseTime());
    }

    /** Haversine 球面距离，四舍五入整数米 */
    private Long haversineMeters(double lng1, double lat1, double lng2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(EARTH_RADIUS_METERS * c).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
