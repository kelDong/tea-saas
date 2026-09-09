package com.teasaas.bom.service;

import com.teasaas.bom.dto.FormulaSaveReq;
import com.teasaas.bom.dto.ProfitCalcReq;
import com.teasaas.bom.entity.BomFormula;
import com.teasaas.bom.vo.FormulaDetailVO;
import com.teasaas.bom.vo.ProfitCalcVO;
import com.teasaas.common.result.PageResult;

/**
 * 配方服务（F-BOM-02 / F-BOM-03）
 */
public interface BomFormulaService {

    /**
     * 分页列表，keyword 模糊匹配名称/编码
     */
    PageResult<BomFormula> page(int page, int size, String keyword);

    /**
     * 配方详情（含明细，VO 组装；不存在抛 FORMULA_NOT_FOUND）
     */
    FormulaDetailVO getDetail(Long id);

    /**
     * 新增配方（明细全量绑定，重复 materialId 后端去重保留最后一条）
     */
    Long create(FormulaSaveReq req);

    /**
     * 编辑配方（version+1，同一条记录 update；明细全量重绑）
     */
    void update(Long id, FormulaSaveReq req);

    /**
     * 复制配方（新编码 -C/-C2/-C3 递推，名称加"(副本)"，明细一并复制）
     */
    Long copy(Long id);

    /**
     * 停用/启用（status：0 停用 / 1 启用）
     */
    void updateStatus(Long id, Integer status);

    /**
     * 发布（状态 1→2，进入产品库候选——产品库联动属批次 2）
     */
    void publish(Long id);

    /**
     * 毛利试算（F-BOM-03），结果回写 bom_formula.cost_total
     */
    ProfitCalcVO calcProfit(ProfitCalcReq req);
}
