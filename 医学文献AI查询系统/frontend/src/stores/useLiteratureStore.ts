import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LiteratureItem } from '@/types'
import { searchLiteratures as apiSearchLiteratures } from '@/services/LiteratureService'

export const useLiteratureStore = defineStore('literature', () => {
  const literatures = ref<LiteratureItem[]>([])
  const totalCount = ref(0)
  const filteredCount = ref(0)
  const currentPage = ref(1)
  const totalPages = ref(0)
  const isLoading = ref(false)
  const selectedIds = ref<Set<string>>(new Set())
  const errorMsg = ref('')
  const currentKeywords = ref<string[]>([])

  const selectedCount = computed(() => selectedIds.value.size)

  function toggleSelection(pmid: string) {
    const newSet = new Set(selectedIds.value)
    if (newSet.has(pmid)) {
      newSet.delete(pmid)
    } else {
      newSet.add(pmid)
    }
    selectedIds.value = newSet
    const item = literatures.value.find((l) => l.pmid === pmid)
    if (item) {
      item.selected = !item.selected
    }
  }

  async function searchLiteratures(keywords: string[], page = 1) {
    isLoading.value = true
    errorMsg.value = ''
    currentKeywords.value = keywords
    try {
      const result = await apiSearchLiteratures(keywords, page, 9)
      totalCount.value = result.total_count
      filteredCount.value = result.filtered_count
      currentPage.value = result.page
      totalPages.value = result.total_pages
      literatures.value = result.literatures.map((l) => ({
        pmid: l.pmid,
        titleCn: (l as unknown as Record<string, unknown>).title_cn as string || l.titleCn || '',
        titleEn: (l as unknown as Record<string, unknown>).title_en as string || l.titleEn || '',
        authors: l.authors || [],
        pubDate: (l as unknown as Record<string, unknown>).pub_date as string || l.pubDate || '',
        abstract: (l as unknown as Record<string, unknown>).abstract as string || l.abstract || '',
        abstractSummary: (l as unknown as Record<string, unknown>).abstract_summary as string || l.abstractSummary || '',
        selected: selectedIds.value.has(l.pmid),
      }))
    } catch (e: unknown) {
      const err = e as Error
      errorMsg.value = err.message || '文献检索服务暂时不可用，请稍后重试'
    } finally {
      isLoading.value = false
    }
  }

  async function loadMore() {
    if (currentPage.value < totalPages.value) {
      await searchLiteratures(currentKeywords.value, currentPage.value + 1)
    }
  }

  return {
    literatures,
    totalCount,
    filteredCount,
    currentPage,
    totalPages,
    isLoading,
    selectedIds,
    selectedCount,
    errorMsg,
    currentKeywords,
    toggleSelection,
    searchLiteratures,
    loadMore,
  }
})
