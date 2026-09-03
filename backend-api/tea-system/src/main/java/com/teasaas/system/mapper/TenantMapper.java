package com.teasaas.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teasaas.system.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * Tenant Mapper
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}