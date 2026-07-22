export interface KeywordTag {
  text: string
  source: 'ai' | 'manual'
}

export interface LiteratureItem {
  pmid: string
  titleCn: string
  titleEn: string
  authors: string[]
  pubDate: string
  abstract: string
  abstractSummary: string
  selected: boolean
}

export interface SummaryItem {
  pmid: string
  titleCn: string
  titleEn: string
  authors: string[]
  pubDate: string
  keyFindings: string
  researchTrends: string
  clinicalSignificance: string
  status: 'pending' | 'generating' | 'success' | 'failed' | 'timeout'
  errorMsg: string
}

export interface GenerateKeywordsRequest {
  description: string
}

export interface GenerateKeywordsResponse {
  keywords: string[]
}

export interface TranslateKeywordsRequest {
  keywords: string[]
}

export interface TranslateKeywordsResponse {
  translated_keywords: string[]
}

export interface SearchLiteraturesRequest {
  keywords: string[]
  page?: number
  page_size?: number
}

export interface SearchLiteraturesResponse {
  total_count: number
  filtered_count: number
  page: number
  page_size: number
  total_pages: number
  literatures: LiteratureItem[]
}

export interface GenerateSummaryRequest {
  pmid: string
  title_en: string
  abstract: string
}

export interface GenerateSummaryResponse {
  pmid: string
  key_findings: string
  research_trends: string
  clinical_significance: string
}

export interface ApiErrorResponse {
  error_code: string
  message: string
}
