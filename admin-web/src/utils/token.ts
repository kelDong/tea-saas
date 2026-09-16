/**
 * token 持久化（localStorage；与 Redis Key 前缀规范保持一致风格）
 * 独立模块、零依赖：request.ts 与 stores/auth.ts 共同引用，避免循环依赖
 */
const TOKEN_KEY = 'teasaas:token'
const TOKEN_NAME_KEY = 'teasaas:token-name'
/** 展示用昵称（刷新后顶栏不再显示"未命名用户"；随 token 一同清除） */
const NICKNAME_KEY = 'teasaas:nickname'
/** 后端默认 tokenName（LoginVO 会带回实际值，登录后覆盖） */
const DEFAULT_TOKEN_NAME = 'satoken'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function getTokenName(): string {
  return localStorage.getItem(TOKEN_NAME_KEY) || DEFAULT_TOKEN_NAME
}

export function getNickname(): string {
  return localStorage.getItem(NICKNAME_KEY) || ''
}

export function setToken(token: string, tokenName?: string, nickname?: string): void {
  localStorage.setItem(TOKEN_KEY, token)
  if (tokenName) {
    localStorage.setItem(TOKEN_NAME_KEY, tokenName)
  }
  if (nickname !== undefined) {
    localStorage.setItem(NICKNAME_KEY, nickname)
  }
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_NAME_KEY)
  localStorage.removeItem(NICKNAME_KEY)
}
