package com.teasaas.product.service;

import com.teasaas.product.dto.CategorySaveDTO;
import com.teasaas.product.entity.PrdCategory;

import java.util.List;

/**
 * 商品分类服务（F-PRD-01 分类管理）
 */
public interface PrdCategoryService {

    /** 全量分类列表（按 parentId、sort 排序） */
    List<PrdCategory> list();

    Long create(CategorySaveDTO dto);

    void update(Long id, CategorySaveDTO dto);
}
