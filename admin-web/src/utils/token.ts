/**
 * token 持久化（localStorage；与 Redis Key 前缀规范保持一致风格）
 * 独立模块、零依赖：request.ts 与 stores/auth.ts 共同引用，避免循环依赖
 */
const TOKEN_KEY = 'teasaas:token'
const TOKEN_NAME_KEY = 'teasaas:token-name'
/** 后端默认 tokenName（LoginVO 会带回实际值，登录后覆盖） */
const DEFAULT_TOKEN_NAME = 'satoken'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function getTokenName(): string {
  return localStorage.getItem(TOKEN_NAME_KEY) || DEFAULT_TOKEN_NAME
}

export function setToken(token: string, tokenName?: string): void {
  localStorage.setItem(TOKEN_KEY, token)
  if (tokenName) {
    localStorage.setItem(TOKEN_NAME_KEY, tokenName)
  }
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_NAME_KEY)
}
