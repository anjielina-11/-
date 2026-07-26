import { describe, expect, it } from 'vitest'
import { localToday, validateFarmArea, validateFieldArea, validatePlantingInput } from '../agriculturalValidation'

describe('agriculturalValidation', () => {
  it('uses local calendar date instead of UTC date', () => {
    expect(localToday(new Date(2026, 6, 26, 1, 30))).toBe('2026-07-26')
  })

  it('rejects invalid farm and field areas', () => {
    expect(() => validateFarmArea(0)).toThrow('\u519c\u573a\u9762\u79ef\u5fc5\u987b\u5927\u4e8e0')
    expect(() => validateFarmArea(9, 10)).toThrow('\u519c\u573a\u9762\u79ef\u4e0d\u80fd\u5c0f\u4e8e\u5df2\u6709\u5730\u5757\u603b\u9762\u79ef')
    expect(() => validateFieldArea(5, 10, 6)).toThrow('\u5730\u5757\u603b\u9762\u79ef\u4e0d\u80fd\u8d85\u8fc7\u519c\u573a\u9762\u79ef')
    expect(() => validateFieldArea(5, 20, 0, 6)).toThrow('\u5730\u5757\u9762\u79ef\u4e0d\u80fd\u5c0f\u4e8e\u5f53\u524d\u672a\u6536\u83b7\u79cd\u690d\u9762\u79ef')
  })

  it('rejects invalid planting dates and field overflow', () => {
    expect(() => validatePlantingInput({ plantingDate: '2026-07-27', expectedHarvestDate: '2026-08-01', areaMu: 1, fieldAreaMu: 10, occupiedAreaMu: 0, today: '2026-07-26' })).toThrow('\u79cd\u690d\u65e5\u671f\u4e0d\u80fd\u665a\u4e8e\u4eca\u5929')
    expect(() => validatePlantingInput({ plantingDate: '2026-07-26', expectedHarvestDate: '2026-07-25', areaMu: 1, fieldAreaMu: 10, occupiedAreaMu: 0, today: '2026-07-26' })).toThrow('\u9884\u8ba1\u6536\u83b7\u65e5\u671f\u4e0d\u80fd\u65e9\u4e8e\u79cd\u690d\u65e5\u671f')
    expect(() => validatePlantingInput({ plantingDate: '2026-07-26', expectedHarvestDate: '2026-08-25', areaMu: 5, fieldAreaMu: 10, occupiedAreaMu: 6, today: '2026-07-26' })).toThrow('\u79cd\u690d\u9762\u79ef\u4e0d\u80fd\u8d85\u8fc7\u5730\u5757\u5269\u4f59\u9762\u79ef 4 \u4ea9')
  })

  it('does not count completed planting history against current field capacity', () => {
    expect(validatePlantingInput({ plantingDate: '2026-07-20', expectedHarvestDate: '2026-08-25', areaMu: 20, fieldAreaMu: 10, occupiedAreaMu: 8, today: '2026-07-26', occupiesField: false })).toBe(20)
  })
})
