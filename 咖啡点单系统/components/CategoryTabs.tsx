'use client'

const CATEGORIES = [
  { key: 'all', label: '全部', icon: '☕' },
  { key: 'coffee', label: '咖啡', icon: '☕' },
  { key: 'tea', label: '茶饮', icon: '🍵' },
  { key: 'pastry', label: '糕点', icon: '🍰' },
  { key: 'special', label: '特调', icon: '✨' },
]

interface Props {
  active: string
  onChange: (key: string) => void
}

export default function CategoryTabs({ active, onChange }: Props) {
  return (
    <div className="flex gap-2 overflow-x-auto hide-scrollbar py-3 px-4">
      {CATEGORIES.map((c) => (
        <button
          key={c.key}
          onClick={() => onChange(c.key)}
          className={`flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium transition-all ${
            active === c.key
              ? 'bg-green-800 text-white shadow-md'
              : 'bg-white text-gray-600 hover:bg-green-50 border border-gray-200'
          }`}
        >
          <span className="mr-1">{c.icon}</span>
          {c.label}
        </button>
      ))}
    </div>
  )
}
