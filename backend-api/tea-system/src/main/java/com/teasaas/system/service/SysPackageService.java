package com.teasaas.system.service;

import com.teasaas.system.vo.SysPackageVO;

import java.util.List;

/**
 * 套餐查询（F-SYS-01，接口需求 2.2；仅 R1；套餐 CRUD 二期细化）
 */
public interface SysPackageService {

    /** 套餐列表（全部，含停用，供品牌绑定下拉与套餐页展示） */
    List<SysPackageVO> list();
}
