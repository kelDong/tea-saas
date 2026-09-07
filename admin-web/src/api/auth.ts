import { http } from '@/utils/request'
import type { CaptchaVO, LoginReq, LoginVO, MenusPayload } from '@/types/api'

/** 认证接口（后端 AuthController /api/v1/auth/*，F-SYS-03） */
export const authApi = {
  login: (data: LoginReq) => http.post<LoginVO>('/auth/login', data),
  logout: () => http.post<void>('/auth/logout'),
  captcha: () => http.get<CaptchaVO>('/auth/captcha'),
  menus: () => http.get<MenusPayload>('/auth/menus'),
}
