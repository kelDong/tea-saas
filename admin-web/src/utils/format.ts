/** 展示格式化工具（红线：金额后端一律分，前端展示时才转元） */

/** 金额分 -> ¥x.xx；空值安全（返回 —） */
export function fen2yuan(fen?: number | null): string {
  if (fen == null) return '—'
  return `¥${(fen / 100).toFixed(2)}`
}

/** 后端 LocalDateTime 序列化串（yyyy-MM-dd HH:mm:ss）截取展示；空值安全 */
export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 19)
}
