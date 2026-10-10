/**
 * 冒烟统一入口：顺序执行 S1~S8，汇总退出码
 * 用法：node tools/smoke/run-all.mjs [--only=s1,s7] [--base=http://localhost:8080]
 * 环境变量透传：SMOKE_BASE / SMOKE_TENANT / SMOKE_PASSWORD
 */
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const dir = path.dirname(fileURLToPath(import.meta.url))

const SCENARIOS = [
  { id: 's1', file: 's1-b-pos-cash.mjs', name: 'B端POS现金全流程' },
  { id: 's2', file: 's2-b-refund.mjs', name: 'B端退款闭环' },
  { id: 's3', file: 's3-b-cancel.mjs', name: 'B端取消+库存回补' },
  { id: 's4', file: 's4-b-settle-reconcile.mjs', name: '对账与日结回流' },
  { id: 's5', file: 's5-c-order-flow.mjs', name: 'C端下单支付全链路' },
  { id: 's6', file: 's6-c-assets.mjs', name: 'C端资产组合' },
  { id: 's7', file: 's7-ws.mjs', name: 'WS实时推送' },
  { id: 's8', file: 's8-authz-negative.mjs', name: '权限租户负例' },
]

const onlyArg = process.argv.find((a) => a.startsWith('--only='))
const only = onlyArg ? onlyArg.slice(7).split(',').map((s) => s.trim().toLowerCase()) : null
const run = only ? SCENARIOS.filter((s) => only.includes(s.id)) : SCENARIOS
if (run.length === 0) {
  console.error(`--only 未匹配任何场景（可用：${SCENARIOS.map((s) => s.id).join(',')}）`)
  process.exit(2)
}

console.log(`>>> 端到端冒烟（docs/茶饮SaaS-测试计划.md）：${run.map((s) => s.id).join(' → ')}\n`)
const results = []
for (const s of run) {
  console.log(`\n========== [${s.id}] ${s.name} ==========`)
  const r = spawnSync(process.execPath, [path.join(dir, s.file)], {
    stdio: 'inherit',
    env: process.env,
  })
  results.push({ ...s, ok: r.status === 0 })
}

console.log('\n>>> 冒烟汇总')
for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  [${r.id}] ${r.name}`)
const failed = results.filter((r) => !r.ok)
console.log(`\n===== ${results.length - failed.length}/${results.length} 场景 PASS =====`)
process.exit(failed.length ? 1 : 0)
