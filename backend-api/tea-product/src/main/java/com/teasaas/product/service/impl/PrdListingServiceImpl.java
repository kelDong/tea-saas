package com.teasaas.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.franchise.entity.FraStore;
import com.teasaas.franchise.mapper.FraStoreMapper;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.product.dto.ListingCreateDTO;
import com.teasaas.product.entity.PrdListing;
import com.teasaas.product.entity.PrdProduct;
import com.teasaas.product.entity.PrdProductSpec;
import com.teasaas.product.entity.PrdProductSpecOption;
import com.teasaas.product.mapper.PrdListingMapper;
import com.teasaas.product.mapper.PrdProductMapper;
import com.teasaas.product.mapper.PrdProductSpecMapper;
import com.teasaas.product.mapper.PrdProductSpecOptionMapper;
import com.teasaas.product.service.PrdListingService;
import com.teasaas.product.vo.AppCategoryVO;
import com.teasaas.product.vo.AppProductVO;
import com.teasaas.product.vo.ListingVO;
import com.teasaas.product.vo.ProductDetailVO;
import com.teasaas.product.vo.SellableCheckVO;
import com.teasaas.product.vo.ShortageRowVO;
import com.teasaas.system.entity.SysUserStore;
import com.teasaas.system.mapper.SysUserStoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 门店上架服务实现（F-PRD-02 加盟商上架 / F-PRD-03 可售联动，接口需求 6.2/6.3）
 *
 * <p>数据权限：上架/调价/下架仅 R6（加盟商老板）且限名下门店；可售校验 R6/R7。
 * 售价管控：salePrice 不得高于总部 suggestPrice（P1 加盟管控决策）。
 * 可售判定走 13.2 实时 SQL（MVP 不落 sellable_flag）。</p>
 */
@Service
@RequiredArgsConstructor
public class PrdListingServiceImpl implements PrdListingService {

    private static final String ROLE_R6 = "R6";
    private static final String ROLE_R7 = "R7";

    private final PrdListingMapper prdListingMapper;
    private final PrdProductMapper prdProductMapper;
    private final PrdProductSpecMapper prdProductSpecMapper;
    private final PrdProductSpecOptionMapper prdProductSpecOptionMapper;
    private final FraStoreMapper fraStoreMapper;
    private final SysUserStoreMapper sysUserStoreMapper;

    // ==================== 查询 ====================

    @Override
    public PageResult<PrdProduct> availableProducts(int page, int size, String keyword) {
        requireRole(ROLE_R6, "仅加盟商可查看可上架商品");
        LambdaQueryWrapper<PrdProduct> wrapper = new LambdaQueryWrapper<PrdProduct>()
                .eq(PrdProduct::getPublishStatus, 1)
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(PrdProduct::getProductName, keyword)
                                .or().like(PrdProduct::getProductCode, keyword))
                .orderByDesc(PrdProduct::getId);
        Page<PrdProduct> result = prdProductMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public List<ListingVO> listByStore(Long storeId) {
        checkStoreAccess(storeId);
        return prdListingMapper.selectStoreList(storeId, null);
    }

