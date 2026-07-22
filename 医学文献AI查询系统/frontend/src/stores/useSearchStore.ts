import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { KeywordTag } from '@/types'
import { generateKeywords as apiGenerateKeywords } from '@/services/KeywordService'

export const useSearchStore = defineStore('search', () => {
  const description = ref('')
  const keywords = ref<KeywordTag[]>([])
  const isGenerating = ref(false)
  const errorMsg = ref('')

  function addKeyword(text: string, source: 'ai' | 'manual' = 'manual') {
    if (keywords.value.some((k) => k.text === text)) {
      errorMsg.value = '该关键词已存在'
      return false
    }
    if (keywords.value.length >= 5) {
      errorMsg.value = '关键词数量已达上限（5个）'
      return false
    }
    keywords.value.push({ text, source })
    errorMsg.value = ''
    return true
  }

  function removeKeyword(text: string) {
    keywords.value = keywords.value.filter((k) => k.text !== text)
  }

  function clearKeywords() {
    keywords.value = []
  }

  async function generateKeywords() {
    if (description.value.length < 10) {
      errorMsg.value = '请输入至少10个字符的疾病描述'
      return
    }
    isGenerating.value = true
    errorMsg.value = ''
    try {
      const result = await apiGenerateKeywords(description.value)
      const aiKeywords = result.slice(0, 3)
      keywords.value = aiKeywords.map((k) => ({ text: k, source: 'ai' as const }))
    } catch {
      errorMsg.value = '关键词生成服务暂时不可用，请稍后重试'
    } finally {
      isGenerating.value = false
    }
  }

  return {
    description,
    keywords,
    isGenerating,
    errorMsg,
    addKeyword,
    removeKeyword,
    clearKeywords,
    generateKeywords,
  }
})
