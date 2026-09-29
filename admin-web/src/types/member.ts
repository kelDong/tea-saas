/** 会员模块类型（F-MEM-01/02，后端 MemberController / MemberConfigController）
 *  二期A起后台含资产汇总与积分/储值流水查询；等级规则（mem_member_level）仍二期开放 */

/** 会员（MemMember 实体直出） */
export interface MemMember {
  id: number
  openid: string
  nickname: string
  avatar: string
  phone: string
  levelId: number
  /** 累计获得积分 */
  totalPoints: number
  /** 1正常/0禁用 */
  status: number
  createTime: string
}

/** 会员资产汇总（GET /members/{id}/assets，二期A） */
export interface MemberAssets {
  /** 储值余额(分) */
  balance: number
  /** 累计充值(分，不含赠送) */
  totalRecharge: number
  /** 累计消费(分) */
  totalConsume: number
  /** 可用积分（SUM 流水口径） */
  availablePoints: number
  /** 累计获得积分 */
  totalPoints: number
}

/** 积分流水（MemPointsLog 实体直出） */
export interface PointsLog {
  id: number
  changePoints: number
  beforePoints: number
  afterPoints: number
  /** 1消费得/2抵现扣/3赠送/4过期扣 */
  bizType: number
  bizNo: string
  createTime: string
}

/** 积分流水业务类型映射（对齐后端 MemPointsLog.bizType 注释口径） */
export const POINTS_BIZ_TYPE_MAP: Record<number, string> = {
  1: '消费获得',
  2: '积分抵现',
  3: '赠送',
  4: '过期扣减',
}

/** 储值充值记录（MemRechargeRecord 实体直出；一期钱包流水=充值记录） */
export interface RechargeRecord {
  id: number
  /** 充值金额(分) */
  amount: number
  /** 赠送(分) */
  bonus: number
  /** 充值后余额(分) */
  balanceAfter: number
  bizNo: string
  createTime: string
}

/** 积分/储值规则（F-MEM-02/03，DTO 与 VO 字段一致） */
export interface PointsConfig {
  /** 消费 1 元得 N 积分 */
  pointsPerYuan: number
  /** 1 积分抵 N 分 */
  pointValue: number
  /** 充值赠送千分比（100=充 1000 送 100） */
  rechargeBonusPermille: number
}
