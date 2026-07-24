export type ReviewUiStatus = 'pending' | 'approved' | 'rejected' | 'failed'

const diseaseNames: Record<string, string> = {
  citrus_canker: '柑橘溃疡病',
  citrus_red_spider: '柑橘红蜘蛛',
  corn_borer: '玉米螟',
  corn_leaf_blight: '玉米大斑病',
  corn_smut: '玉米黑粉病',
  cotton_verticillium: '棉花黄萎病',
  cucumber_downy_mildew: '黄瓜霜霉病',
  cucumber_powdery_mildew: '黄瓜白粉病',
  pepper_anthracnose: '辣椒炭疽病',
  potato_late_blight: '马铃薯晚疫病',
  rice_blast: '水稻稻瘟病',
  rice_sheath_blight: '水稻纹枯病',
  rice_stem_maggot: '水稻秆蝇',
  soybean_pod_borer: '大豆食心虫',
  tomato_gray_mold: '番茄灰霉病',
  tomato_late_blight: '番茄晚疫病',
  wheat_rust: '小麦锈病',
  wheat_scab: '小麦赤霉病'
}

export const diseaseDisplayName = (name?: string | null): string => {
  if (!name) return '未知病害'
  return diseaseNames[name.toLowerCase()] || name
}

export const markdownToPlainText = (value?: string | null): string => {
  if (!value) return ''
  return value
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/^[-*]\s+/gm, '• ')
    .replace(/^---+\s*$/gm, '')
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

export const parseTags = (tags: string | string[] | null | undefined): string => {
  if (Array.isArray(tags)) return tags.join(', ')
  if (!tags) return ''
  try {
    const parsed: unknown = JSON.parse(tags)
    return Array.isArray(parsed) ? parsed.map(String).join(', ') : tags
  } catch {
    return tags
  }
}

export const normalizeReviewStatus = (status?: string | null): ReviewUiStatus => {
  const normalized = status?.toLowerCase()
  if (normalized === 'approved') return 'approved'
  if (normalized === 'rejected') return 'rejected'
  if (normalized === 'failed') return 'failed'
  return 'pending'
}

export const cycleStatusLabel = (status?: string | null): '生长中' | '待收获' | '已收获' => {
  const normalized = status?.toLowerCase()
  if (normalized === 'pending_harvest') return '待收获'
  if (normalized === 'completed' || normalized === 'harvested') return '已收获'
  return '生长中'
}

export const diagnosisProgress = (status?: string | null): number => {
  switch (status?.toLowerCase()) {
    case 'completed':
    case 'need_review':
      return 100
    case 'processing':
      return 60
    case 'pending':
      return 20
    default:
      return 0
  }
}

export const ratioToPercent = (value?: number | string | null): number => {
  const numberValue = Number(value) || 0
  return Number((numberValue <= 1 ? numberValue * 100 : numberValue).toFixed(2))
}

export const percentToRatio = (value?: number | string | null): number => {
  const numberValue = Number(value) || 0
  return Number((numberValue > 1 ? numberValue / 100 : numberValue).toFixed(4))
}
