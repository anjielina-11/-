'use client'

interface CartItemData {
  id: number
  menuId: number
  name: string
  imageUrl: string
  cupSize: string
  temperature: string
  quantity: number
  unitPrice: number
}

interface Props {
  item: CartItemData
  onUpdate: (id: number, qty: number) => void
  onDelete: (id: number) => void
}

export default function CartItemRow({ item, onUpdate, onDelete }: Props) {
  const cupLabel = { S: '小杯', M: '中杯', L: '大杯' }[item.cupSize] || item.cupSize
  const tempLabel = item.temperature === 'hot' ? '🔥热饮' : '🧊冰饮'

  return (
    <div className="flex gap-3 py-3 border-b border-gray-100">
      <div className="w-16 h-16 rounded-xl bg-gray-100 overflow-hidden flex-shrink-0">
        {item.imageUrl ? (
          <img src={item.imageUrl} alt={item.name} className="w-full h-full object-cover" />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-2xl">☕</div>
        )}
      </div>

      <div className="flex-1 min-w-0">
        <h4 className="font-medium text-sm truncate">{item.name}</h4>
        <p className="text-xs text-gray-400 mt-0.5">
          {cupLabel} · {tempLabel}
        </p>
        <p className="text-amber-600 font-bold mt-1">¥{item.unitPrice}</p>
      </div>

      <div className="flex flex-col items-end justify-between">
        <button
          onClick={() => onDelete(item.id)}
          className="text-gray-300 hover:text-red-500 text-xs transition-colors"
        >
          ✕
        </button>
        <div className="flex items-center gap-1 bg-gray-50 rounded-lg">
          <button
            onClick={() => onUpdate(item.id, item.quantity - 1)}
            className="w-7 h-7 flex items-center justify-center text-gray-500 hover:text-green-700 font-bold"
          >
            −
          </button>
          <span className="w-6 text-center text-sm font-medium">{item.quantity}</span>
          <button
            onClick={() => onUpdate(item.id, item.quantity + 1)}
            className="w-7 h-7 flex items-center justify-center text-gray-500 hover:text-green-700 font-bold"
          >
            +
          </button>
        </div>
      </div>
    </div>
  )
}
