<script setup lang="ts">
import { useLiteratureStore } from '@/stores/useLiteratureStore'
import { useSummaryStore } from '@/stores/useSummaryStore'

const literatureStore = useLiteratureStore()
const summaryStore = useSummaryStore()

function handleToggle(pmid: string) {
  literatureStore.toggleSelection(pmid)
}

function handleLoadMore() {
  literatureStore.loadMore()
}

function handleGenerateSummary() {
  const selected = literatureStore.literatures.filter((l) => l.selected)
  if (selected.length === 0) return
  summaryStore.generateSummaries(selected)
}
</script>

<template>
  <section class="literature-section" v-if="literatureStore.literatures.length > 0 || literatureStore.isLoading || literatureStore.errorMsg">
    <div class="section-header">
      <h2 class="section-title">文献列表</h2>
      <p class="section-desc" v-if="literatureStore.totalCount > 0">
        检索到 <strong>{{ literatureStore.totalCount }}</strong> 条PubMed医学文献，已为您筛选
        <strong>{{ literatureStore.filteredCount }}</strong> 篇相关文献，请勾选文献，MedSearch将为您生成专业的总结报告。
      </p>
    </div>

    <div v-if="literatureStore.isLoading" class="loading-area">
      <div class="skeleton" v-for="i in 3" :key="i">
        <div class="skeleton-check"></div>
        <div class="skeleton-content">
          <div class="skeleton-title"></div>
          <div class="skeleton-pmid"></div>
        </div>
      </div>
    </div>

    <div v-else-if="literatureStore.errorMsg" class="error-area">
      <div class="error-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
      </div>
      <p class="error-text">{{ literatureStore.errorMsg }}</p>
    </div>

    <div v-else class="literature-list">
      <div
        v-for="(item, index) in literatureStore.literatures"
        :key="item.pmid"
        class="literature-item"
        :class="{ selected: item.selected }"
        @click="handleToggle(item.pmid)"
      >
        <div class="item-index">{{ (literatureStore.currentPage - 1) * 9 + index + 1 }}</div>
        <label class="item-checkbox" @click.stop>
          <input
            type="checkbox"
            :checked="item.selected"
            @change="handleToggle(item.pmid)"
          />
          <span class="checkmark"></span>
        </label>
        <div class="item-content">
          <div class="item-title">{{ item.titleCn || item.titleEn }}</div>
          <div class="item-meta">
            <span class="item-pmid">PMID: {{ item.pmid }}</span>
          </div>
          <div v-if="item.abstractSummary" class="item-summary">{{ item.abstractSummary }}</div>
        </div>
      </div>
    </div>

    <div class="list-footer" v-if="!literatureStore.isLoading && !literatureStore.errorMsg">
      <div class="footer-left">
        已选择 <strong>{{ literatureStore.selectedCount }}</strong> 篇文献
      </div>
      <div class="footer-right">
        当前第{{ literatureStore.currentPage }}页，共{{ literatureStore.totalPages }}页
      </div>
    </div>

    <div class="list-actions" v-if="!literatureStore.isLoading && !literatureStore.errorMsg">
      <button
        class="btn btn-outline"
        :disabled="literatureStore.currentPage >= literatureStore.totalPages"
        @click="handleLoadMore"
      >
        更多文献
      </button>
      <button
        class="btn btn-accent"
        :disabled="literatureStore.selectedCount === 0 || summaryStore.isGenerating"
        @click="handleGenerateSummary"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
        </svg>
        {{ summaryStore.isGenerating ? '正在生成中...' : '生成AI总结' }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.literature-section {
  background: var(--color-white);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border);
  padding: 28px;
  margin-bottom: 20px;
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

.section-desc strong {
  color: #0052FF;
}

.loading-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  border-radius: var(--radius-md);
  background: var(--color-bg);
}

.skeleton-check {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  background: #E2E8F0;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-content {
  flex: 1;
}

.skeleton-title {
  height: 16px;
  width: 70%;
  border-radius: 4px;
  background: #E2E8F0;
  margin-bottom: 8px;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-pmid {
  height: 12px;
  width: 30%;
  border-radius: 4px;
  background: #E2E8F0;
  animation: shimmer 1.5s ease-in-out infinite 0.2s;
}

@keyframes shimmer {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.error-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px;
  text-align: center;
}

.error-icon {
  color: var(--color-text-muted);
  margin-bottom: 12px;
}

.error-text {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.literature-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.literature-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.15s;
}

.literature-item:hover {
  background: #F8FAFC;
}

.literature-item.selected {
  background: #EFF6FF;
  border: 1px solid #DBEAFE;
}

.item-index {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  min-width: 20px;
  padding-top: 3px;
  font-variant-numeric: tabular-nums;
}

.item-checkbox {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding-top: 2px;
}

.item-checkbox input {
  display: none;
}

.checkmark {
  width: 18px;
  height: 18px;
  border: 2px solid #CBD5E1;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  position: relative;
}

.item-checkbox input:checked + .checkmark {
  background: #0052FF;
  border-color: #0052FF;
}

.item-checkbox input:checked + .checkmark::after {
  content: '';
  width: 5px;
  height: 9px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
  margin-top: -1px;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  line-height: 1.5;
  margin-bottom: 4px;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-pmid {
  font-size: 12px;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}

.item-summary {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid #F1F5F9;
}

.list-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  margin-top: 8px;
  border-top: 1px solid var(--color-border);
  font-size: 13px;
  color: var(--color-text-secondary);
}

.footer-left strong {
  color: #0052FF;
}

.list-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  gap: 12px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  transition: all 0.2s ease;
  height: 40px;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-outline {
  background: var(--color-white);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-outline:hover:not(:disabled) {
  border-color: #0052FF;
  color: #0052FF;
  background: #F0F4FF;
}

.btn-accent {
  background: linear-gradient(135deg, #0052FF, #4D7CFF);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 82, 255, 0.25);
}

.btn-accent:hover:not(:disabled) {
  box-shadow: 0 4px 14px rgba(0, 82, 255, 0.35);
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .literature-section {
    padding: 20px 16px;
    border-radius: var(--radius-lg);
  }

  .list-footer {
    flex-direction: column;
    gap: 4px;
    text-align: center;
  }

  .list-actions {
    flex-direction: column;
  }

  .list-actions .btn {
    width: 100%;
  }
}
</style>
