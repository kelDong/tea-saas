package com.teasaas.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.product.dto.CategorySaveDTO;
import com.teasaas.product.entity.PrdCategory;
import com.teasaas.product.mapper.PrdCategoryMapper;
import com.teasaas.product.service.PrdCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品分类服务实现（F-PRD-01 分类管理，R2/R3）
 * prd_category 为平铺表（非 BaseEntity），tenant_id 显式赋值，租户插件兜底隔离。
 */
@Service
@RequiredArgsConstructor
public class PrdCategoryServiceImpl implements PrdCategoryService {

    private final PrdCategoryMapper prdCategoryMapper;

    @Override
    public List<PrdCategory> list() {
        return prdCategoryMapper.selectList(new LambdaQueryWrapper<PrdCategory>()
                .orderByAsc(PrdCategory::getParentId)
                .orderByAsc(PrdCategory::getSort)
                .orderByAsc(PrdCategory::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CategorySaveDTO dto) {
        checkParent(dto.getParentId());
        PrdCategory entity = new PrdCategory();
        entity.setTenantId(SecurityUtils.getTenantIdFromSession());
        entity.setCategoryName(dto.getCategoryName());
        entity.setParentId(dto.getParentId());
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        prdCategoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategorySaveDTO dto) {
        requireExists(id);
        checkParent(dto.getParentId());
        if (dto.getParentId().equals(id)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "父分类不能是自身");
        }
        PrdCategory entity = new PrdCategory();
        entity.setId(id);
        entity.setCategoryName(dto.getCategoryName());
        entity.setParentId(dto.getParentId());
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        prdCategoryMapper.updateById(entity);
    }

    private void checkParent(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        requireExists(parentId);
    }

    private PrdCategory requireExists(Long id) {
        // 租户插件自动拼 tenant_id 防跨租户访问
        PrdCategory category = prdCategoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }
}
