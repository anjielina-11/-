'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useApp } from './AppProvider'

export default function DesktopNav() {
  const pathname = usePathname()
  const { phone, cartCount, logout } = useApp()

  const links = [
    { href: '/', label: '首页', icon: '🏠' },
    { href: '/menu', label: '菜单', icon: '📋' },
    { href: '/cart', label: '购物车', icon: '🛒' },
    { href: '/orders', label: '我的', icon: '👤' },
  ]

  return (
    <nav className="hidden md:flex fixed top-0 left-0 right-0 h-16 bg-green-900 text-white z-50 items-center justify-between px-8 shadow-lg">
      <Link href="/" className="flex items-center gap-2 text-xl font-bold tracking-wide">
        <span>☕</span>
        <span>CoffeeShop</span>
      </Link>

      <div className="flex items-center gap-1">
        {links.map((l) => {
          const active = pathname === l.href || (l.href !== '/' && pathname.startsWith(l.href))
          return (
            <Link
              key={l.href}
              href={l.href}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                active ? 'bg-amber-500 text-green-900' : 'hover:bg-green-800'
              }`}
            >
              <span className="mr-1">{l.icon}</span>
              {l.label}
              {l.href === '/cart' && cartCount > 0 && (
                <span className="ml-1 bg-red-500 text-white text-xs rounded-full px-1.5 py-0.5">
                  {cartCount}
                </span>
              )}
            </Link>
          )
        })}

        <div className="ml-4 flex items-center gap-2">
          {phone ? (
            <>
              <span className="text-sm text-amber-200">📱 {phone}</span>
              <button
                onClick={logout}
                className="text-sm text-green-300 hover:text-white underline underline-offset-2"
              >
                退出
              </button>
            </>
          ) : (
            <Link href="/login" className="text-sm bg-amber-500 text-green-900 px-3 py-1.5 rounded-lg font-medium">
              登录
            </Link>
          )}
        </div>
      </div>
    </nav>
  )
}
