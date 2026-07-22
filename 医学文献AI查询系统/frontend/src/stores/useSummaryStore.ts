import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { SummaryItem, LiteratureItem } from '@/types'
import { generateSummary as apiGenerateSummary } from '@/services/SummaryService'

export const useSummaryStore = defineStore('summary', () => {
  const summaries = ref<SummaryItem[]>([])
  const isGenerating = ref(false)

  async function generateSummaries(selectedLiteratures: LiteratureItem[]) {
    if (selectedLiteratures.length === 0) return

    isGenerating.value = true

    const newSummaries: SummaryItem[] = selectedLiteratures.map((l) => ({
      pmid: l.pmid,
      titleCn: l.titleCn,
      titleEn: l.titleEn,
      authors: l.authors,
      pubDate: l.pubDate,
      keyFindings: '',
      researchTrends: '',
      clinicalSignificance: '',
      status: 'pending' as const,
      errorMsg: '',
    }))

    summaries.value = [...summaries.value, ...newSummaries]

    for (let i = 0; i < newSummaries.length; i++) {
      const item = newSummaries[i]
      const idx = summaries.value.findIndex((s) => s.pmid === item.pmid)
      if (idx === -1) continue

      summaries.value[idx].status = 'generating'

      try {
        const result = await apiGenerateSummary(
          item.pmid,
          item.titleEn,
          selectedLiteratures[i].abstract || selectedLiteratures[i].abstractSummary
        )
        summaries.value[idx].keyFindings = result.key_findings || '暂无数据'
        summaries.value[idx].researchTrends = result.research_trends || '暂无数据'
        summaries.value[idx].clinicalSignificance = result.clinical_significance || '暂无数据'
        summaries.value[idx].status = 'success'
      } catch {
        summaries.value[idx].status = 'failed'
        summaries.value[idx].errorMsg = '生成失败，请重试'
      }
    }

    isGenerating.value = false
  }

  async function retrySummary(pmid: string, abstract: string = '') {
    const idx = summaries.value.findIndex((s) => s.pmid === pmid)
    if (idx === -1) return

    summaries.value[idx].status = 'generating'
    summaries.value[idx].errorMsg = ''

    try {
      const result = await apiGenerateSummary(
        pmid,
        summaries.value[idx].titleEn,
        abstract
      )
      summaries.value[idx].keyFindings = result.key_findings || '暂无数据'
      summaries.value[idx].researchTrends = result.research_trends || '暂无数据'
      summaries.value[idx].clinicalSignificance = result.clinical_significance || '暂无数据'
      summaries.value[idx].status = 'success'
    } catch {
      summaries.value[idx].status = 'failed'
      summaries.value[idx].errorMsg = '生成失败，请重试'
    }
  }

  return {
    summaries,
    isGenerating,
    generateSummaries,
    retrySummary,
  }
})
