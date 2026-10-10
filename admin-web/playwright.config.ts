import { defineConfig } from '@playwright/test'

/**
 * S9 UI 冒烟（docs/茶饮SaaS-测试计划.md §4.9）
 * 前置：后端 8080 + Docker tea-mysql/tea-redis 在跑（dev server 代理 /api 与 /ws）
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'off',
  },
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 60_000,
  },
})
