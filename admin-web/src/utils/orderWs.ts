/**
 * 订单 WS 客户端（/ws/orders，接口需求 8.3）
 *
 * <p>连接协议：token 经 query 传入（浏览器 WebSocket API 无法自定义请求头，
 * vite 代理 /ws → 8080）；30s 心跳（客户端发文本帧 "PING"，服务端回 "PONG"）；
 * 断线指数退避重连 1s→2s→…→30s 封顶，每次重连重读 token（登出后自然停止）。
 * 推送是尽力而为：消息丢失由页面查询按钮/切店重查兜底。</p>
 */
import type { WsOrderMessage } from '@/types/order'
import { getToken } from '@/utils/token'

const WS_URL = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/orders`
const HEARTBEAT_MS = 30_000
const MAX_RETRY_DELAY_MS = 30_000

export type WsStatus = 'connecting' | 'open' | 'closed' | 'denied'

export interface OrderWsOptions {
  onMessage: (msg: WsOrderMessage) => void
  onStatus?: (status: WsStatus) => void
}

export function createOrderWs(options: OrderWsOptions) {
  let ws: WebSocket | null = null
  let heartbeatTimer: number | undefined
  let retryTimer: number | undefined
  let retryCount = 0
  let stopped = false

  function setStatus(status: WsStatus) {
    options.onStatus?.(status)
  }

  function clearTimers() {
    if (heartbeatTimer !== undefined) window.clearInterval(heartbeatTimer)
    if (retryTimer !== undefined) window.clearTimeout(retryTimer)
    heartbeatTimer = undefined
    retryTimer = undefined
  }

  function connect() {
    // 重连时重读：登出清 token 后不再空转重试（denied 终态）
    const token = getToken()
    if (!token) {
      setStatus('denied')
      return
    }
    setStatus('connecting')
    ws = new WebSocket(`${WS_URL}?token=${encodeURIComponent(token)}`)
    ws.onopen = () => {
      retryCount = 0
      setStatus('open')
      heartbeatTimer = window.setInterval(() => {
        if (ws?.readyState === WebSocket.OPEN) ws.send('PING')
      }, HEARTBEAT_MS)
    }
    ws.onmessage = (event) => {
      if (event.data === 'PONG') return
      try {
        options.onMessage(JSON.parse(event.data as string) as WsOrderMessage)
      } catch {
        // 非 JSON 帧忽略
      }
    }
    // 握手 401/403（token 失效或非 R7/R8）在浏览器表现为 1006 断开：
    // 走重连退避（token 失效后路由守卫会踢登录；无权限角色 30s 一次可忽略）
    ws.onclose = () => {
      clearTimers()
      ws = null
      if (stopped) return
      const delay = Math.min(1000 * 2 ** retryCount, MAX_RETRY_DELAY_MS)
      retryCount += 1
      setStatus('closed')
      retryTimer = window.setTimeout(connect, delay)
    }
    ws.onerror = () => {
      // 统一由 onclose 收口
    }
  }

  connect()

  return {
    stop() {
      stopped = true
      clearTimers()
      ws?.close()
      ws = null
      setStatus('closed')
    },
  }
}
