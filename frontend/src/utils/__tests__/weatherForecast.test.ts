import { describe, expect, it } from 'vitest'
import { selectSevenDayForecast } from '@/utils/weatherForecast'

describe('selectSevenDayForecast', () => {
  it('keeps one record for each date from today through the next six days', () => {
    const result = selectSevenDayForecast([
      { recordedAt: '2026-07-24T10:00:00', temperature: 18 },
      { recordedAt: '2026-07-25T00:00:00', temperature: 20 },
      { recordedAt: '2026-07-25T12:00:00', temperature: 21 },
      { recordedAt: '2026-07-26T00:00:00', temperature: 22 },
      { recordedAt: '2026-07-27T00:00:00', temperature: 23 },
      { recordedAt: '2026-07-28T00:00:00', temperature: 24 },
      { recordedAt: '2026-07-29T00:00:00', temperature: 25 },
      { recordedAt: '2026-07-30T00:00:00', temperature: 26 },
      { recordedAt: '2026-07-31T00:00:00', temperature: 27 },
    ], new Date('2026-07-25T08:00:00+08:00'))

    expect(result).toHaveLength(7)
    expect(result.map(item => item.recordedAt.slice(0, 10))).toEqual([
      '2026-07-25', '2026-07-26', '2026-07-27', '2026-07-28',
      '2026-07-29', '2026-07-30', '2026-07-31'
    ])
    expect(result[0].temperature).toBe(21)
  })
})
