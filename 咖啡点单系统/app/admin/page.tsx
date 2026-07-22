'use client'

import { useState, useEffect, useCallback } from 'react'

interface OrderData {
  id: number
  customerName: string
  phone: string
  totalAmount: number
  status: string
  createdAt: string
}

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  pending: { label: '待支付', color: 'bg-yellow-100 text-yellow-700' },
  paid: { label: '已支付', color: 'bg-blue-100 text-blue-700' },
  making: { label: '制作中', color: 'bg-orange-100 text-orange-700' },
  done: { label: '已完成', color: 'bg-green-100 text-green-700' },
}

export default function AdminPage() {
  const [orders, setOrders] = useState<OrderData[]>([])
  const [loading, setLoading] = useState(true)

  const loadOrders = useCallback(async () => {
    // Fetch all orders via admin (we get all by not filtering)
    try {
      const res = await fetch('/api/orders?phone=admin&all=true')
      const data = await res.json()
      if (Array.isArray(data)) setOrders(data)
    } catch {
      // Fallback: try with individual phones won't work for all orders
      // So we fetch recent orders differently
    }
    setLoading(false)
  }, [])

  // Since we need all orders for admin, let's load from the SSE-style polling
  useEffect(() => {
    // Fetch all recent orders by brute force approach
    async function fetchAll() {
      try {
        // Use the menu API approach: get all orders
        const res = await fetch('/api/orders?phone=admin&all=true')
        const data = await res.json()
        if (Array.isArray(data) && data.length > 0) {
          setOrders(data)
        } else {
          setOrders([])
        }
      } catch {
        setOrders([])
      }
      setLoading(false)
    }
    fetchAll()
  }, [])

  async function updateStatus(id: number, status: string) {
    await fetch(`/api/orders/${id}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    })
    setOrders((prev) => prev.map((o) => (o.id === id ? { ...o, status } : o)))
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-green-900 text-white px-4 pt-6 pb-4 md:rounded-b-3xl">
        <h1 className="text-2xl font-bold">管理后台</h1>
        <p className="text-green-200 text-sm mt-1">管理所有订单</p>
      </div>

      <div className="p-4">
        {loading ? (
          <div className="text-center py-20 text-gray-400 animate-pulse">加载中...</div>
        ) : orders.length === 0 ? (
          <div className="text-center py-20">
            <div className="text-6xl mb-4">📭</div>
            <p className="text-gray-400">暂无订单</p>
          </div>
        ) : (
          <div className="space-y-3">
            {orders.map((order) => {
              const status = STATUS_MAP[order.status] || STATUS_MAP.pending
              return (
                <div
                  key={order.id}
                  className="bg-white rounded-2xl p-4 shadow-sm"
                >
                  <div className="flex items-center justify-between mb-3">
                    <div>
                      <span className="font-semibold text-gray-700">#{order.id}</span>
                      <span className="text-gray-400 text-xs ml-2">{order.customerName}</span>
                      <span className="text-gray-400 text-xs ml-1">{order.phone}</span>
                    </div>
                    <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${status.color}`}>
                      {status.label}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-sm">
                    <span className="text-amber-600 font-bold">¥{order.totalAmount.toFixed(2)}</span>
                    <span className="text-xs text-gray-400">
                      {new Date(order.createdAt).toLocaleString('zh-CN')}
                    </span>
                  </div>

                  {/* Status actions */}
                  <div className="flex gap-2 mt-3 pt-3 border-t border-gray-100">
                    {['pending', 'paid', 'making', 'done'].map((s) => (
                      <button
                        key={s}
                        onClick={() => updateStatus(order.id, s)}
                        disabled={order.status === s}
                        className={`flex-1 py-1.5 rounded-lg text-xs font-medium transition-all ${
                          order.status === s
                            ? 'bg-green-100 text-green-700 cursor-default'
                            : 'bg-gray-50 text-gray-500 hover:bg-green-50 hover:text-green-700'
                        }`}
                      >
                        {STATUS_MAP[s].label}
                      </button>
                    ))}
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
