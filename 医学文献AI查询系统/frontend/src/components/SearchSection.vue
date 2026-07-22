<script setup lang="ts">
import { ref } from 'vue'
import { useSearchStore } from '@/stores/useSearchStore'
import { useLiteratureStore } from '@/stores/useLiteratureStore'

const searchStore = useSearchStore()
const literatureStore = useLiteratureStore()

const newKeyword = ref('')

function handleGenerate() {
  searchStore.generateKeywords()
}

function handleAddKeyword() {
  const text = newKeyword.value.trim()
  if (!text) {
    searchStore.errorMsg = '请输入关键词'
    return
  }
  if (searchStore.addKeyword(text, 'manual')) {
    newKeyword.value = ''
  }
}

function handleRemoveKeyword(text: string) {
  searchStore.removeKeyword(text)
}

function handleSearch() {
  if (searchStore.keywords.length === 0) {
    searchStore.errorMsg = '请先生成或添加关键词'
    return
  }
  const keywords = searchStore.keywords.map((k) => k.text)
  literatureStore.searchLiteratures(keywords)
}
</script>

<template>
  <section class="search-section">
    <div class="section-header">
      <h2 class="section-title">MedSearch检索关键词</h2>
      <p class="section-desc">
        输入疾病/病理/研究方向，MedSearch将自动提取核心关键词并构建PubMed检索策略。
        <span class="desc-example">例：肺腺癌的EGFR突变与预后相关性</span>
      </p>
    </div>

    <div class="search-input-area">
      <div class="input-wrapper">
        <textarea
          v-model="searchStore.description"
          class="search-textarea"
          placeholder="请输入疾病描述..."
          maxlength="300"
          rows="2"
        ></textarea>
        <span class="char-count">{{ searchStore.description.length }}/300</span>
      </div>
      <button
        class="btn btn-primary"
        :disabled="searchStore.isGenerating"
        @click="handleGenerate"
      >
        <span v-if="searchStore.isGenerating" class="btn-loading"></span>
        {{ searchStore.isGenerating ? '生成中...' : '生成关键词' }}
      </button>
    </div>

    <div v-if="searchStore.errorMsg" class="error-msg">
      {{ searchStore.errorMsg }}
    </div>

    <div v-if="searchStore.keywords.length > 0" class="keyword-tags">
      <span
        v-for="tag in searchStore.keywords"
        :key="tag.text"
        class="keyword-tag"
        :class="{ 'tag-ai': tag.source === 'ai', 'tag-manual': tag.source === 'manual' }"
      >
        {{ tag.text }}
        <button class="tag-remove" @click="handleRemoveKeyword(tag.text)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </span>
    </div>

    <div class="add-keyword-area">
      <input
        v-model="newKeyword"
        class="keyword-input"
        placeholder="新的关键词"
        @keyup.enter="handleAddKeyword"
      />
      <button class="btn btn-outline" @click="handleAddKeyword">添加</button>
      <button
        class="btn btn-accent"
        :disabled="searchStore.keywords.length === 0 || literatureStore.isLoading"
        @click="handleSearch"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        开始检索文献
      </button>
    </div>
  </section>
</template>

<style scoped>
.search-section {
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

.desc-example {
  color: var(--color-text-muted);
  font-style: italic;
}

.search-input-area {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.input-wrapper {
  flex: 1;
  position: relative;
}

.search-textarea {
  width: 100%;
  min-height: 72px;
  padding: 12px 14px;
  padding-bottom: 28px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text);
  background: var(--color-bg);
  resize: vertical;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.search-textarea:focus {
  border-color: #0052FF;
  box-shadow: 0 0 0 3px rgba(0, 82, 255, 0.1);
  background: var(--color-white);
}

.search-textarea::placeholder {
  color: var(--color-text-muted);
}

.char-count {
  position: absolute;
  right: 10px;
  bottom: 8px;
  font-size: 11px;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
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

.btn-primary {
  background: linear-gradient(135deg, #0052FF, #4D7CFF);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 82, 255, 0.25);
  flex-shrink: 0;
  align-self: flex-end;
}

.btn-primary:hover:not(:disabled) {
  box-shadow: 0 4px 14px rgba(0, 82, 255, 0.35);
  transform: translateY(-1px);
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

.btn-loading {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-msg {
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  background: #FEF2F2;
  color: var(--color-error);
  font-size: 13px;
  border: 1px solid #FECACA;
}

.keyword-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
  padding: 14px;
  background: linear-gradient(135deg, #EFF6FF, #DBEAFE);
  border-radius: var(--radius-md);
  border: 1px solid #BFDBFE;
}

.keyword-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: white;
  background: linear-gradient(135deg, #0052FF, #4D7CFF);
  box-shadow: 0 1px 3px rgba(0, 82, 255, 0.2);
  transition: all 0.2s;
}

.keyword-tag:hover {
  box-shadow: 0 2px 6px rgba(0, 82, 255, 0.3);
}

.tag-manual {
  background: linear-gradient(135deg, #0F766E, #14B8A6);
  box-shadow: 0 1px 3px rgba(20, 184, 166, 0.2);
}

.tag-manual:hover {
  box-shadow: 0 2px 6px rgba(20, 184, 166, 0.3);
}

.tag-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  color: white;
  transition: background 0.2s;
}

.tag-remove:hover {
  background: rgba(255, 255, 255, 0.4);
}

.add-keyword-area {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  align-items: center;
}

.keyword-input {
  flex: 1;
  max-width: 200px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text);
  background: var(--color-bg);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.keyword-input:focus {
  border-color: #0052FF;
  box-shadow: 0 0 0 3px rgba(0, 82, 255, 0.1);
  background: var(--color-white);
}

.keyword-input::placeholder {
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .search-section {
    padding: 20px 16px;
    border-radius: var(--radius-lg);
  }

  .search-input-area {
    flex-direction: column;
  }

  .btn-primary {
    align-self: stretch;
  }

  .add-keyword-area {
    flex-wrap: wrap;
  }

  .keyword-input {
    max-width: none;
  }
}
</style>
