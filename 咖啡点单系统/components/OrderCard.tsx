'use client'

import Link from 'next/link'

interface OrderData {
  id: number
  customerName: string
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

export default function OrderCard({ order }: { order: OrderData }) {
  const status = STATUS_MAP[order.status] || STATUS_MAP.pending

  return (
    <Link
      href={`/orders/${order.id}`}
      className="block bg-white rounded-2xl p-4 shadow-sm hover:shadow-md transition-shadow"
    >
      <div className="flex items-center justify-between">
        <div>
          <div className="text-sm font-medium text-gray-800">订单 #{order.id}</div>
          <div className="text-xs text-gray-400 mt-0.5">
            {new Date(order.createdAt).toLocaleString('zh-CN')}
          </div>
        </div>
        <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${status.color}`}>
          {status.label}
        </span>
      </div>
      <div className="mt-3 flex items-center justify-between">
        <span className="text-xs text-gray-500">{order.customerName}</span>
        <span className="text-amber-600 font-bold">¥{order.totalAmount.toFixed(2)}</span>
      </div>
    </Link>
  )
}
