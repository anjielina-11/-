import { NextRequest } from 'next/server'
import { getLoginPhone } from '@/lib/auth'
import { getOrCreateCart, getCartItemsCount } from '@/lib/cart'

export const dynamic = 'force-dynamic'

export async function GET(req: NextRequest) {
  const phone = await getLoginPhone()
  const sessionId = req.headers.get('x-session-id') || req.nextUrl.searchParams.get('sid') || 'default'

  const encoder = new TextEncoder()
  let closed = false
  let lastCount = -1

  const stream = new ReadableStream({
    async start(controller) {
      const send = (data: string) => {
        if (!closed) controller.enqueue(encoder.encode(`data: ${data}\n\n`))
      }

      // Heartbeat every 30s
      const heartbeat = setInterval(() => {
        send(JSON.stringify({ type: 'heartbeat' }))
      }, 30000)

      // Poll for cart changes every 1.5s
      const poll = setInterval(async () => {
        try {
          const cart = await getOrCreateCart(phone, sessionId)
          const count = await getCartItemsCount(cart.id)
          if (count !== lastCount) {
            lastCount = count
            send(JSON.stringify({ type: 'cart-update', count }))
          }
        } catch (_) {
          // ignore errors during polling
        }
      }, 1500)

      req.signal.addEventListener('abort', () => {
        closed = true
        clearInterval(heartbeat)
        clearInterval(poll)
        controller.close()
      })
    },
  })

  return new Response(stream, {
    headers: {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      Connection: 'keep-alive',
    },
  })
}
