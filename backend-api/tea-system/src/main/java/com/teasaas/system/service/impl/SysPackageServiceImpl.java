package com.teasaas.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.system.entity.SysPackage;
import com.teasaas.system.mapper.SysPackageMapper;
import com.teasaas.system.service.SysPackageService;
import com.teasaas.system.vo.SysPackageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 套餐查询实现（F-SYS-01；CRUD 二期细化，本批仅列表）
 * 平台表 sys_package 不参与租户隔离（MybatisPlusConfig IGNORE_TABLES）
 */
@Service
@RequiredArgsConstructor
public class SysPackageServiceImpl implements SysPackageService {

    private static final String ROLE_R1 = "R1";
    private static final int NOT_DELETED = 0;

    private final SysPackageMapper sysPackageMapper;

    @Override
    public List<SysPackageVO> list() {
        if (!SecurityUtils.getRolesFromSession().contains(ROLE_R1)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅平台管理员可操作");
        }
        return sysPackageMapper.selectList(new LambdaQueryWrapper<SysPackage>()
                        .eq(SysPackage::getDelFlag, NOT_DELETED)
                        .orderByAsc(SysPackage::getPricePerStore))
                .stream().map(this::toVO).toList();
    }

    private SysPackageVO toVO(SysPackage pkg) {
        SysPackageVO vo = new SysPackageVO();
        vo.setId(pkg.getId());
        vo.setPackageCode(pkg.getPackageCode());
        vo.setPackageName(pkg.getPackageName());
        vo.setPricePerStore(pkg.getPricePerStore());
        vo.setStoreLimit(pkg.getStoreLimit());
        vo.setFeatureJson(pkg.getFeatureJson());
        vo.setStatus(pkg.getStatus());
        return vo;
    }
}
