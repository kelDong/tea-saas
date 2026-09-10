package com.teasaas.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teasaas.inventory.entity.InvStoreMaterialStock;
import com.teasaas.inventory.vo.InventorySummaryVO;
import com.teasaas.inventory.vo.StockWarningVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 门店原料库存 Mapper（F-INV-01/03）
 * 数量字段为 DECIMAL(10,3)（非金额，红线仅约束金额）；
 * tenant_id 由租户插件自动追加（含 XML join 表）。
 */
@Mapper
public interface InvStoreMaterialStockMapper extends BaseMapper<InvStoreMaterialStock> {

    /** 低库存列表（stock_qty < warn_threshold，可选门店过滤） */
    List<StockWarningVO> selectWarnings(@Param("storeId") Long storeId);

    /** 总部库存总览（按原料聚合） */
    List<InventorySummaryVO> selectSummary();
}
