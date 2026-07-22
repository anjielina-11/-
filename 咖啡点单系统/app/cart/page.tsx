'use client'

import { useState, useEffect, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import CartItemRow from '@/components/CartItemRow'
import { useApp } from '@/components/AppProvider'

interface CartItemData {
  id: number
  menuId: number
  name: string
  imageUrl: string
  cupSize: string
  temperature: string
  quantity: number
  unitPrice: number
}

function getSessionId(): string {
  let sid = sessionStorage.getItem('coffeeshop_sid')
  if (!sid) {
    sid = 'sid_' + Math.random().toString(36).slice(2, 10) + Date.now().toString(36)
    sessionStorage.setItem('coffeeshop_sid', sid)
  }
  return sid
}

export default function CartPage() {
  const [items, setItems] = useState<CartItemData[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [customerName, setCustomerName] = useState('')
  const [showCheckout, setShowCheckout] = useState(false)
  const { phone, setCartCount } = useApp()
  const router = useRouter()

  const loadCart = useCallback(async () => {
    const sid = getSessionId()
    const res = await fetch('/api/cart', { headers: { 'x-session-id': sid } })
    const data = await res.json()
    setItems(data.items || [])
    setCartCount(data.totalCount || 0)
    setLoading(false)
  }, [setCartCount])

  useEffect(() => {
    loadCart()
  }, [loadCart])

  async function updateItem(id: number, qty: number) {
    const sid = getSessionId()
    if (qty <= 0) {
      setItems((prev) => prev.filter((i) => i.id !== id))
    } else {
      setItems((prev) => prev.map((i) => (i.id === id ? { ...i, quantity: qty } : i)))
    }
    await fetch(`/api/cart/items/${id}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', 'x-session-id': sid },
      body: JSON.stringify({ quantity: Math.max(0, qty) }),
    })
    loadCart()
  }

  async function deleteItem(id: number) {
    setItems((prev) => prev.filter((i) => i.id !== id))
    const sid = getSessionId()
    await fetch(`/api/cart/items/${id}`, {
      method: 'DELETE',
      headers: { 'x-session-id': sid },
    })
    loadCart()
  }

  async function clearCart() {
    if (!confirm('确认清空购物车？')) return
    const sid = getSessionId()
    await fetch('/api/cart', { method: 'DELETE', headers: { 'x-session-id': sid } })
    setItems([])
    setCartCount(0)
  }

  async function submitOrder() {
    if (!phone) {
      router.push('/login')
      return
    }
    if (!customerName.trim()) {
      alert('请输入您的姓名')
      return
    }
    setSubmitting(true)
    try {
      const sid = getSessionId()
      const res = await fetch('/api/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-session-id': sid },
        body: JSON.stringify({ customerName: customerName.trim() }),
      })
      if (res.ok) {
        const data = await res.json()
        setItems([])
        setCartCount(0)
        setShowCheckout(false)
        router.push(`/orders/${data.order.id}`)
      } else {
        const err = await res.json()
        alert(err.error || '下单失败')
      }
    } finally {
      setSubmitting(false)
    }
  }

  const totalAmount = items.reduce((s, i) => s + i.unitPrice * i.quantity, 0)

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="text-gray-400 animate-pulse">加载中...</div>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto">
      {/* Header */}
      <div className="bg-green-900 text-white px-4 pt-6 pb-4 md:rounded-b-3xl">
        <h1 className="text-2xl font-bold">购物车</h1>
        <p className="text-green-200 text-sm mt-1">{items.length} 种商品</p>
      </div>

      <div className="p-4">
        {items.length === 0 ? (
          <div className="text-center py-20">
            <div className="text-6xl mb-4">🛒</div>
            <p className="text-gray-400 mb-6">购物车是空的</p>
            <button
              onClick={() => router.push('/menu')}
              className="px-6 py-2.5 bg-green-800 text-white rounded-full font-medium hover:bg-green-700 transition-colors"
            >
              去点单
            </button>
          </div>
        ) : (
          <>
            {/* Items */}
            <div className="bg-white rounded-2xl p-4 shadow-sm mb-4">
              {items.map((item) => (
                <CartItemRow
                  key={item.id}
                  item={item}
                  onUpdate={updateItem}
                  onDelete={deleteItem}
                />
              ))}

              <button
                onClick={clearCart}
                className="mt-3 text-xs text-gray-400 hover:text-red-500 transition-colors"
              >
                清空购物车
              </button>
            </div>

            {/* Total */}
            <div className="bg-white rounded-2xl p-4 shadow-sm mb-4">
              <div className="flex items-center justify-between text-lg font-bold">
                <span>合计</span>
                <span className="text-amber-600">¥{totalAmount.toFixed(2)}</span>
              </div>
            </div>

            {/* Checkout section */}
            {showCheckout ? (
              <div className="bg-white rounded-2xl p-4 shadow-sm mb-4 space-y-3">
                <h3 className="font-semibold text-gray-700">顾客信息</h3>
                <input
                  type="text"
                  value={customerName}
                  onChange={(e) => setCustomerName(e.target.value)}
                  placeholder="请输入您的姓名"
                  className="w-full px-4 py-3 rounded-xl border-2 border-gray-200 focus:border-green-800 focus:outline-none transition-colors"
                />
                <div className="text-sm text-gray-400">
                  手机号：{phone || '未登录'}
                  {!phone && (
                    <button onClick={() => router.push('/login')} className="text-green-700 ml-2 underline">
                      去登录
                    </button>
                  )}
                </div>
                <div className="flex gap-3">
                  <button
                    onClick={() => setShowCheckout(false)}
                    className="flex-1 py-3 border-2 border-gray-200 rounded-xl font-medium text-gray-500 hover:bg-gray-50 transition-colors"
                  >
                    取消
                  </button>
                  <button
                    onClick={submitOrder}
                    disabled={submitting || !phone}
                    className="flex-1 py-3 bg-green-800 text-white rounded-xl font-medium hover:bg-green-700 disabled:opacity-50 transition-colors"
                  >
                    {submitting ? '提交中...' : `提交订单 ¥${totalAmount.toFixed(2)}`}
                  </button>
                </div>
              </div>
            ) : (
              <button
                onClick={() => {
                  if (!phone) {
                    router.push('/login')
                    return
                  }
                  setShowCheckout(true)
                }}
                className="w-full py-4 bg-green-800 text-white rounded-2xl font-bold text-lg hover:bg-green-700 transition-colors shadow-lg"
              >
                去结算 ¥{totalAmount.toFixed(2)}
              </button>
            )}
          </>
        )}
      </div>
    </div>
  )
}
