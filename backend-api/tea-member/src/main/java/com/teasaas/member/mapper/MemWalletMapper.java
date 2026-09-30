package com.teasaas.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teasaas.member.entity.MemWallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 储值账户 Mapper（生成器骨架）
 */
@Mapper
public interface MemWalletMapper extends BaseMapper<MemWallet> {

    /**
     * 会员钱包行悲观锁（并发首开卡兜底重查用）。
     * DuplicateKeyException 后的兜底查询必须用 FOR UPDATE 当前读：
     * REPEATABLE READ 下普通一致性读沿用事务首查的旧快照，
     * 看不到并发方刚提交的钱包行会误判"不存在"（集成测试暴露）；
     * 锁定读返回最新已提交行并加锁，本事务随后即更新该行，无额外开销。
     * 必须在事务内调用；租户插件照常拼 tenant_id 隔离。
     */
    @Select("SELECT id, tenant_id, member_id, balance, total_recharge, total_consume, update_time"
            + " FROM mem_wallet WHERE member_id = #{memberId} FOR UPDATE")
    MemWallet lockByMemberId(@Param("memberId") Long memberId);
}
