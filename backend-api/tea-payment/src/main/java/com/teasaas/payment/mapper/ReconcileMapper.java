package com.teasaas.payment.mapper;

import com.teasaas.payment.vo.ReconcileVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 对账 Mapper（F-PAY-03，数据库设计 13.4）
 * tenant_id 由租户插件自动追加（含 XML join 表，同 InvStoreMaterialStockMapper 先例）。
 */
@Mapper
public interface ReconcileMapper {

    /**
     * 库存账实差异：流水累计 SUM(change_qty) &lt;&gt; 台账现值 stock_qty 的门店/原料组合
     * （数量为 DECIMAL(10,3)，非金额，红线仅约束金额）
     */
    List<ReconcileVO.StockDiffNode> selectStockDiffs();
}
