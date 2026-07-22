<script setup lang="ts">
import { useSummaryStore } from '@/stores/useSummaryStore'

const summaryStore = useSummaryStore()

function formatAuthors(authors: string[]): string {
  if (authors.length <= 3) return authors.join(', ')
  return authors.slice(0, 3).join(', ') + '...'
}

function openPubMed(pmid: string) {
  window.open(`https://pubmed.ncbi.nlm.nih.gov/${pmid}/`, '_blank')
}

function handleRetry(pmid: string) {
  summaryStore.retrySummary(pmid)
}
</script>

<template>
  <section class="summary-section" v-if="summaryStore.summaries.length > 0">
    <div class="section-header">
      <h2 class="section-title">文献详情</h2>
      <p class="section-desc">MedSearch已为您检索的文献生成专业总结</p>
    </div>

    <div class="summary-cards">
      <div
        v-for="item in summaryStore.summaries"
        :key="item.pmid"
        class="summary-card"
      >
        <div class="card-header">
          <h3 class="card-title-cn">{{ item.titleCn }}</h3>
          <p class="card-title-en">{{ item.titleEn }}</p>
          <div class="card-meta">
            <span class="meta-authors">{{ formatAuthors(item.authors) }}</span>
            <span class="meta-divider">|</span>
            <span class="meta-date">{{ item.pubDate }}</span>
            <span class="meta-divider">|</span>
            <span class="meta-pmid">PMID: {{ item.pmid }}</span>
            <button class="btn-link" @click="openPubMed(item.pmid)">查看原文</button>
          </div>
        </div>

        <div v-if="item.status === 'generating'" class="card-loading">
          <div class="loading-spinner"></div>
          <span>正在生成总结...</span>
        </div>

        <div v-else-if="item.status === 'failed' || item.status === 'timeout'" class="card-error">
          <p class="error-text">{{ item.status === 'timeout' ? '生成超时，请重试' : item.errorMsg }}</p>
          <button class="btn btn-retry" @click="handleRetry(item.pmid)">重试</button>
        </div>

        <div v-else-if="item.status === 'success'" class="card-body">
          <div class="insight-block">
            <div class="insight-label">
              <span class="insight-dot dot-findings"></span>
              关键发现
            </div>
            <p class="insight-text">{{ item.keyFindings }}</p>
          </div>
          <div class="insight-block">
            <div class="insight-label">
              <span class="insight-dot dot-trends"></span>
              研究趋势
            </div>
            <p class="insight-text">{{ item.researchTrends }}</p>
          </div>
          <div class="insight-block">
            <div class="insight-label">
              <span class="insight-dot dot-clinical"></span>
              临床意义
            </div>
            <p class="insight-text">{{ item.clinicalSignificance }}</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.summary-section {
  background: var(--color-white);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border);
  padding: 28px;
  box-shadow: var(--shadow-sm);
}

.section-header {
  margin-bottom: 20px;
}

.section-title {
  font-family: 'Plus Jakarta Sans', 'PingFang SC', 'Microsoft YaHei UI', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-secondary);
  margin-bottom: 6px;
  letter-spacing: -0.01em;
}

.section-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.summary-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.summary-card:hover {
  box-shadow: var(--shadow-md);
}

.card-header {
  padding: 18px 20px;
  background: linear-gradient(135deg, #F8FAFC, #F1F5F9);
  border-bottom: 1px solid var(--color-border);
}

.card-title-cn {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-secondary);
  line-height: 1.5;
  margin-bottom: 4px;
}

.card-title-en {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin-bottom: 8px;
  font-style: italic;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--color-text-muted);
}

.meta-divider {
  color: #E2E8F0;
}

.btn-link {
  color: #0052FF;
  font-size: 12px;
  font-weight: 600;
  background: none;
  padding: 0;
  transition: color 0.15s;
}

.btn-link:hover {
  color: #003EB3;
  text-decoration: underline;
}

.card-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 32px 20px;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid #E2E8F0;
  border-top-color: #0052FF;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.card-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 24px 20px;
  text-align: center;
}

.error-text {
  font-size: 13px;
  color: var(--color-error);
}

.btn-retry {
  padding: 6px 16px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  background: var(--color-bg);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  transition: all 0.15s;
}

.btn-retry:hover {
  border-color: #0052FF;
  color: #0052FF;
  background: #F0F4FF;
}

.card-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.insight-block {
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: var(--color-bg);
  border: 1px solid #F1F5F9;
}

.insight-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 8px;
}

.insight-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-findings {
  background: #0052FF;
}

.dot-trends {
  background: #8B5CF6;
}

.dot-clinical {
  background: #059669;
}

.insight-text {
  font-size: 14px;
  color: var(--color-text);
  line-height: 1.7;
}

@media (max-width: 768px) {
  .summary-section {
    padding: 20px 16px;
    border-radius: var(--radius-lg);
  }

  .card-header {
    padding: 14px 16px;
  }

  .card-body {
    padding: 16px;
  }

  .card-meta {
    font-size: 11px;
  }
}
</style>
