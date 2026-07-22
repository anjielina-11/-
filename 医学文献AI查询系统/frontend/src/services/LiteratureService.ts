import apiClient from './ApiClient'
import type {
  SearchLiteraturesRequest,
  SearchLiteraturesResponse,
} from '@/types'

export async function searchLiteratures(
  keywords: string[],
  page = 1,
  pageSize = 9
): Promise<SearchLiteraturesResponse> {
  const { data } = await apiClient.post<SearchLiteraturesResponse>(
    '/literatures/search',
    {
      keywords,
      page,
      page_size: pageSize,
    } as SearchLiteraturesRequest
  )
  return data
}
