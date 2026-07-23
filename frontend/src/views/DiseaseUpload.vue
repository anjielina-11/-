<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import {
  ElCard,
  ElUpload,
  ElSelect,
  ElOption,
  ElButton,
  ElProgress,
  ElMessage,
  ElImage,
  ElCollapse,
  ElCollapseItem,
  type UploadRawFile
} from 'element-plus'
import { Delete, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

interface UploadResponse {
  code: number
  data: {
    taskId: string
  }
}

interface DiagnosisResultResponse {
  code: number
  data: {
    taskId: string
    status: 'pending' | 'processing' | 'completed' | 'failed'
    progress: number
    result?: {
      diseaseName: string
      confidence: number
      suggestion: string
    }
    message?: string
  }
}

interface Field {
  id: string
  name: string
}

const userStore = useUserStore()
const fields = ref<Field[]>([])
const fieldsLoading = ref(false)

const selectedField = ref('')
const uploadedImageUrl = ref('')
const uploadedFileId = ref('')
const isUploading = ref(false)

const taskId = ref('')
const diagnosisStatus = ref<'pending' | 'processing' | 'completed' | 'failed' | ''>('')
const diagnosisProgress = ref(0)
const diagnosisResult = ref<{ diseaseName: string; confidence: number; suggestion: string } | null>(null)

let pollTimer: number | null = null

const uploadUrl = '/api/v1/diagnosis/upload'

const mockFarmerFields: Field[] = [
  { id: '1', name: '水稻田A' },
  { id: '2', name: '玉米地B' },
  { id: '3', name: '蔬菜基地C' },
  { id: '4', name: '水果园D' }
]

const fetchFields = async () => {
  fieldsLoading.value = true

  try {
    const useMock = import.meta.env.VITE_MOCK_LOGIN === 'true'

    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500))

      if (userStore.hasRole('farmer')) {
        fields.value = mockFarmerFields
      } else {
        fields.value = []
        ElMessage.warning('请以农户身份登录操作')
      }
    } else {
      const farmResponse = await request.get<{ code: number; data: { list: { id: string; name: string }[] } }>('/farms')
      if (farmResponse.code === 200 && farmResponse.data.list.length > 0) {
        const farmId = farmResponse.data.list[0].id
        const fieldsResponse = await request.get<{ code: number; data: { list: Field[] } }>(`/farms/${farmId}/fields`)
        if (fieldsResponse.code === 200) {
          fields.value = fieldsResponse.data.list
        }
      }
    }
  } catch (error) {
    ElMessage.error('获取地块列表失败')
  } finally {
    fieldsLoading.value = false
  }
}

const beforeUpload = (file: UploadRawFile) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG 格式的图片')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  isUploading.value = true
  return true
}

const handleUploadSuccess = (response: UploadResponse) => {
  isUploading.value = false
  if (response.code === 200 && response.data) {
    taskId.value = response.data.taskId
    diagnosisStatus.value = 'processing'
    diagnosisProgress.value = 10
    diagnosisResult.value = null
    ElMessage.success('识别任务已提交')
    startPolling()
  } else {
    ElMessage.error('图片上传失败')
  }
}

const handleUploadError = () => {
  isUploading.value = false
  ElMessage.error('图片上传失败')
}

const handleRemove = () => {
  uploadedImageUrl.value = ''
  uploadedFileId.value = ''
}



