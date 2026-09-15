package com.teasaas.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.common.result.PageResult;
import com.teasaas.framework.security.context.SecurityUtils;
import com.teasaas.system.dto.TenantCreateReq;
import com.teasaas.system.dto.TenantUpdateReq;
import com.teasaas.system.entity.Tenant;
import com.teasaas.system.mapper.TenantMapper;
import com.teasaas.system.service.TenantService;
import com.teasaas.system.vo.TenantVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 品牌租户管理实现（F-SYS-01）
 * 平台表 tenant 不参与租户隔离（MybatisPlusConfig IGNORE_TABLES，数据库设计 14.2）
 * ——R1 平台管理员专属入口，非 R1 一律拒绝（PRD 5.2：R1 才可管理品牌）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private static final String ROLE_R1 = "R1";
    /** 平台表未继承 BaseEntity、无 @TableLogic，del_flag 手动过滤 */
    private static final int NOT_DELETED = 0;

    private final TenantMapper tenantMapper;

    @Override
    public PageResult<TenantVO> page(int page, int size, String keyword) {
        requirePlatformRole();
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getDelFlag, NOT_DELETED)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Tenant::getTenantCode, keyword)
                        .or().like(Tenant::getTenantName, keyword)
                        .or().like(Tenant::getContactPhone, keyword))
                .orderByDesc(Tenant::getCreateTime);
        Page<Tenant> result = tenantMapper.selectPage(Page.of(page, size), wrapper);
        List<TenantVO> vos = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TenantCreateReq req) {
        requirePlatformRole();
        Long duplicated = tenantMapper.selectCount(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getTenantCode, req.getTenantCode())
                .eq(Tenant::getDelFlag, NOT_DELETED));
        if (duplicated > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "品牌编码已存在");
        }
        Tenant entity = new Tenant();
        entity.setTenantCode(req.getTenantCode());
        entity.setTenantName(req.getTenantName());
        entity.setContactName(req.getContactName());
        entity.setContactPhone(req.getContactPhone());
        entity.setPackageCode(req.getPackageCode());
        entity.setStoreLimit(req.getStoreLimit() == null ? 0 : req.getStoreLimit());
        entity.setExpireTime(req.getExpireTime());
        entity.setStatus(1);
        entity.setDelFlag(NOT_DELETED);
        tenantMapper.insert(entity);
        log.info("tenant {} ({}) created by {}", entity.getTenantCode(), entity.getId(), SecurityUtils.getUserId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TenantUpdateReq req) {
        requirePlatformRole();
        Tenant tenant = requireTenant(id);
        tenant.setTenantName(req.getTenantName());
        tenant.setContactName(req.getContactName());
        tenant.setContactPhone(req.getContactPhone());
        tenant.setPackageCode(req.getPackageCode());
        if (req.getStoreLimit() != null) {
            tenant.setStoreLimit(req.getStoreLimit());
        }
        tenant.setExpireTime(req.getExpireTime());
        tenantMapper.updateById(tenant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requirePlatformRole();
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "status 仅允许 0/1");
        }
        Tenant tenant = requireTenant(id);
        tenant.setStatus(status);
        tenantMapper.updateById(tenant);
        log.info("tenant {} status -> {} by {}", id, status, SecurityUtils.getUserId());
    }

    // ---------------- 内部 ----------------

    /** F-SYS-01 全部接口仅 R1 可操作（PRD 5.2 权限矩阵） */
    private void requirePlatformRole() {
        if (!SecurityUtils.getRolesFromSession().contains(ROLE_R1)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅平台管理员可操作");
        }
    }

    private Tenant requireTenant(Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null || tenant.getDelFlag() != NOT_DELETED) {
            throw new BizException(ErrorCode.TENANT_NOT_FOUND);
        }
        return tenant;
    }

    private TenantVO toVO(Tenant tenant) {
        TenantVO vo = new TenantVO();
        vo.setId(tenant.getId());
        vo.setTenantCode(tenant.getTenantCode());
        vo.setTenantName(tenant.getTenantName());
        vo.setContactName(tenant.getContactName());
        vo.setContactPhone(tenant.getContactPhone());
        vo.setPackageCode(tenant.getPackageCode());
        vo.setStoreLimit(tenant.getStoreLimit());
        vo.setStatus(tenant.getStatus());
        vo.setExpireTime(tenant.getExpireTime());
        vo.setCreateTime(tenant.getCreateTime());
        return vo;
    }
}
