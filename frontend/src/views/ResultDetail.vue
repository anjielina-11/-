<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  ElCard,
  ElProgress,
  ElAlert,
  ElCollapse,
  ElCollapseItem,
  ElButton,
  ElMessage,
  ElTag
} from 'element-plus'

export interface ICitation {
  docTitle: string
  snippet: string
}

export interface IResult {
  diseaseName: string
  confidence: number
  treatment: string
  citations: ICitation[]
  status: 'pending' | 'processing' | 'completed' | 'need_review'
}

const result = ref<IResult | null>(null)
const loading = ref(true)

const mockResult: IResult = {
  diseaseName: '水稻稻瘟病',
  confidence: 0.85,
  treatment: '1. 选用抗病品种：选择当地推广的抗病品种，如"滇杂31"、"云粳38"等。\n\n2. 种子处理：播种前用25%咪鲜胺乳油2000-3000倍液浸种24-48小时，或用10%浸种灵乳油500倍液浸种48小时。\n\n3. 加强田间管理：合理密植，避免过度氮肥，浅水勤灌，适时晒田，增强植株抗病能力。\n\n4. 药剂防治：发病初期及时喷药，可选用40%稻瘟灵乳油1000倍液、75%三环唑可湿性粉剂2000倍液、25%咪鲜胺乳油1500-2000倍液等，每隔7-10天喷一次，连续防治2-3次。',
  citations: [
    {
      docTitle: '云南水稻病虫害防治技术手册（2024版）',
      snippet: '稻瘟病是云南省水稻生产中最严重的病害之一，在高温高湿环境下极易爆发。选用抗病品种是最经济有效的防治措施，配合药剂防治可有效控制病害蔓延。'
    },
    {
      docTitle: '中国农业百科全书·植物病理学卷',
      snippet: '稻瘟病菌属半知菌亚门真菌，主要以分生孢子在病稻草和病谷上越冬。发病适宜温度为25-28℃，相对湿度90%以上。'
    },
    {
      docTitle: '云南省农作物病虫害综合防治技术规范',
      snippet: '在云南高原气候条件下，稻瘟病一般在分蘖期和抽穗期发生较重，应加强监测预警，及时采取综合防治措施。'
    }
  ],
  status: 'completed'
}

const confidencePercent = computed(() => {
  return result.value ? Math.round(result.value.confidence * 100) : 0
})

const confidenceColor = computed(() => {
  const percent = confidencePercent.value
  if (percent >= 80) return '#67c23a'
  if (percent >= 50) return '#e6a23c'
  return '#f56c6c'
})

const confidenceText = computed(() => {
  const percent = confidencePercent.value
  if (percent >= 80) return '高'
  if (percent >= 50) return '中'
  return '低'
})

const isNeedReview = computed(() => {
  return result.value?.status === 'need_review'
})

const handleBack = () => {
  ElMessage.info('返回列表')
}

const handleGenerateTask = () => {
  if (!result.value) return
  ElMessage.success(`已为「${result.value.diseaseName}」生成农事任务`)
}

onMounted(() => {
  setTimeout(() => {
    result.value = mockResult
    loading.value = false
  }, 500)
})
</script>

<template>
  <div class="result-detail-container">
    <div v-if="loading" class="loading-container">
      <ElProgress type="circle" :percentage="0" />
    </div>

    <template v-else-if="result">
      <div class="header-section">
        <ElAlert
          v-if="isNeedReview"
          title="该结果置信度较低，已进入人工审核队列"
          type="error"
          :closable="false"
          show-icon
        />
      </div>

      <ElCard class="info-card">
        <div class="disease-info">
          <div class="disease-header">
            <h1 class="disease-name">{{ result.diseaseName }}</h1>
            <ElTag :type="isNeedReview ? 'danger' : 'success'" size="large">
              {{ result.status === 'completed' ? '识别完成' : result.status === 'need_review' ? '待审核' : result.status === 'processing' ? '处理中' : '待处理' }}
            </ElTag>
          </div>

          <div class="confidence-section">
            <div class="confidence-circle">
              <ElProgress
                type="circle"
                :percentage="confidencePercent"
                :stroke-width="12"
                :stroke-color="confidenceColor"
                :text-inside="true"
                :width="120"
              />
            </div>
            <div class="confidence-info">
              <p class="confidence-label">置信度</p>
              <p class="confidence-value" :style="{ color: confidenceColor }">
                {{ confidencePercent }}%
              </p>
              <p class="confidence-level">
                <span>等级：</span>
                <span :style="{ color: confidenceColor, fontWeight: 'bold' }">{{ confidenceText }}</span>
              </p>
            </div>
          </div>
        </div>

        <div class="button-group">
          <ElButton type="primary" @click="handleBack">返回列表</ElButton>
          <ElButton type="success" @click="handleGenerateTask">生成农事任务</ElButton>
        </div>
      </ElCard>

      <ElCard class="treatment-card" header="防治建议">
        <div class="treatment-content">
          <pre>{{ result.treatment }}</pre>
        </div>
      </ElCard>

      <ElCard class="citations-card" header="引用来源">
        <ElCollapse>
          <ElCollapseItem
            v-for="(citation, index) in result.citations"
            :key="index"
            :title="citation.docTitle"
          >
            <div class="citation-item">
              <p class="citation-title">{{ citation.docTitle }}</p>
              <p class="citation-snippet">{{ citation.snippet }}</p>
            </div>
          </ElCollapseItem>
        </ElCollapse>
      </ElCard>
    </template>
  </div>
</template>

<style scoped>
.result-detail-container {
  padding: 24px;
  background: #fafafa;
  min-height: calc(100vh - 60px);
  max-width: 900px;
  margin: 0 auto;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 50vh;
}

.header-section {
  margin-bottom: 20px;
}

.info-card {
  margin-bottom: 20px;
}

.disease-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.disease-name {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.confidence-section {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.confidence-circle {
  flex-shrink: 0;
}

.confidence-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.confidence-label {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.confidence-value {
  margin: 0;
  font-size: 36px;
  font-weight: 600;
}

.confidence-level {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.button-group {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.treatment-card {
  margin-bottom: 20px;
}

.treatment-content {
  background: #f9fafb;
  padding: 20px;
  border-radius: 8px;
}

.treatment-content pre {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-all;
}

.citations-card {
  margin-bottom: 20px;
}

.citation-item {
  padding: 16px;
  background: #f9fafb;
  border-radius: 6px;
}

.citation-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 500;
  color: #409eff;
}

.citation-snippet {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
}

:deep(.el-collapse-item__header) {
  font-weight: 500;
  color: #303133;
}

:deep(.el-collapse-item__content) {
  padding-top: 12px !important;
}
</style>