const startPolling = () => {
  stopPolling()
  pollTimer = window.setInterval(async () => {
    try {
      const response = await request.get<DiagnosisResultResponse>(`/diagnosis/result/${taskId.value}`)

      if (response.code === 200 && response.data) {
        diagnosisStatus.value = response.data.status
        diagnosisProgress.value = response.data.progress

        if (response.data.status === 'completed') {
          diagnosisResult.value = response.data.result || null
          stopPolling()
          ElMessage.success('识别完成')
        } else if (response.data.status === 'failed') {
          stopPolling()
          ElMessage.error(response.data.message || '识别失败')
        }
      }
    } catch (error) {
      console.error('轮询识别结果失败:', error)
    }
  }, 2000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const handleReset = () => {
  stopPolling()
  selectedField.value = ''
  uploadedImageUrl.value = ''
  uploadedFileId.value = ''
  taskId.value = ''
  diagnosisStatus.value = ''
  diagnosisProgress.value = 0
  diagnosisResult.value = null
}

const statusTextMap: Record<string, string> = {
  '': '等待提交',
  'pending': '排队中...',
  'processing': '正在识别中...',
  'completed': '识别完成',
  'failed': '识别失败'
}

onMounted(() => {
  fetchFields()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">病害上报</h1>
        <p class="page-subtitle">上传病害图片，获取智能诊断结果和防治建议</p>
      </div>
    </div>

    <!-- 两栏布局 -->
    <div class="content-row">
      <!-- 左侧：上传表单 -->
      <ElCard class="upload-card" shadow="never">
        <template #header>
          <div class="card-header-title">病害图片上报</div>
        </template>

        <div class="form-section">
          <div class="form-item">
            <label class="form-label">关联地块</label>
            <ElSelect
              v-model="selectedField"
              placeholder="请选择地块"
              style="width: 100%"
              :disabled="isUploading || fieldsLoading"
            >
              <ElOption v-for="field in fields" :key="field.id" :label="field.name" :value="field.id" />
            </ElSelect>
          </div>

          <div class="form-item">
            <label class="form-label">病害图片</label>
            <div class="upload-wrapper">
              <ElUpload
                v-if="!uploadedImageUrl"
                :action="uploadUrl"
                :show-file-list="false"
                :before-upload="beforeUpload"
                :on-success="handleUploadSuccess"
                :on-error="handleUploadError"
                :disabled="isUploading || !selectedField"
                :data="{ cycleId: selectedField }"
                drag
                class="custom-upload"
              >
                <div class="upload-drag-area">
                  <div class="upload-icon">
                    <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                  </div>
                  <p class="upload-text">将图片拖到此处，或<em>点击上传</em></p>
                  <p class="upload-hint">支持 JPG/PNG 格式，单张不超过 10MB</p>
                </div>
              </ElUpload>

              <div v-else class="preview-area">
                <div class="preview-image-wrap">
                  <ElImage :src="uploadedImageUrl" class="preview-image" fit="contain" />
                  <div class="preview-overlay" @click="handleRemove">
                    <ElButton type="danger" :icon="Delete" circle size="small" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <ElButton @click="handleReset" :icon="Refresh">重置</ElButton>
          </div>
        </div>
      </ElCard>

      <!-- 右侧：识别结果 -->
      <div class="result-column">
        <!-- 识别进度 -->
        <ElCard v-if="taskId" class="progress-card" shadow="never">
          <template #header>
            <div class="card-header-title">识别进度</div>
          </template>

          <div class="progress-content">
            <div class="progress-ring">
              <ElProgress
                type="circle"
                :percentage="diagnosisProgress"
                :width="120"
                :stroke-width="8"
                :status="diagnosisStatus === 'completed' ? 'success' : diagnosisStatus === 'failed' ? 'exception' : undefined"
                :color="diagnosisStatus === 'processing' ? 'var(--color-primary)' : undefined"
              />
            </div>
            <div class="progress-info">
              <div class="progress-status" :class="{
                'status-processing': diagnosisStatus === 'processing',
                'status-completed': diagnosisStatus === 'completed',
                'status-failed': diagnosisStatus === 'failed'
              }">
                {{ statusTextMap[diagnosisStatus] }}
              </div>
              <div class="progress-task-id">
                <span class="task-id-label">任务ID：</span>
                <span class="task-id-value">{{ taskId }}</span>
              </div>
            </div>
          </div>

          <div v-if="diagnosisStatus === 'processing'" class="progress-animation">
            <div class="pulse-dot"></div>
            <span class="animation-text">AI 正在分析图片特征...</span>
          </div>
        </ElCard>

        <!-- 识别结果 -->
        <ElCard v-if="diagnosisResult" class="result-card" shadow="never">
          <template #header>
            <div class="card-header-title">识别结果</div>
          </template>

          <div class="result-content">
            <!-- 病害名称 -->
            <div class="disease-name-section">
              <div class="disease-name">{{ diagnosisResult.diseaseName }}</div>
              <div class="disease-badge">AI 诊断</div>
            </div>

            <!-- 置信度 -->
            <div class="confidence-section">
              <div class="confidence-header">
                <span class="confidence-label">置信度</span>
                <span class="confidence-value">{{ diagnosisResult.confidence }}%</span>
              </div>
              <div class="confidence-bar">
                <div
                  class="confidence-fill"
                  :style="{ width: diagnosisResult.confidence + '%' }"
                  :class="{
                    'confidence-high': diagnosisResult.confidence >= 80,
                    'confidence-medium': diagnosisResult.confidence >= 50 && diagnosisResult.confidence < 80,
                    'confidence-low': diagnosisResult.confidence < 50
                  }"
                ></div>
              </div>
            </div>

            <!-- 防治建议 -->
            <div class="suggestion-section">
              <div class="suggestion-title">防治建议</div>
              <div class="suggestion-card">
                <div class="suggestion-icon">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z"/><path d="M7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/></svg>
                </div>
                <div class="suggestion-text">{{ diagnosisResult.suggestion }}</div>
              </div>
            </div>

            <!-- 引用来源折叠面板 -->
            <ElCollapse class="source-collapse">
              <ElCollapseItem title="参考来源与扩展信息" name="1">
                <div class="source-content">
                  <div class="source-item">
                    <span class="source-dot"></span>
                    <span>云南省农业科学院植保研究所 - 常见病害防治手册</span>
                  </div>
                  <div class="source-item">
                    <span class="source-dot"></span>
                    <span>中国农作物病虫害数据库 - 区域病害图谱</span>
                  </div>
                  <div class="source-item">
                    <span class="source-dot"></span>
                    <span>AI 模型训练数据集 - 云南特色作物病害样本</span>
                  </div>
                </div>
              </ElCollapseItem>
            </ElCollapse>
          </div>
        </ElCard>

        <!-- 无结果占位 -->
        <ElCard v-if="!taskId" class="placeholder-card" shadow="never">
          <div class="placeholder-content">
            <div class="placeholder-icon">
              <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
            </div>
            <p class="placeholder-text">提交病害图片后，识别结果将在此处展示</p>
          </div>
        </ElCard>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 内容区两栏布局 */
.content-row {
  display: flex;
  gap: var(--spacing-md);
  align-items: flex-start;
}

.upload-card {
  width: 480px;
  flex-shrink: 0;
}

.upload-card :deep(.el-card__header) {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-light);
}

.result-column {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.card-header-title {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 表单 */
.form-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.form-label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text-primary);
}

/* 上传区域 */
.upload-wrapper {
  width: 100%;
}

.custom-upload {
  width: 100%;
}

.custom-upload :deep(.el-upload) {
  width: 100%;
}

.custom-upload :deep(.el-upload-dragger) {
  width: 100%;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-md);
  background-color: var(--color-bg-page);
  padding: var(--spacing-xl) var(--spacing-lg);
  transition: all var(--transition-normal);
}

