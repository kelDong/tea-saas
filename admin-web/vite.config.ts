import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 后端 tea-server 本地 8080，接口统一 /api/v1 前缀
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 订单实时推送 WS（/ws/orders，接口需求 8.3）——ws: true 升级代理
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
})
