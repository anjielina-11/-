'use client'

import { useState } from 'react'
import CategoryTabs from '@/components/CategoryTabs'
import MenuCard from '@/components/MenuCard'

interface MenuItem {
  id: number
  name: string
  price: number
  imageUrl: string
  description?: string
  category: string
}

export default function MenuPageClient({
  items,
  categories,
}: {
  items: MenuItem[]
  categories: string[]
}) {
  const [activeCategory, setActiveCategory] = useState('all')

  const filtered = activeCategory === 'all' ? items : items.filter((i) => i.category === activeCategory)

  return (
    <div>
      {/* Hero Header */}
      <div className="bg-green-900 text-white px-4 pt-6 pb-4 md:rounded-b-3xl">
        <h1 className="text-2xl font-bold">菜单</h1>
        <p className="text-green-200 text-sm mt-1">共 {items.length} 款商品可选</p>
      </div>

      <div className="sticky top-0 md:top-16 z-10 bg-amber-50">
        <CategoryTabs active={activeCategory} onChange={setActiveCategory} />
      </div>

      <div className="px-4 pb-8">
        {filtered.length === 0 ? (
          <div className="text-center py-20 text-gray-400">
            <div className="text-4xl mb-3">😔</div>
            <p>该分类暂无商品</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 md:gap-4">
            {filtered.map((item) => (
              <MenuCard key={item.id} item={item} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
