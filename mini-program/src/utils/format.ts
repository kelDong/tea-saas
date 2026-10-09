/**
 * 展示格式化工具（对齐 admin-web src/utils/format.ts 的金额口径）
 *
 * 红线 1（前端版）：传输层金额一律 number「分」，仅展示时转元。
 */

/** 分 → "¥35.00"（空值返回 "—"） */
export function fen2yuan(fen?: number | null): string {
  if (fen === undefined || fen === null) return '—';
  return `¥${(fen / 100).toFixed(2)}`;
}

/** 分 → 数字元（用于需要再运算的场景） */
export function fen2yuanNum(fen?: number | null): number {
  return fen === undefined || fen === null ? 0 : fen / 100;
}

/** "2026-10-09T14:32:01" → "2026-10-09 14:32" */
export function formatDateTime(v?: string | null): string {
  if (!v) return '—';
  return v.replace('T', ' ').slice(0, 16);
}

/** "2026-10-09T14:32:01" → "10-09 14:32"（列表卡片紧凑展示） */
export function formatDateTimeShort(v?: string | null): string {
  if (!v) return '—';
  return v.replace('T', ' ').slice(5, 16);
}

/** 米 → "350m" / "1.2km" */
export function formatDistance(m?: number | null): string {
  if (m === undefined || m === null) return '';
  return m < 1000 ? `${m}m` : `${(m / 1000).toFixed(1)}km`;
}

/** 订单状态（后端 OrdOrder 状态机 1~7） */
export const ORDER_STATUS: Record<number, { text: string; color: string }> = {
  1: { text: '待支付', color: 'amber' },
  2: { text: '已支付', color: 'green' },
  3: { text: '制作中', color: 'amber' },
  4: { text: '待取餐', color: 'amber' },
  5: { text: '已完成', color: 'green' },
  6: { text: '已取消', color: 'gray' },
  7: { text: '已退款', color: 'red' },
};

export function orderStatusText(status?: number | null): string {
  return (status && ORDER_STATUS[status]?.text) || '未知';
}

/** 支付渠道文案（订单表口径：1微信/2储值/3现金） */
export const PAY_CHANNEL: Record<number, string> = { 1: '微信支付', 2: '储值支付', 3: '现金' };

/**
 * 订单进度 steps：已支付 → 制作中 → 待取餐 → 已完成
 * 返回每步 {label, state: done|now|todo}；待支付/终态单由页面自行处理展示
 */
export function orderSteps(status: number): { label: string; state: 'done' | 'now' | 'todo' }[] {
  const labels = ['已支付', '制作中', '待取餐', '已完成'];
  // status 2→第1步 done；3→当前第2步；4→当前第3步；5→全部 done
  const nowIdx = status === 5 ? 4 : status >= 2 && status <= 4 ? status - 1 : -1;
  return labels.map((label, i) => ({
    label,
    state: i < nowIdx ? 'done' : i === nowIdx ? 'now' : 'todo',
  }));
}

/** 券描述：满减「满20减3」/ 折扣「8.5折」（discount 存 DECIMAL(3,2)，0.85=8.5折，服务端直接乘） */
export function couponDesc(c: { type: number; threshold: number; amount: number; discount?: number | null }): string {
  if (c.type === 1) {
    return `满${(c.threshold / 100).toFixed(0)}减${(c.amount / 100).toFixed(0)}`;
  }
  return `${((c.discount ?? 0) * 10).toFixed(1)}折`;
}
