package com.teasaas.bom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.bom.dto.FormulaSaveReq;
import com.teasaas.bom.dto.ProfitCalcReq;
import com.teasaas.bom.entity.BomFormula;
import com.teasaas.bom.entity.BomFormulaItem;
import com.teasaas.bom.entity.BomMaterial;
import com.teasaas.bom.mapper.BomFormulaItemMapper;
import com.teasaas.bom.mapper.BomFormulaMapper;
import com.teasaas.bom.mapper.BomMaterialMapper;
import com.teasaas.bom.service.BomFormulaService;
import com.teasaas.bom.vo.FormulaDetailVO;
import com.teasaas.bom.vo.ProfitCalcVO;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 配方服务实现（F-BOM-02 / F-BOM-03）
 *
 * <p>版本策略：schema 无版本历史表，编辑即同一条记录 update 并 version+1（已拍板决策）。
 * 状态语义：0 停用 / 1 正常 / 2 已发布（02_data.sql 无 bom_ 种子数据，按默认方案）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BomFormulaServiceImpl implements BomFormulaService {

    /** status：停用 */
    private static final int STATUS_DISABLED = 0;
    /** status：正常 */
    private static final int STATUS_ENABLED = 1;
    /** status：已发布 */
    private static final int STATUS_PUBLISHED = 2;

    private final BomFormulaMapper bomFormulaMapper;
    private final BomFormulaItemMapper bomFormulaItemMapper;
    private final BomMaterialMapper bomMaterialMapper;

    @Override
    public PageResult<BomFormula> page(int page, int size, String keyword) {
        // 租户插件自动拼 tenant_id；keyword 同时模糊匹配名称/编码
        LambdaQueryWrapper<BomFormula> wrapper = new LambdaQueryWrapper<BomFormula>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(BomFormula::getFormulaName, keyword)
                        .or()
                        .like(BomFormula::getFormulaCode, keyword))
                .orderByDesc(BomFormula::getId);
        Page<BomFormula> result = bomFormulaMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public FormulaDetailVO getDetail(Long id) {
        BomFormula formula = getById(id);
        List<BomFormulaItem> items = listItems(id);

        // 批量补齐原料名称/单位（租户插件自动过滤 tenant_id）
        Map<Long, BomMaterial> materialMap = items.isEmpty() ? Map.of()
                : bomMaterialMapper.selectBatchIds(items.stream()
                        .map(BomFormulaItem::getMaterialId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(BomMaterial::getId, Function.identity()));

        FormulaDetailVO vo = new FormulaDetailVO();
        vo.setId(formula.getId());
        vo.setTenantId(formula.getTenantId());
        vo.setFormulaCode(formula.getFormulaCode());
        vo.setFormulaName(formula.getFormulaName());
        vo.setVersion(formula.getVersion());
        vo.setCostTotal(formula.getCostTotal());
        vo.setStatus(formula.getStatus());
        vo.setCreateTime(formula.getCreateTime());
        vo.setUpdateTime(formula.getUpdateTime());
        vo.setItems(items.stream().map(item -> {
            FormulaDetailVO.ItemVO itemVO = new FormulaDetailVO.ItemVO();
            itemVO.setId(item.getId());
            itemVO.setMaterialId(item.getMaterialId());
            BomMaterial material = materialMap.get(item.getMaterialId());
            if (material != null) {
                itemVO.setMaterialName(material.getMaterialName());
                itemVO.setUnit(material.getUnit());
            }
            itemVO.setQty(item.getQty());
            itemVO.setSort(item.getSort());
            return itemVO;
        }).toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FormulaSaveReq req) {
        checkCodeUnique(req.getFormulaCode(), null);
        Long tenantId = SecurityUtils.getTenantIdFromSession();
        BomFormula formula = new BomFormula();
        formula.setTenantId(tenantId);
        formula.setFormulaCode(req.getFormulaCode());
        formula.setFormulaName(req.getFormulaName());
        formula.setVersion(1);
        formula.setStatus(STATUS_ENABLED);
        bomFormulaMapper.insert(formula);
        rebindItems(tenantId, formula.getId(), req.getItems());
        return formula.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FormulaSaveReq req) {
        BomFormula formula = getById(id);
        checkCodeUnique(req.getFormulaCode(), id);
        // 版本策略：无历史表，同一条记录 update，version+1
        BomFormula update = new BomFormula();
        update.setId(id);
        update.setFormulaCode(req.getFormulaCode());
        update.setFormulaName(req.getFormulaName());
        update.setVersion(formula.getVersion() + 1);
        bomFormulaMapper.updateById(update);
        rebindItems(formula.getTenantId(), id, req.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id) {
        BomFormula source = getById(id);
        Long tenantId = source.getTenantId();

        BomFormula copy = new BomFormula();
        copy.setTenantId(tenantId);
        copy.setFormulaCode(nextCopyCode(source.getFormulaCode()));
        // 防止"(副本)"后缀超过 formula_name VARCHAR(64)
        String baseName = source.getFormulaName();
        copy.setFormulaName(baseName.substring(0, Math.min(baseName.length(), 64 - 4)) + "(副本)");
        copy.setVersion(1);
        // 与源状态一致；源已发布则副本回到 1（需重新发布）
        copy.setStatus(source.getStatus() == STATUS_PUBLISHED ? STATUS_ENABLED : source.getStatus());
        copy.setCostTotal(source.getCostTotal());
        bomFormulaMapper.insert(copy);

        List<BomFormulaItem> items = listItems(id);
        LocalDateTime now = LocalDateTime.now();
        for (BomFormulaItem item : items) {
            BomFormulaItem copyItem = new BomFormulaItem();
            copyItem.setTenantId(tenantId);
            copyItem.setFormulaId(copy.getId());
            copyItem.setMaterialId(item.getMaterialId());
            copyItem.setQty(item.getQty());
            copyItem.setSort(item.getSort());
            copyItem.setCreateTime(now);
            bomFormulaItemMapper.insert(copyItem);
        }
        return copy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_DISABLED && status != STATUS_ENABLED)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "status 仅允许 0(停用)/1(启用)");
        }
        getById(id);
        BomFormula update = new BomFormula();
        update.setId(id);
        update.setStatus(status);
        bomFormulaMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        BomFormula formula = getById(id);
        if (formula.getStatus() == STATUS_DISABLED) {
            throw new BizException(ErrorCode.PARAM_ERROR, "已停用的配方不能发布");
        }
        if (formula.getStatus() == STATUS_PUBLISHED) {
            // 幂等：重复发布直接成功
            return;
        }
        BomFormula update = new BomFormula();
        update.setId(id);
        update.setStatus(STATUS_PUBLISHED);
        bomFormulaMapper.updateById(update);
        // TODO F-PRD-01 批次2：进入产品库候选
    }

    @Override
    public ProfitCalcVO calcProfit(ProfitCalcReq req) {
        BomFormula formula = getById(req.getFormulaId());
        List<BomFormulaItem> items = listItems(formula.getId());

        Map<Long, BomMaterial> materialMap = items.isEmpty() ? Map.of()
                : bomMaterialMapper.selectBatchIds(items.stream()
                        .map(BomFormulaItem::getMaterialId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(BomMaterial::getId, Function.identity()));

        // 逐项：qty × costPrice → BigDecimal 乘后 HALF_UP 取整分再累加；
        // 原料已停用或缺失的明细项跳过（试算是估算，不中断）
        int costTotal = 0;
        for (BomFormulaItem item : items) {
            BomMaterial material = materialMap.get(item.getMaterialId());
            if (material == null || material.getCostPrice() == null
                    || material.getStatus() == null || material.getStatus() != STATUS_ENABLED) {
                log.warn("毛利试算跳过明细: formulaId={}, materialId={}（原料缺失或已停用）",
                        formula.getId(), item.getMaterialId());
                continue;
            }
            BigDecimal itemCost = item.getQty()
                    .multiply(BigDecimal.valueOf(material.getCostPrice()))
                    .setScale(0, RoundingMode.HALF_UP);
            costTotal += itemCost.intValueExact();
        }

        int suggestPrice = req.getSuggestPrice();
        int grossProfit = suggestPrice - costTotal;
        int grossMargin = 0;
        if (suggestPrice > 0) {
            grossMargin = BigDecimal.valueOf(grossProfit)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(suggestPrice), 0, RoundingMode.HALF_UP)
                    .intValueExact();
        }

        // 试算结果回写 bom_formula.cost_total（单字段 update，无事务破坏性）
        BomFormula update = new BomFormula();
        update.setId(formula.getId());
        update.setCostTotal(costTotal);
        bomFormulaMapper.updateById(update);

        ProfitCalcVO vo = new ProfitCalcVO();
        vo.setCostTotal(costTotal);
        vo.setSuggestPrice(suggestPrice);
        vo.setGrossProfit(grossProfit);
        vo.setGrossMargin(grossMargin);
        return vo;
    }

    private BomFormula getById(Long id) {
        BomFormula formula = bomFormulaMapper.selectById(id);
        if (formula == null) {
            throw new BizException(ErrorCode.FORMULA_NOT_FOUND);
        }
        return formula;
    }

    private List<BomFormulaItem> listItems(Long formulaId) {
        // 租户插件自动拼 tenant_id
        return bomFormulaItemMapper.selectList(new LambdaQueryWrapper<BomFormulaItem>()
                .eq(BomFormulaItem::getFormulaId, formulaId)
                .orderByAsc(BomFormulaItem::getSort));
    }

    /**
     * 明细"全量重绑"：事务内先 delete by formulaId 再批量 insert；
     * 重复 materialId 由后端去重（LinkedHashMap 保留最后一条，维持原顺序）
     */
    private void rebindItems(Long tenantId, Long formulaId, List<FormulaSaveReq.FormulaItemReq> items) {
        Map<Long, FormulaSaveReq.FormulaItemReq> unique = new LinkedHashMap<>();
        for (FormulaSaveReq.FormulaItemReq item : items) {
            unique.put(item.getMaterialId(), item);
        }
        bomFormulaItemMapper.delete(new LambdaQueryWrapper<BomFormulaItem>()
                .eq(BomFormulaItem::getFormulaId, formulaId));
        LocalDateTime now = LocalDateTime.now();
        List<BomFormulaItem> toInsert = new ArrayList<>(unique.size());
        for (FormulaSaveReq.FormulaItemReq req : unique.values()) {
            BomFormulaItem item = new BomFormulaItem();
            item.setTenantId(tenantId);
            item.setFormulaId(formulaId);
            item.setMaterialId(req.getMaterialId());
            item.setQty(req.getQty());
            item.setSort(req.getSort() == null ? 0 : req.getSort());
            item.setCreateTime(now);
            toInsert.add(item);
        }
        toInsert.forEach(bomFormulaItemMapper::insert);
    }

    /**
     * formulaCode 租户内唯一校验（租户插件自动过滤 tenant_id）
     *
     * @param excludeId 编辑时排除自身，新增传 null
     */
    private void checkCodeUnique(String formulaCode, Long excludeId) {
        LambdaQueryWrapper<BomFormula> wrapper = new LambdaQueryWrapper<BomFormula>()
                .eq(BomFormula::getFormulaCode, formulaCode)
                .ne(excludeId != null, BomFormula::getId, excludeId);
        if (bomFormulaMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "配方编码已存在");
        }
    }

    /** formula_code 列宽 VARCHAR(32) */
    private static final int CODE_MAX_LEN = 32;

    /**
     * 复制编码递推：原编码-C → 已存在则 -C2、-C3 … 直到不重复；
     * 源编码过长时先截断再拼后缀，保证不超过列宽 32
     */
    private String nextCopyCode(String sourceCode) {
        String suffix = "-C";
        int seq = 2;
        String candidate = truncate(sourceCode, CODE_MAX_LEN - suffix.length()) + suffix;
        while (codeExists(candidate)) {
            suffix = "-C" + seq;
            candidate = truncate(sourceCode, CODE_MAX_LEN - suffix.length()) + suffix;
            seq++;
        }
        return candidate;
    }

    private String truncate(String value, int maxLen) {
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    private boolean codeExists(String formulaCode) {
        return bomFormulaMapper.selectCount(new LambdaQueryWrapper<BomFormula>()
                .eq(BomFormula::getFormulaCode, formulaCode)) > 0;
    }
}
