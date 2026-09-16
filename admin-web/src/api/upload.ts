import { http } from '@/utils/request'

/** 文件上传（MinIO，登录态） */
export const uploadApi = {
  /** 上传图片（jpg/jpeg/png/webp/gif，≤5MB），返回永久公开 URL */
  upload: (file: File) => {
    const data = new FormData()
    data.append('file', file)
    return http.upload<string>('/files/upload', data)
  },
}