.custom-upload :deep(.el-upload-dragger:hover) {
  border-color: var(--color-primary);
  background-color: var(--color-primary-lighter);
}

.custom-upload :deep(.el-upload-dragger:hover .upload-icon) {
  color: var(--color-primary);
}

.upload-drag-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
}

.upload-icon {
  color: var(--color-text-placeholder);
  transition: color var(--transition-normal);
}

.upload-text {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
}

.upload-text em {
  color: var(--color-primary);
  font-style: normal;
  cursor: pointer;
}

.upload-hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-placeholder);
}

/* 预览区域 */
.preview-area {
  display: flex;
  justify-content: center;
}

.preview-image-wrap {
  position: relative;
  border-radius: var(--radius-md);
  overflow: hidden;
  max-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--color-bg-page);
}

.preview-image {
  width: 100%;
  max-height: 280px;
  border-radius: var(--radius-md);
}

.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--transition-normal);
  border-radius: var(--radius-md);
}

.preview-image-wrap:hover .preview-overlay {
  opacity: 1;
}

/* 表单操作按钮 */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border-light);
}

/* 识别进度卡片 */
.progress-card :deep(.el-card__header) {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-light);
}

.progress-content {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-md) 0;
}

.progress-ring :deep(.el-progress__text) {
  font-size: var(--font-size-lg) !important;
  font-weight: 700;
}

