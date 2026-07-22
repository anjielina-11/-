'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import SpecSelector from '@/components/SpecSelector'
import { useApp } from '@/components/AppProvider'

interface MenuItem {
  id: number
  name: string
  price: number
  imageUrl: string
  description?: string
  category: string
}

const SIZE_PRICE: Record<string, number> = { S: 0, M: 3, L: 6 }

function getSessionId(): string {
  let sid = sessionStorage.getItem('coffeeshop_sid')
  if (!sid) {
    sid = 'sid_' + Math.random().toString(36).slice(2, 10) + Date.now().toString(36)
    sessionStorage.setItem('coffeeshop_sid', sid)
  }
  return sid
}

export default function MenuDetailClient({ item }: { item: MenuItem }) {
  const [cupSize, setCupSize] = useState('M')
  const [temperature, setTemperature] = useState('hot')
  const [adding, setAdding] = useState(false)
  const [added, setAdded] = useState(false)
  const router = useRouter()
  const { setCartCount } = useApp()

  const unitPrice = item.price + (SIZE_PRICE[cupSize] || 0)
  const cupLabel = { S: '小杯', M: '中杯', L: '大杯' }[cupSize]

  async function addToCart() {
    setAdding(true)
    try {
      const sid = getSessionId()
      const res = await fetch('/api/cart/items', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-session-id': sid },
        body: JSON.stringify({ menuId: item.id, cupSize, temperature }),
      })
      if (res.ok) {
        // Refresh cart count
        const cartRes = await fetch('/api/cart', { headers: { 'x-session-id': sid } })
        const cartData = await cartRes.json()
        setCartCount(cartData.totalCount || 0)
        setAdded(true)
        setTimeout(() => setAdded(false), 1500)
      }
    } finally {
      setAdding(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      {/* Image */}
      <div className="aspect-square md:aspect-[2/1] bg-gray-100 relative md:rounded-b-3xl overflow-hidden">
        {item.imageUrl ? (
          <img src={item.imageUrl} alt={item.name} className="w-full h-full object-cover" />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-8xl">☕</div>
        )}
        <Link
          href="/menu"
          className="absolute top-4 left-4 w-10 h-10 bg-white/80 backdrop-blur rounded-full flex items-center justify-center text-lg shadow"
        >
          ←
        </Link>
      </div>

      <div className="p-4 md:p-6 space-y-6">
        {/* Info */}
        <div>
          <h1 className="text-2xl font-bold text-gray-800">{item.name}</h1>
          <p className="text-gray-400 text-sm mt-1">{item.description}</p>
          <p className="text-3xl font-bold text-amber-600 mt-3">
            ¥{unitPrice}
            {cupSize !== 'S' && (
              <span className="text-sm text-gray-400 font-normal ml-1">({cupLabel})</span>
            )}
          </p>
        </div>

        {/* Spec Selector */}
        <SpecSelector
          cupSize={cupSize}
          temperature={temperature}
          onCupSizeChange={setCupSize}
          onTemperatureChange={setTemperature}
        />

        {/* Add to cart button */}
        <div className="fixed bottom-20 md:static left-0 right-0 px-4 md:px-0 pb-2 md:pb-0 bg-amber-50 md:bg-transparent">
          <button
            onClick={addToCart}
            disabled={adding}
            className={`w-full py-4 rounded-2xl font-bold text-lg transition-all shadow-lg ${
              added
                ? 'bg-green-500 text-white'
                : 'bg-green-800 text-white hover:bg-green-700'
            } disabled:opacity-50`}
          >
            {added ? '✅ 已加入购物车' : adding ? '添加中...' : `加入购物车 - ¥${unitPrice}`}
          </button>
        </div>

        {/* Navigation hint on mobile */}
        <div className="md:hidden flex justify-center gap-4 text-xs text-gray-400 pb-4">
          <Link href="/cart" className="underline">
            查看购物车
          </Link>
          <Link href="/menu" className="underline">
            继续浏览
          </Link>
        </div>
      </div>
    </div>
  )
}
