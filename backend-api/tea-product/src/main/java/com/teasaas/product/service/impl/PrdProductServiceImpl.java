package com.teasaas.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.bom.entity.BomFormula;
import com.teasaas.bom.mapper.BomFormulaMapper;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.product.dto.ProductSaveDTO;
import com.teasaas.product.dto.ProductSpecDTO;
import com.teasaas.product.dto.ProductSpecOptionDTO;
import com.teasaas.product.entity.PrdProduct;
import com.teasaas.product.entity.PrdProductSpec;
import com.teasaas.product.entity.PrdProductSpecOption;
import com.teasaas.product.mapper.PrdProductMapper;
import com.teasaas.product.mapper.PrdProductSpecMapper;
import com.teasaas.product.mapper.PrdProductSpecOptionMapper;
import com.teasaas.product.service.PrdProductService;
import com.teasaas.product.vo.ProductDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品服务实现（F-PRD-01，接口需求 6.1；R2/R3 总部产品研发）
 *
 * <p>发布 = 创建即 publishStatus=1；编辑走"规格全量重绑"（同 BOM 明细模式）：
 * 先删旧规格组/选项再插入，事务保证一致。uk_product(tenant_id, product_code, del_flag)
 * 冲突 → 商品编码已存在。</p>
 */
@Service
@RequiredArgsConstructor
public class PrdProductServiceImpl implements PrdProductService {

    private static final String ROLE_R2 = "R2";
    private static final String ROLE_R3 = "R3";

    private final PrdProductMapper prdProductMapper;
    private final PrdProductSpecMapper prdProductSpecMapper;
    private final PrdProductSpecOptionMapper prdProductSpecOptionMapper;
    private final BomFormulaMapper bomFormulaMapper;

    // ==================== 查询 ====================

    @Override
    public PageResult<PrdProduct> page(int page, int size, Long categoryId, Integer publishStatus, String keyword) {
        LambdaQueryWrapper<PrdProduct> wrapper = new LambdaQueryWrapper<PrdProduct>()
                .eq(categoryId != null, PrdProduct::getCategoryId, categoryId)
                .eq(publishStatus != null, PrdProduct::getPublishStatus, publishStatus)
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(PrdProduct::getProductName, keyword)
                                .or().like(PrdProduct::getProductCode, keyword))
                .orderByDesc(PrdProduct::getId);
        Page<PrdProduct> result = prdProductMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public ProductDetailVO detail(Long id) {
        PrdProduct product = requireExists(id);
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(product.getId());
        vo.setCategoryId(product.getCategoryId());
        vo.setProductCode(product.getProductCode());
        vo.setProductName(product.getProductName());
        vo.setFormulaId(product.getFormulaId());
        vo.setSuggestPrice(product.getSuggestPrice());
        vo.setImage(product.getImage());
        vo.setDescription(product.getDescription());
        vo.setPublishStatus(product.getPublishStatus());
        vo.setCreateTime(product.getCreateTime());
        BomFormula formula = bomFormulaMapper.selectById(product.getFormulaId());
        if (formula != null) {
            vo.setFormulaName(formula.getFormulaName());
        }
        vo.setSpecs(loadSpecs(List.of(id)).getOrDefault(id, List.of()));
        return vo;
    }

    // ==================== 发布 / 编辑 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductSaveDTO dto) {
        requireProductRole();
        checkRefs(dto);
        Long tenantId = SecurityUtils.getTenantIdFromSession();

