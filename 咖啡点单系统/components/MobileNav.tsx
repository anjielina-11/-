'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useApp } from './AppProvider'

export default function MobileNav() {
  const pathname = usePathname()
  const { cartCount } = useApp()

  const tabs = [
    { href: '/', label: '首页', icon: '🏠' },
    { href: '/menu', label: '菜单', icon: '📋' },
    { href: '/cart', label: '购物车', icon: '🛒', badge: cartCount },
    { href: '/orders', label: '我的', icon: '👤' },
  ]

  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 z-50 safe-bottom">
      <div className="flex items-center justify-around h-16 max-w-lg mx-auto">
        {tabs.map((t) => {
          const active = pathname === t.href || (t.href !== '/' && pathname.startsWith(t.href))
          return (
            <Link
              key={t.href}
              href={t.href}
              className={`flex flex-col items-center justify-center flex-1 h-full text-xs gap-0.5 transition-colors ${
                active ? 'text-green-800 font-semibold' : 'text-gray-500'
              }`}
            >
              <span className="text-xl relative">
                {t.icon}
                {t.badge && t.badge > 0 ? (
                  <span className="absolute -top-1.5 -right-3 bg-red-500 text-white text-[10px] rounded-full min-w-[16px] h-4 flex items-center justify-center px-1">
                    {t.badge > 99 ? '99+' : t.badge}
                  </span>
                ) : null}
              </span>
              <span>{t.label}</span>
            </Link>
          )
        })}
      </div>
    </nav>
  )
}
