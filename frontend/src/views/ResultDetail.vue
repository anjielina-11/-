<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElProgress,
  ElAlert,
  ElButton,
  ElEmpty,
  ElMessage,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import { ArrowLeft, WarningFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'
import {
  diseaseDisplayName,
  markdownToPlainText,
  normalizeReviewStatus,
  ratioToPercent
} from '@/utils/domainMappers'

export interface ICitation {
  docTitle: string
  snippet: string
}

export interface IResult {
  diseaseName: string
  confidence: number
  treatment: string
  citations: ICitation[]
  status: 'pending' | 'processing' | 'completed' | 'need_review' | 'failed'
}

const route = useRoute()
const router = useRouter()
const result = ref<IResult | null>(null)
const diagnosisList = ref<Array<{
  id: string
  diseaseName?: string
  confidence?: number
  reviewStatus?: string
  createdAt?: string
}>>([])
const loading = ref(true)
const selectedId = computed(() => route.query.id as string | undefined)

const confidencePercent = computed(() => {
  return result.value ? Math.round(result.value.confidence * 100) : 0
})

const confidenceColor = computed(() => {
  const percent = confidencePercent.value
  if (percent >= 80) return 'var(--color-success)'
  if (percent >= 50) return 'var(--color-warning)'
  return 'var(--color-danger)'
})

const confidenceColorHex = computed(() => {
  const percent = confidencePercent.value
  if (percent >= 80) return '#52C41A'
  if (percent >= 50) return '#FAAD14'
  return '#F5222D'
})

const confidenceText = computed(() => {
  const percent = confidencePercent.value
  if (percent >= 80) return '高'
  if (percent >= 50) return '中'
  return '低'
})

const statusLabel = computed(() => {
  if (!result.value) return ''
  const map: Record<string, string> = {
    completed: '识别完成',
    need_review: '待审核',
    processing: '处理中',
    pending: '待处理',
    failed: '识别失败'
  }
  return map[result.value.status] || ''
})

const statusTagType = computed(() => {
  if (!result.value) return 'info' as const
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    completed: 'success',
    need_review: 'danger',
    processing: 'warning',
    pending: 'info',
    failed: 'danger'
  }
  return map[result.value.status] || 'info'
})

const isNeedReview = computed(() => {
  return result.value?.status === 'need_review'
})

const treatmentText = computed(() => markdownToPlainText(result.value?.treatment))

const reviewStatusLabel = (status?: string) => {
  const labels = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回',
    failed: '识别失败'
  }
  return labels[normalizeReviewStatus(status)]
}

const reviewStatusType = (status?: string) => {
  const types = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger',
    failed: 'info'
  } as const
  return types[normalizeReviewStatus(status)]
}

const loadList = async () => {
  const page = await request.get<{ list: typeof diagnosisList.value; total: number }>('/diagnosis', {
    params: { page: 1, size: 100 }
  })
  diagnosisList.value = page.list
}

const loadDetail = async (diagnosisId: string) => {
  const data = await request.get<{
    status: IResult['status']
    diseaseName?: string
    confidence?: number
    treatment?: string
    citations?: Array<ICitation & { source?: string; content?: string }>
  }>(`/diagnosis/result/${diagnosisId}`)
  result.value = {
    diseaseName: diseaseDisplayName(data.diseaseName),
    confidence: Number(data.confidence) || 0,
    treatment: data.treatment || '暂无防治建议',
    citations: (data.citations || []).map(c => ({
      docTitle: c.docTitle || c.source || '',
      snippet: c.snippet || c.content || ''
    })),
    status: data.status
  }
}

const handleOpen = async (diagnosisId: string) => {
  loading.value = true
  try {
    await router.replace({ query: { id: diagnosisId } })
    await loadDetail(diagnosisId)
  } catch {
    ElMessage.error('获取诊断结果失败')
  } finally {
    loading.value = false
  }
}

