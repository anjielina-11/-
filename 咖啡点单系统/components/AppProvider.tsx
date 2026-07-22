'use client'

import { useState, useEffect, useCallback, createContext, useContext, useRef } from 'react'
import MobileNav from './MobileNav'
import DesktopNav from './DesktopNav'
import CartFab from './CartFab'

interface AuthState {
  phone: string | null
  loading: boolean
}

interface AppContextType {
  phone: string | null
  loading: boolean
  cartCount: number
  setCartCount: (n: number) => void
  login: (phone: string) => Promise<void>
  logout: () => Promise<void>
  sseConnected: boolean
}

export const AppContext = createContext<AppContextType>({
  phone: null,
  loading: true,
  cartCount: 0,
  setCartCount: () => {},
  login: async () => {},
  logout: async () => {},
  sseConnected: false,
})

export function useApp() {
  return useContext(AppContext)
}

// SSE Leader election via BroadcastChannel
const BC_CHANNEL = 'coffeeshop-cart'

function getSessionId(): string {
  if (typeof window === 'undefined') return ''
  let sid = sessionStorage.getItem('coffeeshop_sid')
  if (!sid) {
    sid = 'sid_' + Math.random().toString(36).slice(2, 10) + Date.now().toString(36)
    sessionStorage.setItem('coffeeshop_sid', sid)
  }
  return sid
}

export default function AppProvider({ children }: { children: React.ReactNode }) {
  const [auth, setAuth] = useState<AuthState>({ phone: null, loading: true })
  const [cartCount, setCartCount] = useState(0)
  const [sseConnected, setSseConnected] = useState(false)
  const bcRef = useRef<BroadcastChannel | null>(null)
  const sseRef = useRef<EventSource | null>(null)
  const isLeaderRef = useRef(false)

  // Check login state on mount
  useEffect(() => {
    fetch('/api/auth/me')
      .then((r) => r.json())
      .then((d) => {
        if (d.user) setAuth({ phone: d.user.phone, loading: false })
        else setAuth({ phone: null, loading: false })
      })
      .catch(() => setAuth({ phone: null, loading: false }))
  }, [])

  // BroadcastChannel setup + Leader election
  useEffect(() => {
    if (typeof window === 'undefined') return

    const bc = new BroadcastChannel(BC_CHANNEL)
    bcRef.current = bc

    // Leader election: wait 50ms, if no one claims, become leader
    const leaderTimeout = setTimeout(() => {
      isLeaderRef.current = true
      bc.postMessage({ type: 'claim-leader' })
      setupSSE()
    }, 50)

    bc.onmessage = (e) => {
      const { type, count, phone } = e.data
      if (type === 'claim-leader' && !isLeaderRef.current) {
        // Another tab claimed leader, cancel our claim
        clearTimeout(leaderTimeout)
      }
      if (type === 'cart-sync' && typeof count === 'number') {
        setCartCount(count)
      }
      if (type === 'auth-sync' && phone) {
        setAuth({ phone, loading: false })
      }
      if (type === 'auth-sync' && phone === null) {
        setAuth({ phone: null, loading: false })
      }
    }

    return () => {
      clearTimeout(leaderTimeout)
      sseRef.current?.close()
      bc.close()
    }
  }, [auth.phone])

  function setupSSE() {
    if (typeof window === 'undefined') return
    const sid = getSessionId()
    const es = new EventSource(`/api/cart/sse?sid=${sid}`)
    sseRef.current = es

    es.onopen = () => setSseConnected(true)
    es.onerror = () => {
      setSseConnected(false)
      // Reconnect after 3s
      setTimeout(() => {
        sseRef.current?.close()
        setupSSE()
      }, 3000)
    }

    es.onmessage = (e) => {
      try {
        const data = JSON.parse(e.data)
        if (data.type === 'cart-update' && typeof data.count === 'number') {
          setCartCount(data.count)
          bcRef.current?.postMessage({ type: 'cart-sync', count: data.count })
        }
      } catch (_) {}
    }
  }

  // Load cart count
  useEffect(() => {
    if (auth.loading) return
    const sid = getSessionId()
    fetch('/api/cart', { headers: { 'x-session-id': sid } })
      .then((r) => r.json())
      .then((d) => {
        if (typeof d.totalCount === 'number') setCartCount(d.totalCount)
      })
      .catch(() => {})
  }, [auth.loading, auth.phone])

  const login = useCallback(async (phone: string) => {
    await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone }),
    })
    setAuth({ phone, loading: false })
    // Notify other tabs
    bcRef.current?.postMessage({ type: 'auth-sync', phone })
  }, [])

  const logout = useCallback(async () => {
    await fetch('/api/auth/logout', { method: 'POST' })
    setAuth({ phone: null, loading: false })
    setCartCount(0)
    // Notify other tabs
    bcRef.current?.postMessage({ type: 'auth-sync', phone: null })
  }, [])

  return (
    <AppContext.Provider
      value={{
        phone: auth.phone,
        loading: auth.loading,
        cartCount,
        setCartCount,
        login,
        logout,
        sseConnected,
      }}
    >
      <DesktopNav />
      <main className="pb-20 md:pb-0 md:pt-16 min-h-screen">{children}</main>
      <MobileNav />
      <CartFab />
    </AppContext.Provider>
  )
}
