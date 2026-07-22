'use client'

import { useState, useEffect } from 'react'
import { useApp } from '@/components/AppProvider'
import OrderCard from '@/components/OrderCard'
import Link from 'next/link'

export default function OrdersPage() {
  const { phone, logout } = useApp()
  const [orders, setOrders] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!phone) {
      setLoading(false)
      return
    }
    fetch(`/api/orders?phone=${phone}`)
      .then((r) => r.json())
      .then((data) => setOrders(Array.isArray(data) ? data : []))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [phone])

  if (!phone) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] px-6">
        <div className="text-6xl mb-4">👤</div>
        <p className="text-gray-400 mb-6">请先登录查看订单</p>
        <Link
          href="/login"
          className="px-8 py-3 bg-green-800 text-white rounded-full font-medium hover:bg-green-700 transition-colors"
        >
          去登录
        </Link>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="bg-green-900 text-white px-4 pt-6 pb-4 md:rounded-b-3xl">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">我的订单</h1>
            <p className="text-green-200 text-sm mt-1">{phone}</p>
          </div>
          <button
            onClick={logout}
            className="text-sm text-green-200 hover:text-white border border-green-400/50 rounded-lg px-3 py-1.5 transition-colors"
          >
            退出登录
          </button>
        </div>
      </div>

      <div className="p-4 space-y-3">
        {loading ? (
          <div className="text-center py-20 text-gray-400 animate-pulse">加载中...</div>
        ) : orders.length === 0 ? (
          <div className="text-center py-20">
            <div className="text-6xl mb-4">📭</div>
            <p className="text-gray-400 mb-6">暂无订单</p>
            <Link
              href="/menu"
              className="px-6 py-2.5 bg-green-800 text-white rounded-full font-medium hover:bg-green-700 transition-colors"
            >
              去点单
            </Link>
          </div>
        ) : (
          orders.map((order) => <OrderCard key={order.id} order={order} />)
        )}
      </div>
    </div>
  )
}
