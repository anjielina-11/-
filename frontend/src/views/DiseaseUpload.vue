<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  ElCard,
  ElUpload,
  ElSelect,
  ElOption,
  ElButton,
  ElProgress,
  ElMessage,
  ElImage,
  type UploadRawFile
} from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

interface UploadResponse {
  code: number
  data: {
    fileUrl: string
    fileId: string
  }
}

interface DiagnosisSubmitResponse {
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

interface FieldsResponse {
  code: number
  data: Field[]
}

const userStore = useUserStore()
const fields = ref<Field[]>([])
const fieldsLoading = ref(false)

const selectedField = ref('')
const uploadedImageUrl = ref('')
const uploadedFileId = ref('')
const isUploading = ref(false)
const isSubmitting = ref(false)

const taskId = ref('')
const diagnosisStatus = ref<'pending' | 'processing' | 'completed' | 'failed' | ''>('')
const diagnosisProgress = ref(0)
const diagnosisResult = ref<{ diseaseName: string; confidence: number; suggestion: string } | null>(null)

let pollTimer: number | null = null

const canSubmit = computed(() => {
  return selectedField.value && uploadedImageUrl.value && !isSubmitting.value
})

const uploadUrl = '/api/upload/image'

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
      const response = await request.get<FieldsResponse>('/api/farms/my-fields')
      if (response.code === 0) {
        fields.value = response.data
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
  if (response.code === 0 && response.data) {
    uploadedImageUrl.value = response.data.fileUrl
    uploadedFileId.value = response.data.fileId
    ElMessage.success('图片上传成功')
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

const submitDiagnosis = async () => {
  if (!canSubmit.value) return

  isSubmitting.value = true

  try {
    const response = await request.post<DiagnosisSubmitResponse>('/api/diagnosis/submit', {
      fieldId: selectedField.value,
      imageUrl: uploadedImageUrl.value
    })

    if (response.code === 0 && response.data) {
      taskId.value = response.data.taskId
      diagnosisStatus.value = 'processing'
      diagnosisProgress.value = 10
      diagnosisResult.value = null
      ElMessage.success('识别任务已提交')
      startPolling()
    } else {
      ElMessage.error('提交识别任务失败')
    }
  } catch (error) {
    ElMessage.error('提交识别任务失败')
  } finally {
    isSubmitting.value = false
  }
}

const startPolling = () => {
  stopPolling()
  pollTimer = window.setInterval(async () => {
    try {
      const response = await request.get<DiagnosisResultResponse>(`/api/diagnosis/result/${taskId.value}`)

      if (response.code === 0 && response.data) {
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

onMounted(() => {
  fetchFields()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div class="disease-upload-container">
    <ElCard class="upload-card" header="病害图片上报">
      <div class="form-item">
        <label class="form-label">关联地块</label>
        <ElSelect
          v-model="selectedField"
          placeholder="请选择地块"
          style="width: 100%"
          :disabled="isSubmitting || fieldsLoading"
        >
          <ElOption v-for="field in fields" :key="field.id" :label="field.name" :value="field.id" />
        </ElSelect>
      </div>

      <div class="form-item">
        <label class="form-label">病害图片</label>
        <ElUpload
          :action="uploadUrl"
          :show-file-list="false"
          :before-upload="beforeUpload"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :disabled="isSubmitting"
          drag
        >
          <div v-if="!uploadedImageUrl" class="upload-area">
            <ElImage v-if="false" :src="''" />
            <p class="upload-text">将图片拖到此处，或<em>点击上传</em></p>
            <p class="upload-hint">支持 JPG/PNG 格式，单张不超过 10MB</p>
          </div>
          <div v-else class="preview-area">
            <ElImage :src="uploadedImageUrl" class="preview-image" fit="contain" />
            <ElButton type="danger" link @click="handleRemove">删除图片</ElButton>
          </div>
        </ElUpload>
      </div>

      <div class="button-group">
        <ElButton type="primary" :loading="isSubmitting" :disabled="!canSubmit" @click="submitDiagnosis">
          提交识别
        </ElButton>
        <ElButton @click="handleReset">重置</ElButton>
      </div>
    </ElCard>

    <ElCard v-if="taskId" class="result-card" header="识别进度">
      <div class="task-info">
        <span class="label">识别任务 ID：</span>
        <span class="value">{{ taskId }}</span>
      </div>

      <div class="progress-section">
        <ElProgress :percentage="diagnosisProgress" :status="diagnosisStatus === 'completed' ? 'success' : diagnosisStatus === 'failed' ? 'exception' : 'warning'" />
        <span class="progress-text">{{ diagnosisStatus === 'processing' ? '正在识别中...' : diagnosisStatus === 'completed' ? '识别完成' : diagnosisStatus === 'failed' ? '识别失败' : '等待中' }}</span>
      </div>

      <div v-if="diagnosisResult" class="result-section">
        <h4>识别结果</h4>
        <div class="result-item">
          <span class="label">病害名称：</span>
          <span class="value">{{ diagnosisResult.diseaseName }}</span>
        </div>
        <div class="result-item">
          <span class="label">置信度：</span>
          <span class="value">{{ diagnosisResult.confidence }}%</span>
        </div>
        <div class="result-item">
          <span class="label">防治建议：</span>
          <span class="value">{{ diagnosisResult.suggestion }}</span>
        </div>
      </div>
    </ElCard>
  </div>
</template>

<style scoped>
.disease-upload-container {
  padding: 20px;
  background: #fafafa;
  min-height: calc(100vh - 60px);
}

.upload-card,
.result-card {
  max-width: 600px;
  margin: 0 auto 20px;
}

.form-item {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #303133;
}

.upload-area {
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.3s;
}

.upload-area:hover {
  border-color: #409eff;
}

.upload-text {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.upload-text em {
  color: #409eff;
  cursor: pointer;
}

.upload-hint {
  margin: 12px 0 0;
  color: #909399;
  font-size: 12px;
}

.preview-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.preview-image {
  width: 100%;
  max-height: 300px;
  border-radius: 8px;
}

.button-group {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}

.task-info {
  margin-bottom: 20px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.task-info .label {
  color: #606266;
}

.task-info .value {
  color: #409eff;
  font-family: monospace;
  font-size: 14px;
}

.progress-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.progress-section .progress-text {
  font-size: 14px;
  color: #606266;
}

.result-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #ebeef5;
}

.result-section h4 {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.result-item {
  display: flex;
  margin-bottom: 12px;
}

.result-item .label {
  width: 100px;
  color: #606266;
  flex-shrink: 0;
}

.result-item .value {
  color: #303133;
}
</style>