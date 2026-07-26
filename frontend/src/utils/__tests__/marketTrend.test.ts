import { describe, expect, it } from 'vitest'
import { buildMarketTrendData } from '../marketTrend'

describe('buildMarketTrendData', () => {
  it('builds aligned series for every crop returned by the API', () => {
    const result = buildMarketTrendData([
      { cropName: '水稻', currentPrice: 2.8, date: '2026-07-25' },
      { cropName: '番茄', currentPrice: 5.2, date: '2026-07-25' },
      { cropName: '水稻', currentPrice: 2.9, date: '2026-07-26' },
      { cropName: '玉米', currentPrice: 2.4, date: '2026-07-26' }
    ])

    expect(result.dates).toEqual(['2026-07-25', '2026-07-26'])
    expect(result.series).toEqual([
      { name: '水稻', data: [2.8, 2.9] },
      { name: '番茄', data: [5.2, null] },
      { name: '玉米', data: [null, 2.4] }
    ])
  })

  it('returns an empty chart model when there are no prices', () => {
    expect(buildMarketTrendData([])).toEqual({ dates: [], series: [] })
  })
})