const handleBack = async () => {
  loading.value = true
  result.value = null
  try {
    await router.replace({ query: {} })
    await loadList()
  } catch {
    ElMessage.error('获取诊断列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    if (selectedId.value) await loadDetail(selectedId.value)
    else await loadList()
  } catch {
    ElMessage.error(selectedId.value ? '获取诊断结果失败' : '获取诊断列表失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <ElProgress type="circle" :percentage="0" />
    </div>

    <template v-else-if="!selectedId">
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">识别结果查询</h1>
          <p class="page-subtitle">查看历史识别结果、置信度及人工审核状态</p>
        </div>
      </div>

      <div class="result-list-card">
        <ElTable v-if="diagnosisList.length" :data="diagnosisList" stripe>
          <ElTableColumn label="病害名称" min-width="180">
            <template #default="{ row }">
              <span class="disease-list-name">{{ diseaseDisplayName(row.diseaseName) }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="置信度" width="120">
            <template #default="{ row }">{{ ratioToPercent(row.confidence) }}%</template>
          </ElTableColumn>
          <ElTableColumn label="审核状态" width="120">
            <template #default="{ row }">
              <ElTag :type="reviewStatusType(row.reviewStatus)" effect="light">
                {{ reviewStatusLabel(row.reviewStatus) }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="createdAt" label="提交时间" min-width="180" />
          <ElTableColumn label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <ElButton type="primary" link @click="handleOpen(row.id)">查看详情</ElButton>
            </template>
          </ElTableColumn>
        </ElTable>
        <ElEmpty v-else description="暂无识别记录" />
      </div>
    </template>

    <template v-else-if="result">
      <!-- 待审核提示横幅 -->
      <ElAlert
        v-if="isNeedReview"
        class="review-alert"
        title="该结果置信度较低，已进入人工审核队列"
        type="error"
        :closable="false"
        show-icon
      >
        <template #icon>
          <el-icon :size="18"><WarningFilled /></el-icon>
        </template>
      </ElAlert>

      <!-- 页面头部 -->
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">识别结果详情</h1>
          <p class="page-subtitle">查看病害识别的详细结果与防治建议</p>
        </div>
        <div class="header-actions">
          <ElButton @click="handleBack">
            <el-icon><ArrowLeft /></el-icon>
            返回列表
          </ElButton>
        </div>
      </div>

      <!-- 顶部信息卡片 -->
      <div class="info-card">
        <div class="info-card__left">
          <div class="disease-header">
            <h1 class="disease-name">{{ result.diseaseName }}</h1>
            <ElTag :type="statusTagType" size="large" effect="light">{{ statusLabel }}</ElTag>
          </div>
          <div class="disease-meta">
            <span class="meta-item">
              <span class="meta-label">置信度等级</span>
              <span class="meta-value" :style="{ color: confidenceColor }">{{ confidenceText }}</span>
            </span>
            <span class="meta-divider">|</span>
            <span class="meta-item">
              <span class="meta-label">识别状态</span>
              <span class="meta-value">{{ statusLabel }}</span>
            </span>
          </div>
        </div>
        <div class="info-card__right">
          <div class="confidence-ring">
            <ElProgress
              type="circle"
              :percentage="confidencePercent"
              :stroke-width="10"
              :stroke-color="confidenceColorHex"
              :width="140"
              :show-text="false"
            />
            <div class="confidence-ring__center">
              <span class="confidence-ring__value" :style="{ color: confidenceColor }">{{ confidencePercent }}</span>
              <span class="confidence-ring__unit">%</span>
            </div>
          </div>
          <span class="confidence-ring__label">置信度</span>
        </div>
      </div>

      <!-- 防治建议卡片 -->
      <div class="treatment-card">
        <div class="treatment-card__bar"></div>
        <div class="treatment-card__content">
          <div class="section-header">
            <h3 class="section-title">防治建议</h3>
          </div>
          <div class="treatment-body">
            <div class="treatment-text">{{ treatmentText }}</div>
          </div>
        </div>
      </div>

      <!-- 引用来源卡片 -->
      <div class="citations-section">
        <div class="section-header">
          <h3 class="section-title">引用来源</h3>
          <span class="section-count">共 {{ result.citations.length }} 条</span>
        </div>
        <div class="citations-grid">
          <div
            v-for="(citation, index) in result.citations"
            :key="index"
            class="citation-card"
          >
            <div class="citation-index">{{ index + 1 }}</div>
            <div class="citation-body">
              <a class="citation-title" href="javascript:void(0)">{{ citation.docTitle }}</a>
              <p class="citation-snippet">{{ citation.snippet }}</p>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* 加载状态 */
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 50vh;
}

/* 待审核提示 */
.review-alert {
  margin-bottom: var(--spacing-md);
  border-radius: var(--radius-sm);
}

/* 页面头部 */
.header-left {
  display: flex;
  flex-direction: column;
}

.header-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.result-list-card {
  padding: var(--spacing-lg);
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.disease-list-name {
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 顶部信息卡片 */
.info-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-lg);
  transition: box-shadow var(--transition-normal);
}

.info-card:hover {
  box-shadow: var(--shadow-md);
}

.info-card__left {
  flex: 1;
}

.disease-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.disease-name {
  margin: 0;
  font-size: var(--font-size-4xl);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.5px;
}

.disease-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.meta-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.meta-value {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text-primary);
}

.meta-divider {
  color: var(--color-border);
}

/* 置信度环形 */
.info-card__right {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
  margin-left: var(--spacing-2xl);
}

.confidence-ring {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.confidence-ring__center {
  position: absolute;
  display: flex;
  align-items: baseline;
  justify-content: center;
}

.confidence-ring__value {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  line-height: 1;
}

.confidence-ring__unit {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin-left: 2px;
}

.confidence-ring__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 防治建议卡片 */
.treatment-card {
  display: flex;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  margin-bottom: var(--spacing-lg);
  transition: box-shadow var(--transition-normal);
}

.treatment-card:hover {
  box-shadow: var(--shadow-md);
}

.treatment-card__bar {
  width: 4px;
  background: var(--color-primary);
  border-radius: 2px;
  flex-shrink: 0;
}

.treatment-card__content {
  flex: 1;
  padding: var(--spacing-xl);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-md);
}

.section-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-text-primary);
}

.section-count {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.treatment-body {
  background: var(--color-primary-lighter);
  padding: var(--spacing-lg);
  border-radius: var(--radius-md);
}

.treatment-text {
  font-size: var(--font-size-base);
  line-height: 1.8;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-md);
  white-space: pre-wrap;
}

.treatment-text:last-child {
  margin-bottom: 0;
}

/* 引用来源 */
.citations-section {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-lg);
  transition: box-shadow var(--transition-normal);
}

.citations-section:hover {
  box-shadow: var(--shadow-md);
}

.citations-grid {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.citation-card {
  display: flex;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  background: var(--color-bg-page);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
  transition: all var(--transition-normal);
}

.citation-card:hover {
  border-color: var(--color-primary-lighter);
  background: var(--color-primary-lighter);
}

.citation-index {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary);
  color: var(--color-bg-card);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  font-weight: 600;
  flex-shrink: 0;
}

.citation-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.citation-title {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-info);
  text-decoration: none;
  cursor: pointer;
  transition: color var(--transition-fast);
}

.citation-title:hover {
  color: var(--color-primary-light);
  text-decoration: underline;
}

.citation-snippet {
  margin: 0;
  font-size: var(--font-size-sm);
  line-height: 1.7;
  color: var(--color-text-secondary);
}

/* 响应式 */
@media (max-width: 768px) {
  .info-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .info-card__right {
    margin-left: 0;
    margin-top: var(--spacing-lg);
    align-self: center;
  }

  .disease-name {
    font-size: var(--font-size-2xl);
  }

  .header-actions {
    flex-wrap: wrap;
  }
}
</style>
