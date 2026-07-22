import { prisma } from '@/lib/prisma'
import { notFound } from 'next/navigation'
import Link from 'next/link'

export const dynamic = 'force-dynamic'

export default async function OrderDetailPage({ params }: { params: { id: string } }) {
  const order = await prisma.order.findUnique({
    where: { id: parseInt(params.id) },
    include: { items: { include: { menu: true } } },
  })

  if (!order) notFound()

  const STATUS_MAP: Record<string, { label: string; color: string; bg: string }> = {
    pending: { label: '待支付', color: 'text-yellow-600', bg: 'bg-yellow-50 border-yellow-200' },
    paid: { label: '已支付', color: 'text-blue-600', bg: 'bg-blue-50 border-blue-200' },
    making: { label: '制作中', color: 'text-orange-600', bg: 'bg-orange-50 border-orange-200' },
    done: { label: '已完成', color: 'text-green-600', bg: 'bg-green-50 border-green-200' },
  }

  const statusInfo = STATUS_MAP[order.status] || STATUS_MAP.pending
  const cupLabel = (s: string) => ({ S: '小杯', M: '中杯', L: '大杯' }[s] || s)

  return (
    <div className="max-w-2xl mx-auto">
      <div className="bg-green-900 text-white px-4 pt-6 pb-4 md:rounded-b-3xl">
        <Link href="/orders" className="text-green-200 text-sm hover:text-white mb-2 inline-block">
          ← 返回订单列表
        </Link>
        <h1 className="text-2xl font-bold">订单 #{order.id}</h1>
        <p className="text-green-200 text-sm mt-1">
          {new Date(order.createdAt).toLocaleString('zh-CN')}
        </p>
      </div>

      <div className="p-4 space-y-4">
        {/* Status */}
        <div className={`p-4 rounded-2xl border-2 ${statusInfo.bg}`}>
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-gray-600">订单状态</span>
            <span className={`font-bold text-lg ${statusInfo.color}`}>{statusInfo.label}</span>
          </div>
          {/* Progress bar */}
          <div className="mt-3 flex items-center gap-1">
            {['pending', 'paid', 'making', 'done'].map((s, i) => {
              const passed = ['pending', 'paid', 'making', 'done'].indexOf(order.status) >= i
              return (
                <div key={s} className="flex-1 flex flex-col items-center">
                  <div
                    className={`w-3 h-3 rounded-full ${
                      passed ? 'bg-green-600' : 'bg-gray-300'
                    }`}
                  />
                  <span className="text-[10px] mt-1 text-gray-400">
                    {['待支付', '已支付', '制作中', '已完成'][i]}
                  </span>
                </div>
              )
            })}
          </div>
        </div>

        {/* Customer Info */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="font-semibold text-gray-700 mb-2">顾客信息</h3>
          <div className="text-sm text-gray-500 space-y-1">
            <p>姓名：{order.customerName}</p>
            <p>手机：{order.phone}</p>
          </div>
        </div>

        {/* Items */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="font-semibold text-gray-700 mb-3">商品明细</h3>
          <div className="space-y-3">
            {order.items.map((item) => (
              <div key={item.id} className="flex gap-3 py-2 border-b border-gray-50 last:border-0">
                <div className="w-12 h-12 rounded-lg bg-gray-100 flex items-center justify-center text-xl flex-shrink-0">
                  {item.menu?.imageUrl ? (
                    <img
                      src={item.menu.imageUrl}
                      alt=""
                      className="w-full h-full object-cover rounded-lg"
                    />
                  ) : (
                    '☕'
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-medium text-gray-700 truncate">
                    {item.menu?.name || '商品已下架'}
                  </div>
                  <div className="text-xs text-gray-400">
                    {cupLabel(item.cupSize)} · {item.temperature === 'hot' ? '🔥热' : '🧊冰'}
                    × {item.quantity}
                  </div>
                </div>
                <div className="text-sm font-bold text-amber-600">¥{(item.price * item.quantity).toFixed(2)}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Total */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <div className="flex items-center justify-between text-lg font-bold">
            <span>合计</span>
            <span className="text-amber-600">¥{order.totalAmount.toFixed(2)}</span>
          </div>
        </div>
      </div>
    </div>
  )
}
