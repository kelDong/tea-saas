package com.teasaas.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teasaas.member.entity.MemMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 会员 Mapper（生成器骨架）
 */
@Mapper
public interface MemMemberMapper extends BaseMapper<MemMember> {

    /**
     * 会员行悲观锁（Review §3.3 方案 A）。
     * 可用积分=SUM(流水) 无余额字段，先查后写模式下同会员两笔并发单可双双通过余额校验刷负；
     * 积分抵扣（useForOrder）事务开头锁会员行串行化——会员单行热点低，锁冲突可忽略。
     * 必须在事务内调用；租户插件照常拼 tenant_id 隔离。
     */
    @Select("SELECT id FROM mem_member WHERE id = #{id} AND del_flag = 0 FOR UPDATE")
    Long lockById(@Param("id") Long id);
}
