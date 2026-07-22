import apiClient from './ApiClient'
import type {
  GenerateSummaryRequest,
  GenerateSummaryResponse,
} from '@/types'

export async function generateSummary(
  pmid: string,
  titleEn: string,
  abstract: string
): Promise<GenerateSummaryResponse> {
  const { data } = await apiClient.post<GenerateSummaryResponse>(
    '/summaries/generate',
    {
      pmid,
      title_en: titleEn,
      abstract,
    } as GenerateSummaryRequest
  )
  return data
}