    // ==================== 上架 / 调价 / 下架（仅 R6） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ListingCreateDTO dto) {
        requireRole(ROLE_R6, "仅加盟商可上架商品");
        // ① 名下门店校验
        checkStoreAccess(dto.getStoreId());
        // ② 商品校验：存在且已发布
        PrdProduct product = prdProductMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (product.getPublishStatus() == null || product.getPublishStatus() != 1) {
            throw new BizException(ErrorCode.PRODUCT_NOT_PUBLISHED);
        }
        // ③ 售价管控：不得高于总部建议价
        if (dto.getSalePrice() > product.getSuggestPrice()) {
            throw new BizException(ErrorCode.SALE_PRICE_EXCEED_LIMIT);
        }
        // ④ upsert：已下架记录复活改价；上架中则拒绝
        PrdListing existing = prdListingMapper.selectOne(new LambdaQueryWrapper<PrdListing>()
                .eq(PrdListing::getStoreId, dto.getStoreId())
                .eq(PrdListing::getProductId, dto.getProductId()));
        if (existing != null) {
            if (existing.getStatus() != null && existing.getStatus() == 1) {
                throw new BizException(ErrorCode.LISTING_DUPLICATED);
            }
            PrdListing revive = new PrdListing();
            revive.setId(existing.getId());
            revive.setSalePrice(dto.getSalePrice());
            revive.setStatus(1);
            prdListingMapper.updateById(revive);
            return existing.getId();
        }
        PrdListing listing = new PrdListing();
        listing.setTenantId(SecurityUtils.getTenantIdFromSession());
        listing.setStoreId(dto.getStoreId());
        listing.setProductId(dto.getProductId());
        listing.setSalePrice(dto.getSalePrice());
        listing.setSort(0);
        listing.setStatus(1);
        try {
            prdListingMapper.insert(listing);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.LISTING_DUPLICATED);
        }
        return listing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPrice(Long id, Integer price) {
        requireRole(ROLE_R6, "仅加盟商可调整本店售价");
        PrdListing listing = requireListingExists(id);
        checkStoreAccess(listing.getStoreId());
        PrdProduct product = prdProductMapper.selectById(listing.getProductId());
        if (product != null && price > product.getSuggestPrice()) {
            throw new BizException(ErrorCode.SALE_PRICE_EXCEED_LIMIT);
        }
        PrdListing update = new PrdListing();
        update.setId(id);
        update.setSalePrice(price);
        prdListingMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offShelf(Long id) {
        requireRole(ROLE_R6, "仅加盟商可下架商品");
        PrdListing listing = requireListingExists(id);
        checkStoreAccess(listing.getStoreId());
        PrdListing update = new PrdListing();
        update.setId(id);
        update.setStatus(0);
        prdListingMapper.updateById(update);
    }

    // ==================== 可售联动（F-PRD-03） ====================

    @Override
    public List<SellableCheckVO> checkSellable(List<Long> listingIds) {
        if (!hasRole(ROLE_R6) && !hasRole(ROLE_R7)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅加盟商或店长可校验可售状态");
        }
        // 归属校验：listing 必须属于当前租户且（非 R6 场景省略，租户插件兜底）名下门店
        List<PrdListing> listings = prdListingMapper.selectBatchIds(listingIds);
        if (listings.size() != listingIds.stream().distinct().count()) {
            throw new BizException(ErrorCode.LISTING_NOT_FOUND);
        }
        for (PrdListing listing : listings) {
            checkStoreAccess(listing.getStoreId());
        }
        // 缺料行按 listing 分组 → 有缺料即不可售
        Map<Long, List<ShortageRowVO>> shortageMap = prdListingMapper.selectShortages(
                        listingIds.stream().distinct().toList()).stream()
                .collect(Collectors.groupingBy(ShortageRowVO::getListingId));
        return listings.stream().map(listing -> {
            SellableCheckVO vo = new SellableCheckVO();
            vo.setListingId(listing.getId());
            List<ShortageRowVO> rows = shortageMap.getOrDefault(listing.getId(), List.of());
            vo.setSellable(rows.isEmpty());
            vo.setShortages(rows.stream().map(row -> {
                SellableCheckVO.ShortageNode node = new SellableCheckVO.ShortageNode();
                node.setMaterialId(row.getMaterialId());
                node.setMaterialName(row.getMaterialName());
                node.setUnit(row.getUnit());
                node.setNeedQty(row.getNeedQty());
                node.setStockQty(row.getStockQty());
                return node;
            }).toList());
            return vo;
        }).toList();
    }

    @Override
    public List<AppProductVO> appStoreProducts(Long storeId, Long categoryId) {
        // C 端公开：TenantContext 已由 Controller 从 X-Tenant-Id 注入，租户插件隔离
        FraStore store = fraStoreMapper.selectById(storeId);
        if (store == null) {
            throw new BizException(ErrorCode.STORE_NOT_FOUND);
        }
        List<AppProductVO> products = prdListingMapper.selectAppProducts(storeId, categoryId);
        // 批量装配规格组
        if (!products.isEmpty()) {
            List<Long> productIds = products.stream().map(AppProductVO::getProductId).toList();
            Map<Long, List<ProductDetailVO.SpecNode>> specMap = loadSpecs(productIds);
            products.forEach(p -> p.setSpecs(specMap.getOrDefault(p.getProductId(), List.of())));
        }
        return products;
    }

    @Override
    public List<AppCategoryVO> appStoreCategories(Long storeId) {
        // C 端公开：TenantContext 已由 Controller 从 X-Tenant-Id 注入，租户插件隔离
        FraStore store = fraStoreMapper.selectById(storeId);
        if (store == null) {
            throw new BizException(ErrorCode.STORE_NOT_FOUND);
        }
        return prdListingMapper.selectAppCategories(storeId);
    }

    // ==================== 私有工具 ====================

    /** 批量装配规格组（与 PrdProductServiceImpl 同构，C 端展示用） */
    private Map<Long, List<ProductDetailVO.SpecNode>> loadSpecs(List<Long> productIds) {
        List<PrdProductSpec> specs = prdProductSpecMapper.selectList(
                new LambdaQueryWrapper<PrdProductSpec>()
                        .in(PrdProductSpec::getProductId, productIds)
                        .orderByAsc(PrdProductSpec::getSort).orderByAsc(PrdProductSpec::getId));
        if (specs.isEmpty()) {
            return Map.of();
        }
        Set<Long> specIds = specs.stream().map(PrdProductSpec::getId).collect(Collectors.toSet());
        Map<Long, List<PrdProductSpecOption>> optionMap = prdProductSpecOptionMapper.selectList(
                        new LambdaQueryWrapper<PrdProductSpecOption>()
                                .in(PrdProductSpecOption::getSpecId, specIds)
                                .orderByAsc(PrdProductSpecOption::getSort).orderByAsc(PrdProductSpecOption::getId))
                .stream()
                .collect(Collectors.groupingBy(PrdProductSpecOption::getSpecId));
        return specs.stream()
                .collect(Collectors.groupingBy(PrdProductSpec::getProductId,
                        Collectors.mapping(spec -> {
                            ProductDetailVO.SpecNode node = new ProductDetailVO.SpecNode();
                            node.setId(spec.getId());
                            node.setSpecName(spec.getSpecName());
                            node.setIsRequired(spec.getIsRequired());
                            node.setSort(spec.getSort());
                            node.setOptions(optionMap.getOrDefault(spec.getId(), List.of()).stream()
                                    .map(option -> {
                                        ProductDetailVO.OptionNode optionNode = new ProductDetailVO.OptionNode();
                                        optionNode.setId(option.getId());
                                        optionNode.setOptionName(option.getOptionName());
                                        optionNode.setExtraPrice(option.getExtraPrice());
                                        optionNode.setSort(option.getSort());
                                        return optionNode;
                                    }).toList());
                            return node;
                        }, Collectors.toList())));
    }

    private PrdListing requireListingExists(Long id) {
        // 租户插件自动拼 tenant_id 防跨租户访问
        PrdListing listing = prdListingMapper.selectById(id);
        if (listing == null) {
            throw new BizException(ErrorCode.LISTING_NOT_FOUND);
        }
        return listing;
    }

    /**
     * 门店数据权限：R6 要求名下门店（sys_user_store 绑定）
     */
    private void checkStoreAccess(Long storeId) {
        if (hasRole(ROLE_R6)) {
            List<Long> storeIds = sysUserStoreMapper.selectList(
                            new LambdaQueryWrapper<SysUserStore>().eq(SysUserStore::getUserId, SecurityUtils.getUserId()))
                    .stream().map(SysUserStore::getStoreId).distinct().toList();
            if (!storeIds.contains(storeId)) {
                throw new BizException(ErrorCode.FORBIDDEN, "非名下门店");
            }
        }
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
