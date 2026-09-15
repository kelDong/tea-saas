/** 会员模块类型（F-MEM-01/02，后端 MemberController / MemberConfigController）
 *  注：储值余额/可用积分/等级明细在 C 端 /me 接口，后台一期仅列表 + 积分规则配置 */

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

/** 积分/储值规则（F-MEM-02/03，DTO 与 VO 字段一致） */
export interface PointsConfig {
  /** 消费 1 元得 N 积分 */
  pointsPerYuan: number
  /** 1 积分抵 N 分 */
  pointValue: number
  /** 充值赠送千分比（100=充 1000 送 100） */
  rechargeBonusPermille: number
}
