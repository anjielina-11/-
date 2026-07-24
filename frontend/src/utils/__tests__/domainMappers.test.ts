import { describe, expect, it } from 'vitest'
import {
  cycleStatusLabel,
  diagnosisProgress,
  diseaseDisplayName,
  markdownToPlainText,
  normalizeReviewStatus,
  parseTags,
  percentToRatio,
  ratioToPercent
} from '../domainMappers'

describe('domain API mappers', () => {
  it('parses JSON/string/array knowledge tags', () => {
    expect(parseTags('["水稻","稻瘟病"]')).toBe('水稻, 稻瘟病')
    expect(parseTags(['玉米', '虫害'])).toBe('玉米, 虫害')
    expect(parseTags('水稻, 病害')).toBe('水稻, 病害')
  })

  it('normalizes diagnosis review states', () => {
    expect(normalizeReviewStatus('pending_review')).toBe('pending')
    expect(normalizeReviewStatus('pending')).toBe('pending')
    expect(normalizeReviewStatus('approved')).toBe('approved')
    expect(normalizeReviewStatus('rejected')).toBe('rejected')
    expect(normalizeReviewStatus('failed')).toBe('failed')
  })

  it('maps planting-cycle states and diagnosis progress', () => {
    expect(cycleStatusLabel('active')).toBe('生长中')
    expect(cycleStatusLabel('pending_harvest')).toBe('待收获')
    expect(cycleStatusLabel('completed')).toBe('已收获')
    expect(diagnosisProgress('processing')).toBe(60)
    expect(diagnosisProgress('need_review')).toBe(100)
  })

  it('converts model metrics between ratios and percentages', () => {
    expect(ratioToPercent(0.956)).toBe(95.6)
    expect(ratioToPercent(95.6)).toBe(95.6)
    expect(percentToRatio(95.6)).toBe(0.956)
  })

  it('converts model class codes to Chinese disease names', () => {
    expect(diseaseDisplayName('rice_blast')).toBe('水稻稻瘟病')
    expect(diseaseDisplayName('unknown_code')).toBe('unknown_code')
    expect(diseaseDisplayName()).toBe('未知病害')
  })

  it('renders treatment markdown as readable plain text', () => {
    expect(markdownToPlainText('## 病害分析\n- 清除病叶\n- 合理施肥\n\n---\n\n### 参考来源'))
      .toBe('病害分析\n• 清除病叶\n• 合理施肥\n\n参考来源')
  })
})