.progress-info {
  flex: 1;
}

.progress-status {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xs);
}

.status-processing {
  color: var(--color-primary);
}

.status-completed {
  color: var(--color-success);
}

.status-failed {
  color: var(--color-danger);
}

.progress-task-id {
  font-size: var(--font-size-xs);
  color: var(--color-text-placeholder);
}

.task-id-label {
  color: var(--color-text-secondary);
}

.task-id-value {
  font-family: monospace;
  color: var(--color-info);
}

.progress-animation {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background-color: var(--color-primary-lighter);
  border-radius: var(--radius-sm);
  margin-top: var(--spacing-sm);
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--color-primary);
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

.animation-text {
  font-size: var(--font-size-xs);
  color: var(--color-primary);
}

/* 识别结果卡片 */
.result-card :deep(.el-card__header) {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-light);
}

.result-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.disease-name-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.disease-name {
  font-size: var(--font-size-2xl);
  font-weight: 700;
  color: var(--color-primary-dark);
}

.disease-badge {
  display: inline-block;
  padding: 2px 10px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: #fff;
  border-radius: 20px;
  font-size: var(--font-size-xs);
  font-weight: 500;
}

/* 置信度 */
.confidence-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.confidence-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.confidence-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.confidence-value {
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

.confidence-bar {
  width: 100%;
  height: 8px;
  background-color: var(--color-border-light);
  border-radius: 4px;
  overflow: hidden;
}

.confidence-fill {
  height: 100%;
  border-radius: 4px;
  transition: width var(--transition-slow);
}

.confidence-high {
  background: linear-gradient(90deg, var(--color-primary), var(--color-success));
}

.confidence-medium {
  background: linear-gradient(90deg, var(--color-warning), #FFD666);
}

.confidence-low {
  background: linear-gradient(90deg, var(--color-danger), #FF7875);
}

/* 防治建议 */
.suggestion-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.suggestion-title {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-secondary);
}

.suggestion-card {
  display: flex;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  background-color: var(--color-bg-page);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--color-primary);
}

.suggestion-icon {
  flex-shrink: 0;
  color: var(--color-primary);
  margin-top: 2px;
}

.suggestion-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-regular);
  line-height: 1.6;
}

/* 引用来源折叠面板 */
.source-collapse {
  border: none;
}

.source-collapse :deep(.el-collapse-item__header) {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  border-bottom: none;
  background-color: transparent;
  padding: 0;
  height: 36px;
  line-height: 36px;
}

.source-collapse :deep(.el-collapse-item__wrap) {
  border-bottom: none;
  background-color: transparent;
}

.source-collapse :deep(.el-collapse-item__content) {
  padding: 0 0 var(--spacing-sm);
}

.source-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.source-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.source-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background-color: var(--color-primary);
  flex-shrink: 0;
}

/* 占位卡片 */
.placeholder-card :deep(.el-card__body) {
  padding: var(--spacing-2xl);
}

.placeholder-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-xl) 0;
}

.placeholder-icon {
  color: var(--color-text-placeholder);
  opacity: 0.5;
}

.placeholder-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-placeholder);
  text-align: center;
}

/* 响应式 */
@media (max-width: 1024px) {
  .content-row {
    flex-direction: column;
  }
  .upload-card {
    width: 100%;
  }
}
</style>
