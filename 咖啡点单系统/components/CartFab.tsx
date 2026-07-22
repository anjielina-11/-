'use client'

import Link from 'next/link'
import { useApp } from './AppProvider'

export default function CartFab() {
  const { cartCount } = useApp()

  if (cartCount === 0) return null

  return (
    <Link
      href="/cart"
      className="md:hidden fixed bottom-20 right-4 z-40 w-14 h-14 bg-green-800 text-white rounded-full shadow-xl flex items-center justify-center animate-bounce"
      style={{ boxShadow: '0 4px 20px rgba(0,80,0,0.4)' }}
    >
      <span className="text-2xl">🛒</span>
      <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full min-w-[20px] h-5 flex items-center justify-center px-1 font-bold">
        {cartCount > 99 ? '99+' : cartCount}
      </span>
    </Link>
  )
}
