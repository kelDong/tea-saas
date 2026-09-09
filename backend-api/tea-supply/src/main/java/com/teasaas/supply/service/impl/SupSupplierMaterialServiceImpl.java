package com.teasaas.supply.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.supply.entity.SupSupplierMaterial;
import com.teasaas.supply.mapper.SupSupplierMaterialMapper;
import com.teasaas.supply.service.SupSupplierMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 供应商可供应原料服务实现（F-SUP-01 内部复用：绑定入口在 SupSupplierService）
 */
@Service
@RequiredArgsConstructor
public class SupSupplierMaterialServiceImpl implements SupSupplierMaterialService {

    private final SupSupplierMaterialMapper supSupplierMaterialMapper;

    @Override
    public List<SupSupplierMaterial> listBySupplierId(Long supplierId) {
        return supSupplierMaterialMapper.selectList(new LambdaQueryWrapper<SupSupplierMaterial>()
                .eq(SupSupplierMaterial::getSupplierId, supplierId)
                .orderByAsc(SupSupplierMaterial::getMaterialId));
    }
}
