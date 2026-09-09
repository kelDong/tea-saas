package com.teasaas.supply.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.supply.dto.SupplierMaterialBindDTO;
import com.teasaas.supply.dto.SupplierSaveDTO;
import com.teasaas.supply.entity.SupSupplier;
import com.teasaas.supply.entity.SupSupplierMaterial;
import com.teasaas.supply.mapper.SupSupplierMapper;
import com.teasaas.supply.mapper.SupSupplierMaterialMapper;
import com.teasaas.supply.service.SupSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商服务实现（F-SUP-01，接口需求 4.1）
 */
@Service
@RequiredArgsConstructor
public class SupSupplierServiceImpl implements SupSupplierService {

    private final SupSupplierMapper supSupplierMapper;
    private final SupSupplierMaterialMapper supSupplierMaterialMapper;

    @Override
    public PageResult<SupSupplier> page(int page, int size, String keyword) {
        LambdaQueryWrapper<SupSupplier> wrapper = new LambdaQueryWrapper<SupSupplier>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SupSupplier::getSupplierName, keyword)
                        .or()
                        .like(SupSupplier::getSupplierCode, keyword))
                .orderByDesc(SupSupplier::getId);
        Page<SupSupplier> result = supSupplierMapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SupplierSaveDTO dto) {
        checkCodeUnique(dto.getSupplierCode(), null);
        SupSupplier entity = new SupSupplier();
        entity.setSupplierCode(dto.getSupplierCode());
        entity.setSupplierName(dto.getSupplierName());
        entity.setContactName(dto.getContactName());
        entity.setContactPhone(dto.getContactPhone());
        entity.setPaymentTerms(dto.getPaymentTerms());
        entity.setStatus(1);
        supSupplierMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SupplierSaveDTO dto) {
        SupSupplier exist = requireExists(id);
        checkCodeUnique(dto.getSupplierCode(), exist.getId());
        exist.setSupplierCode(dto.getSupplierCode());
        exist.setSupplierName(dto.getSupplierName());
        exist.setContactName(dto.getContactName());
        exist.setContactPhone(dto.getContactPhone());
        exist.setPaymentTerms(dto.getPaymentTerms());
        supSupplierMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindMaterials(Long id, SupplierMaterialBindDTO dto) {
        requireExists(id);
        // 全量重绑（幂等）：先按 supplierId 物理删除旧关联，再批量插入新关联
        supSupplierMaterialMapper.delete(new LambdaQueryWrapper<SupSupplierMaterial>()
                .eq(SupSupplierMaterial::getSupplierId, id));
        Long tenantId = com.teasaas.framework.security.context.SecurityUtils.getTenantIdFromSession();
        LocalDateTime now = LocalDateTime.now();
        for (var item : dto.getItems()) {
            SupSupplierMaterial rel = new SupSupplierMaterial();
            rel.setTenantId(tenantId);
            rel.setSupplierId(id);
            rel.setMaterialId(item.getMaterialId());
            rel.setSupplyPrice(item.getSupplyPrice());
            rel.setCreateTime(now);
            supSupplierMaterialMapper.insert(rel);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        requireExists(id);
        SupSupplier entity = new SupSupplier();
        entity.setId(id);
        entity.setStatus(status);
        supSupplierMapper.updateById(entity);
    }

    @Override
    public List<SupSupplierMaterial> listMaterials(Long supplierId) {
        return supSupplierMaterialMapper.selectList(new LambdaQueryWrapper<SupSupplierMaterial>()
                .eq(SupSupplierMaterial::getSupplierId, supplierId)
                .orderByAsc(SupSupplierMaterial::getMaterialId));
    }

    private SupSupplier requireExists(Long id) {
        // 租户插件自动拼 tenant_id 防跨租户访问
        SupSupplier entity = supSupplierMapper.selectById(id);
        if (entity == null) {
            // 建议：协调者补充 SUPPLIER_NOT_FOUND(60103) 后替换此处 PARAM_ERROR
            throw new BizException(ErrorCode.PARAM_ERROR, "供应商不存在");
        }
        return entity;
    }

    private void checkCodeUnique(String supplierCode, Long excludeId) {
        Long count = supSupplierMapper.selectCount(new LambdaQueryWrapper<SupSupplier>()
                .eq(SupSupplier::getSupplierCode, supplierCode)
                .ne(excludeId != null, SupSupplier::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "供应商编码已存在");
        }
    }
}