        PrdProduct product = new PrdProduct();
        product.setCategoryId(dto.getCategoryId());
        product.setProductCode(dto.getProductCode());
        product.setProductName(dto.getProductName());
        product.setFormulaId(dto.getFormulaId());
        product.setSuggestPrice(dto.getSuggestPrice());
        product.setImage(dto.getImage());
        product.setDescription(dto.getDescription());
        // 发布商品：创建即发布
        product.setPublishStatus(1);
        product.setStatus(1);
        product.setTenantId(tenantId);
        try {
            prdProductMapper.insert(product);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "商品编码已存在");
        }
        rebindSpecs(product.getId(), dto.getSpecs(), tenantId);
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductSaveDTO dto) {
        requireProductRole();
        requireExists(id);
        checkRefs(dto);

        PrdProduct product = new PrdProduct();
        product.setId(id);
        product.setCategoryId(dto.getCategoryId());
        product.setProductCode(dto.getProductCode());
        product.setProductName(dto.getProductName());
        product.setFormulaId(dto.getFormulaId());
        product.setSuggestPrice(dto.getSuggestPrice());
        product.setImage(dto.getImage());
        product.setDescription(dto.getDescription());
        // publishStatus 不传则保持现状；传 1 可将下架商品重新发布
        if (dto.getPublishStatus() != null) {
            product.setPublishStatus(dto.getPublishStatus());
        }
        try {
            prdProductMapper.updateById(product);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "商品编码已存在");
        }
        rebindSpecs(id, dto.getSpecs(), SecurityUtils.getTenantIdFromSession());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPrice(Long id, Integer price) {
        requireProductRole();
        requireExists(id);
        PrdProduct update = new PrdProduct();
        update.setId(id);
        update.setSuggestPrice(price);
        prdProductMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offShelf(Long id) {
        requireProductRole();
        requireExists(id);
        PrdProduct update = new PrdProduct();
        update.setId(id);
        update.setPublishStatus(0);
        prdProductMapper.updateById(update);
    }

    // ==================== 私有工具 ====================

    /** 引用校验：配方必须存在；分类若传必须存在 */
    private void checkRefs(ProductSaveDTO dto) {
        if (bomFormulaMapper.selectById(dto.getFormulaId()) == null) {
            throw new BizException(ErrorCode.FORMULA_NOT_FOUND);
        }
        // categoryId 合法性由外键语义保证：不传或传错只影响筛选，不阻断（宽松处理，避免分类被删后商品不可编辑）
    }

    /**
     * 规格全量重绑（同 BOM 配方明细模式）：删旧插新，含选项级联
     */
    private void rebindSpecs(Long productId, List<ProductSpecDTO> specs, Long tenantId) {
        // 级联删除旧规格：先选项后规格组
        List<PrdProductSpec> oldSpecs = prdProductSpecMapper.selectList(
                new LambdaQueryWrapper<PrdProductSpec>().eq(PrdProductSpec::getProductId, productId));
        if (!oldSpecs.isEmpty()) {
            Set<Long> specIds = oldSpecs.stream().map(PrdProductSpec::getId).collect(Collectors.toSet());
            prdProductSpecOptionMapper.delete(new LambdaQueryWrapper<PrdProductSpecOption>()
                    .in(PrdProductSpecOption::getSpecId, specIds));
            prdProductSpecMapper.delete(new LambdaQueryWrapper<PrdProductSpec>()
                    .eq(PrdProductSpec::getProductId, productId));
        }
        if (specs == null || specs.isEmpty()) {
            return;
        }
        int specSort = 0;
        for (ProductSpecDTO specDTO : specs) {
            PrdProductSpec spec = new PrdProductSpec();
            spec.setTenantId(tenantId);
            spec.setProductId(productId);
            spec.setSpecName(specDTO.getSpecName());
            spec.setIsRequired(specDTO.getIsRequired() == null || specDTO.getIsRequired() == 1 ? 1 : 0);
            spec.setSort(specDTO.getSort() == null ? specSort++ : specDTO.getSort());
            prdProductSpecMapper.insert(spec);
            if (specDTO.getOptions() != null) {
                int optionSort = 0;
                for (ProductSpecOptionDTO optionDTO : specDTO.getOptions()) {
                    PrdProductSpecOption option = new PrdProductSpecOption();
                    option.setTenantId(tenantId);
                    option.setSpecId(spec.getId());
                    option.setOptionName(optionDTO.getOptionName());
                    option.setExtraPrice(optionDTO.getExtraPrice() == null ? 0 : optionDTO.getExtraPrice());
                    option.setSort(optionDTO.getSort() == null ? optionSort++ : optionDTO.getSort());
                    prdProductSpecOptionMapper.insert(option);
                }
            }
        }
    }

    /** 批量装配规格组（详情/C端复用） */
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

    private PrdProduct requireExists(Long id) {
        // 租户插件自动拼 tenant_id 防跨租户访问
        PrdProduct product = prdProductMapper.selectById(id);
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private void requireProductRole() {
        List<String> roles = SecurityUtils.getRolesFromSession();
        if (!roles.contains(ROLE_R2) && !roles.contains(ROLE_R3)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部产品角色可操作商品库");
        }
    }
}
