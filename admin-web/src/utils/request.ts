import axios from 'axios'
import type { AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

import type { Result } from '@/types/api'
import { clearToken, getToken, getTokenName } from '@/utils/token'

/** Result.code：0 成功 */
const CODE_SUCCESS = 0
/** Result.code：未登录/会话过期（HTTP 401 同步返回） */
const CODE_UNAUTHORIZED = 2

/** 业务错误：携带后端错误码，调用方按需区分处理 */
export class ApiError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const instance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

instance.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.set(getTokenName(), token)
  }
  return config
})

/** 会话过期统一处理：清 token 后硬跳登录页（绕开 Pinia/router，避免循环依赖） */
function redirectToLogin(): void {
  clearToken()
  if (!location.pathname.startsWith('/login')) {
    location.replace('/login')
  }
}

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data as Result
    if (body.code === CODE_SUCCESS) {
      return response
    }
    if (body.code === CODE_UNAUTHORIZED) {
      redirectToLogin()
    } else {
      ElMessage.error(body.message || '请求失败')
    }
    return Promise.reject(new ApiError(body.code, body.message))
  },
  (error) => {
    if (error.response?.status === 401) {
      redirectToLogin()
    } else {
      const message = (error.response?.data as Result | undefined)?.message
      ElMessage.error(message || error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  },
)

/** 解包 Result&lt;T&gt;：成功返回 data，失败 reject（ApiError 或原始错误） */
async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await instance.request<Result<T>>(config)
  return response.data.data
}

export const http = {
  get: <T>(url: string, params?: object) => request<T>({ url, method: 'get', params }),
  post: <T>(url: string, data?: object) => request<T>({ url, method: 'post', data }),
  put: <T>(url: string, data?: object) => request<T>({ url, method: 'put', data }),
  patch: <T>(url: string, data?: object) => request<T>({ url, method: 'patch', data }),
  delete: <T>(url: string) => request<T>({ url, method: 'delete' }),
}
