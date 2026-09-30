package com.teasaas.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.framework.tenant.TenantContext;
import com.teasaas.marketing.dto.CouponTemplateDTO;
import com.teasaas.marketing.entity.MktCouponScope;
import com.teasaas.marketing.entity.MktCouponTemplate;
import com.teasaas.marketing.mapper.MktCouponScopeMapper;
import com.teasaas.marketing.mapper.MktCouponTemplateMapper;
import com.teasaas.marketing.service.MktCouponTemplateService;
import com.teasaas.marketing.vo.CouponTemplateDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 券模板服务实现（F-MKT-01，接口需求 11.1）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MktCouponTemplateServiceImpl implements MktCouponTemplateService {

    private static final String ROLE_R2 = "R2";
    private static final String ROLE_R5 = "R5";
    private static final String ROLE_R6 = "R6";

    private static final int TYPE_FULL_CUT = 1;
    private static final int VALID_TYPE_FIXED = 1;

    private static final int SCOPE_ALL = 1;
    private static final int SCOPE_PRODUCT = 2;

    private final MktCouponTemplateMapper mktCouponTemplateMapper;
    private final MktCouponScopeMapper mktCouponScopeMapper;

    @Override
    public PageResult<MktCouponTemplate> page(int page, int size, Integer status) {
        requireViewRole();
        Page<MktCouponTemplate> result = mktCouponTemplateMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<MktCouponTemplate>()
                        .eq(status != null, MktCouponTemplate::getStatus, status)
                        .orderByDesc(MktCouponTemplate::getId));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public CouponTemplateDetailVO detail(Long id) {
        requireViewRole();
        MktCouponTemplate template = requireTemplate(id);
        CouponTemplateDetailVO vo = new CouponTemplateDetailVO();
        vo.setId(template.getId());
        vo.setName(template.getName());
        vo.setType(template.getType());
        vo.setThreshold(template.getThreshold());
        vo.setAmount(template.getAmount());
        vo.setDiscount(template.getDiscount());
        vo.setTotalQty(template.getTotalQty());
        vo.setPerLimit(template.getPerLimit());
        vo.setValidType(template.getValidType());
        vo.setValidStart(template.getValidStart());
        vo.setValidEnd(template.getValidEnd());
        vo.setValidDays(template.getValidDays());
        vo.setStatus(template.getStatus());
        List<CouponTemplateDetailVO.ScopeNode> scopeNodes =
                mktCouponScopeMapper.selectScopeWithProduct(id);
        vo.setScopeType(scopeNodes.isEmpty() ? SCOPE_ALL : SCOPE_PRODUCT);
        vo.setScope(scopeNodes);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MktCouponTemplate create(CouponTemplateDTO dto) {
        requireEditRole();
        validateRuleFields(dto, true);
        MktCouponTemplate template = new MktCouponTemplate();
        applyFields(template, dto);
        template.setTenantId(TenantContext.getTenantId());
        template.setCreateBy(SecurityUtils.getUserId());
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        mktCouponTemplateMapper.insert(template);
        saveScope(template.getId(), dto);
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CouponTemplateDTO dto) {
        requireEditRole();
        MktCouponTemplate template = requireTemplate(id);
        validateRuleFields(dto, false);
        applyFields(template, dto);
        template.setUpdateTime(LocalDateTime.now());
        mktCouponTemplateMapper.updateById(template);
        saveScope(id, dto);
    }

    // ==================== 私有工具 ====================

    /**
     * 落适用范围行：先按模板全删再按需插入——scopeType=1 只删不插
     * （无行=全场，存量模板天然兼容）；scopeType=2 必填非空商品列表。
     * 表无 create_time/del_flag，全量重建语义（勿改逻辑删）。
     */
    private void saveScope(Long templateId, CouponTemplateDTO dto) {
        mktCouponScopeMapper.delete(new LambdaQueryWrapper<MktCouponScope>()
                .eq(MktCouponScope::getCouponId, templateId));
        if (dto.getScopeType() == null || dto.getScopeType() == SCOPE_ALL) {
            return;
        }
        if (dto.getProductIds() == null || dto.getProductIds().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "指定商品券必须选择商品");
        }
        Long tenantId = TenantContext.getTenantId();
        for (Long productId : dto.getProductIds().stream().distinct().toList()) {
            MktCouponScope row = new MktCouponScope();
            row.setTenantId(tenantId);
            row.setCouponId(templateId);
            row.setScopeType(SCOPE_PRODUCT);
            row.setTargetId(productId);
            mktCouponScopeMapper.insert(row);
        }
    }

    private MktCouponTemplate requireTemplate(Long id) {
        MktCouponTemplate template = mktCouponTemplateMapper.selectById(id);
        if (template == null) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        return template;
    }

    /** 规则字段交叉校验：type 决定 amount/discount，validType 决定时段/天数 */
    private void validateRuleFields(CouponTemplateDTO dto, boolean forCreate) {
        if (dto.getType() == TYPE_FULL_CUT && dto.getAmount() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "满减券必须配置减免金额");
        }
        if (dto.getType() != TYPE_FULL_CUT && dto.getDiscount() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "折扣券必须配置折扣率");
        }
        if (dto.getValidType() == VALID_TYPE_FIXED
                && (dto.getValidStart() == null || dto.getValidEnd() == null
                || !dto.getValidStart().isBefore(dto.getValidEnd()))) {
            throw new BizException(ErrorCode.PARAM_ERROR, "固定时段有效期起止不合法");
        }
        if (dto.getValidType() != VALID_TYPE_FIXED && dto.getValidDays() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "领取后生效必须配置有效天数");
        }
        if (forCreate && dto.getType() == TYPE_FULL_CUT && dto.getThreshold() != null
                && dto.getAmount() != null && dto.getAmount() >= dto.getThreshold() && dto.getThreshold() > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "减免金额应小于门槛金额");
        }
    }

    private void applyFields(MktCouponTemplate template, CouponTemplateDTO dto) {
        template.setName(dto.getName());
        template.setType(dto.getType());
        template.setThreshold(dto.getThreshold());
        template.setAmount(dto.getAmount());
        template.setDiscount(dto.getDiscount());
        template.setTotalQty(dto.getTotalQty() == null ? -1 : dto.getTotalQty());
        template.setPerLimit(dto.getPerLimit() == null ? 1 : dto.getPerLimit());
        template.setValidType(dto.getValidType());
        template.setValidStart(dto.getValidStart());
        template.setValidEnd(dto.getValidEnd());
        template.setValidDays(dto.getValidDays());
        template.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    /** 列表（11.1）：R2/R5/R6 */
    private void requireViewRole() {
        List<String> roles = SecurityUtils.getRolesFromSession();
        if (!roles.contains(ROLE_R2) && !roles.contains(ROLE_R5) && !roles.contains(ROLE_R6)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部/运营/加盟商可查看");
        }
    }

    /** 创建/编辑（11.1）：R2/R5 */
    private void requireEditRole() {
        List<String> roles = SecurityUtils.getRolesFromSession();
        if (!roles.contains(ROLE_R2) && !roles.contains(ROLE_R5)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅总部/运营可配置券模板");
        }
    }
}
