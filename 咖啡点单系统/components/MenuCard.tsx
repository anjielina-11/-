'use client'

import Link from 'next/link'

interface MenuItem {
  id: number
  name: string
  price: number
  imageUrl: string
  description?: string
  category: string
}

export default function MenuCard({ item }: { item: MenuItem }) {
  return (
    <Link
      href={`/menu/${item.id}`}
      className="block bg-white rounded-2xl shadow-sm hover:shadow-md transition-shadow overflow-hidden group"
    >
      <div className="aspect-square bg-gray-100 relative overflow-hidden">
        {item.imageUrl ? (
          <img
            src={item.imageUrl}
            alt={item.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            loading="lazy"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-4xl">☕</div>
        )}
      </div>
      <div className="p-3">
        <h3 className="font-semibold text-sm text-gray-800 truncate">{item.name}</h3>
        <p className="text-xs text-gray-400 mt-0.5 truncate">{item.description}</p>
        <div className="flex items-center justify-between mt-2">
          <span className="text-amber-600 font-bold">
            ¥<span className="text-lg">{item.price}</span>
          </span>
          <span className="text-[10px] bg-green-100 text-green-700 px-1.5 py-0.5 rounded">+</span>
        </div>
      </div>
    </Link>
  )
}
