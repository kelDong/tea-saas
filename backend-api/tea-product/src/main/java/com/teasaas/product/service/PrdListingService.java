package com.teasaas.product.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.product.dto.ListingCreateDTO;
import com.teasaas.product.entity.PrdProduct;
import com.teasaas.product.vo.AppCategoryVO;
import com.teasaas.product.vo.AppProductVO;
import com.teasaas.product.vo.ListingVO;
import com.teasaas.product.vo.SellableCheckVO;

import java.util.List;

/**
 * 门店上架服务（F-PRD-02 加盟商上架 / F-PRD-03 可售状态联动）
 */
public interface PrdListingService {

    /** 可上架商品（产品库已发布商品，R6） */
    PageResult<PrdProduct> availableProducts(int page, int size, String keyword);

    /** 本店上架列表（含实时可售标记；R6/R7 名下门店） */
    List<ListingVO> listByStore(Long storeId);

    /** 上架商品（仅 R6，名下门店；售价不得高于总部建议价） */
    Long create(ListingCreateDTO dto);

    /** 调整本店售价（R6） */
    void adjustPrice(Long id, Integer price);

    /** 下架（R6） */
    void offShelf(Long id);

    /** 商品可售校验（R6/R7，含缺料明细） */
    List<SellableCheckVO> checkSellable(List<Long> listingIds);

    /** C 端门店可售商品列表（公开；仅返回库存充足商品） */
    List<AppProductVO> appStoreProducts(Long storeId, Long categoryId);

    /** C 端门店商品分类（公开；仅返回该店有可售商品的分类，按 sort 排序） */
    List<AppCategoryVO> appStoreCategories(Long storeId);
}
