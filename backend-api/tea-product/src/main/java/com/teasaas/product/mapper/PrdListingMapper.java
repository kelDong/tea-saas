package com.teasaas.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teasaas.product.entity.PrdListing;
import com.teasaas.product.vo.AppProductVO;
import com.teasaas.product.vo.ListingVO;
import com.teasaas.product.vo.ShortageRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 门店上架 Mapper（F-PRD-02/03）
 * 可售判定（数据库设计 13.2）：门店上架商品的配方原料均充足才可售；
 * tenant_id 由租户插件对 XML 中所有表（含 join）自动追加，无需手写。
 */
@Mapper
public interface PrdListingMapper extends BaseMapper<PrdListing> {

    /** 本店上架列表（含实时 sellable 计算） */
    List<ListingVO> selectStoreList(@Param("storeId") Long storeId, @Param("status") Integer status);

    /** 缺料明细行（可售校验用：配方用量 vs 当前库存） */
    List<ShortageRowVO> selectShortages(@Param("listingIds") List<Long> listingIds);

    /** C 端门店可售商品（仅上架+已发布+原料充足） */
    List<AppProductVO> selectAppProducts(@Param("storeId") Long storeId, @Param("categoryId") Long categoryId);
}
