export interface MarketTrendItem {
  cropName: string
  currentPrice: number
  date: string
}

export interface MarketTrendSeries {
  name: string
  data: Array<number | null>
}

export function buildMarketTrendData(items: MarketTrendItem[]): {
  dates: string[]
  series: MarketTrendSeries[]
} {
  const dates = [...new Set(items.map(item => item.date).filter(Boolean))].sort()
  const cropNames = [...new Set(items.map(item => item.cropName).filter(Boolean))]
  const values = new Map(
    items.map(item => [`${item.cropName}\u0000${item.date}`, item.currentPrice] as const)
  )

  return {
    dates,
    series: cropNames.map(name => ({
      name,
      data: dates.map(date => values.get(`${name}\u0000${date}`) ?? null)
    }))
  }
}
