package com.teasaas.product.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.product.dto.ProductSaveDTO;
import com.teasaas.product.entity.PrdProduct;
import com.teasaas.product.vo.ProductDetailVO;

/**
 * 商品服务（F-PRD-01 产品库与商品，R2/R3）
 */
public interface PrdProductService {

    /** 商品分页列表 */
    PageResult<PrdProduct> page(int page, int size, Long categoryId, Integer publishStatus, String keyword);

    /** 商品详情（含配方名称与规格组） */
    ProductDetailVO detail(Long id);

    /** 发布商品（创建即发布，publishStatus=1） */
    Long create(ProductSaveDTO dto);

    /** 编辑商品（基本信息 + 规格全量重绑；publishStatus 可选传入用于重新发布） */
    void update(Long id, ProductSaveDTO dto);

    /** 调整建议售价（分） */
    void adjustPrice(Long id, Integer price);

    /** 下架（publishStatus=0） */
    void offShelf(Long id);
}
