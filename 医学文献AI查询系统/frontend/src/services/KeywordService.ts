import apiClient from './ApiClient'
import type {
  GenerateKeywordsRequest,
  GenerateKeywordsResponse,
  TranslateKeywordsRequest,
  TranslateKeywordsResponse,
} from '@/types'

export async function generateKeywords(description: string): Promise<string[]> {
  const { data } = await apiClient.post<GenerateKeywordsResponse>(
    '/keywords/generate',
    { description } as GenerateKeywordsRequest
  )
  return data.keywords
}

export async function translateKeywords(keywords: string[]): Promise<string[]> {
  const { data } = await apiClient.post<TranslateKeywordsResponse>(
    '/keywords/translate',
    { keywords } as TranslateKeywordsRequest
  )
  return data.translated_keywords
}
