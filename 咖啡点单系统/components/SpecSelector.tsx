'use client'

interface Props {
  cupSize: string
  temperature: string
  onCupSizeChange: (s: string) => void
  onTemperatureChange: (t: string) => void
}

const CUP_SIZES = [
  { key: 'S', label: '小杯', price: '¥0' },
  { key: 'M', label: '中杯', price: '+¥3' },
  { key: 'L', label: '大杯', price: '+¥6' },
]

const TEMPERATURES = [
  { key: 'hot', label: '🔥 热饮' },
  { key: 'iced', label: '🧊 冰饮' },
]

export default function SpecSelector({
  cupSize,
  temperature,
  onCupSizeChange,
  onTemperatureChange,
}: Props) {
  return (
    <div className="space-y-4">
      {/* 杯型 */}
      <div>
        <label className="text-sm font-medium text-gray-700 mb-2 block">杯型</label>
        <div className="grid grid-cols-3 gap-2">
          {CUP_SIZES.map((s) => (
            <button
              key={s.key}
              onClick={() => onCupSizeChange(s.key)}
              className={`py-2.5 rounded-xl text-sm font-medium border-2 transition-all ${
                cupSize === s.key
                  ? 'border-green-800 bg-green-50 text-green-800'
                  : 'border-gray-200 bg-white text-gray-600 hover:border-green-300'
              }`}
            >
              <div>{s.label}</div>
              <div className="text-xs opacity-70">{s.price}</div>
            </button>
          ))}
        </div>
      </div>

      {/* 温度 */}
      <div>
        <label className="text-sm font-medium text-gray-700 mb-2 block">温度</label>
        <div className="grid grid-cols-2 gap-2">
          {TEMPERATURES.map((t) => (
            <button
              key={t.key}
              onClick={() => onTemperatureChange(t.key)}
              className={`py-2.5 rounded-xl text-sm font-medium border-2 transition-all ${
                temperature === t.key
                  ? 'border-green-800 bg-green-50 text-green-800'
                  : 'border-gray-200 bg-white text-gray-600 hover:border-green-300'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
