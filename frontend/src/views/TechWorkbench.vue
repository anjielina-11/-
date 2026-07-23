<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  ElTable,
  ElTableColumn,
  ElButton,
  ElTag,
  ElSelect,
  ElOption,
  ElDatePicker,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElProgress,
  ElMessage,
  type FormInstance
} from 'element-plus'
import { Clock, CircleCheck, CircleClose, RefreshRight } from '@element-plus/icons-vue'

export type ReviewStatus = 'pending' | 'approved' | 'rejected'

export interface ReviewItem {
  taskId: string
  farmerName: string
  fieldName: string
  diseaseName: string
  confidence: number
  status: ReviewStatus
  submitTime: string
  treatment: string
  citations: { docTitle: string; snippet: string }[]
}

interface ReviewResponse {
  code: number
  message: string
}

const tableData = ref<ReviewItem[]>([])
const loading = ref(false)

const statusFilter = ref<ReviewStatus | ''>('')
const dateRange = ref<Date[]>([])

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const currentItem = ref<ReviewItem | null>(null)

const form = reactive({
  rejectReason: ''
})

const formRef = ref<FormInstance>()

const mockData: ReviewItem[] = [
  {
    taskId: 'DIAG-20240701-001',
    farmerName: '张农户',
    fieldName: '水稻田A',
    diseaseName: '水稻稻瘟病',
    confidence: 0.78,
    status: 'pending',
    submitTime: '2024-07-01 10:30:00',
    treatment: '选用抗病品种，加强田间管理，适时喷药防治。',
    citations: [
      { docTitle: '云南水稻病虫害防治技术手册', snippet: '稻瘟病在高温高湿环境下极易爆发。' }
    ]
  },
  {
    taskId: 'DIAG-20240701-002',
    farmerName: '李农户',
    fieldName: '玉米地B',
    diseaseName: '玉米纹枯病',
    confidence: 0.65,
    status: 'pending',
    submitTime: '2024-07-01 14:20:00',
    treatment: '合理密植，减少氮肥，及时清除病株。',
    citations: [
      { docTitle: '玉米病虫害综合防治技术', snippet: '纹枯病主要危害玉米茎秆和叶鞘。' }
    ]
  },
  {
    taskId: 'DIAG-20240630-003',
    farmerName: '王农户',
    fieldName: '蔬菜基地C',
    diseaseName: '番茄晚疫病',
    confidence: 0.92,
    status: 'approved',
    submitTime: '2024-06-30 09:15:00',
    treatment: '轮作倒茬，加强通风透光，使用保护性药剂。',
    citations: [
      { docTitle: '蔬菜病虫害防治指南', snippet: '晚疫病是番茄生产中的毁灭性病害。' }
    ]
  },
  {
    taskId: 'DIAG-20240630-004',
    farmerName: '赵农户',
    fieldName: '水果园D',
    diseaseName: '柑橘黄龙病',
    confidence: 0.45,
    status: 'rejected',
    submitTime: '2024-06-30 16:45:00',
    treatment: '加强检疫，及时清除病树，控制木虱传播。',
    citations: [
      { docTitle: '柑橘病虫害防治手册', snippet: '黄龙病是柑橘的毁灭性病害。' }
    ]
  },
  {
    taskId: 'DIAG-20240629-005',
    farmerName: '孙农户',
    fieldName: '茶叶基地E',
    diseaseName: '茶炭疽病',
    confidence: 0.88,
    status: 'approved',
    submitTime: '2024-06-29 11:00:00',
    treatment: '选用抗病品种，及时摘除病叶，喷药保护。',
    citations: [
      { docTitle: '茶树病虫害防治技术', snippet: '炭疽病主要危害茶树叶片。' }
    ]
  },
  {
    taskId: 'DIAG-20240629-006',
    farmerName: '周农户',
    fieldName: '中药材园F',
    diseaseName: '三七根腐病',
    confidence: 0.55,
    status: 'pending',
    submitTime: '2024-06-29 13:30:00',
    treatment: '轮作倒茬，土壤消毒，选用健康种苗。',
    citations: [
      { docTitle: '中药材病虫害防治技术', snippet: '根腐病是三七的主要病害之一。' }
    ]
  }
]

const statusOptions = [
  { value: '', label: '全部' },
  { value: 'pending', label: '待审核' },
  { value: 'approved', label: '已通过' },
  { value: 'rejected', label: '已驳回' }
]

const getStatusTag = (status: ReviewStatus) => {
  const configs = {
    pending: { type: 'warning' as const, label: '待审核' },
    approved: { type: 'success' as const, label: '已通过' },
    rejected: { type: 'danger' as const, label: '已驳回' }
  }
  return configs[status]
}

const pendingCount = computed(() => tableData.value.filter(item => item.status === 'pending').length)
const approvedCount = computed(() => tableData.value.filter(item => item.status === 'approved').length)
const rejectedCount = computed(() => tableData.value.filter(item => item.status === 'rejected').length)

const getConfidenceColor = (confidence: number) => {
  if (confidence >= 0.8) return 'var(--color-success)'
  if (confidence >= 0.5) return 'var(--color-warning)'
  return 'var(--color-danger)'
}

const filteredData = computed(() => {
  let data = [...tableData.value]
  
  if (statusFilter.value) {
    data = data.filter(item => item.status === statusFilter.value)
  }
  
  if (dateRange.value && dateRange.value.length === 2) {
    const start = dateRange.value[0].toISOString().split('T')[0]
    const end = dateRange.value[1].toISOString().split('T')[0]
    data = data.filter(item => {
      const submitDate = item.submitTime.split(' ')[0]
      return submitDate >= start && submitDate <= end
    })
  }
  
  return data
})

const fetchData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = [...mockData]
    loading.value = false
  }, 500)
}

const handleFilterChange = () => {
  fetchData()
}

const handleReset = () => {
  statusFilter.value = ''
  dateRange.value = []
  fetchData()
}

const handleViewDetail = (row: ReviewItem) => {
  currentItem.value = row
  dialogVisible.value = true
}

const handleReview = (row: ReviewItem) => {
  currentItem.value = row
  form.rejectReason = ''
  dialogVisible.value = true
}

const handleApprove = async () => {
  if (!currentItem.value) return
  
  dialogLoading.value = true
  
  try {
    await new Promise<ReviewResponse>((resolve) => {
      setTimeout(() => {
        resolve({ code: 0, message: '审核通过成功' })
      }, 500)
    })
    
    const index = tableData.value.findIndex(item => item.taskId === currentItem.value?.taskId)
    if (index !== -1) {
      tableData.value[index].status = 'approved'
    }
    
    ElMessage.success('审核通过')
    dialogVisible.value = false
  } catch {
    ElMessage.error('审核通过失败')
  } finally {
    dialogLoading.value = false
  }
}

const handleReject = async () => {
  if (!currentItem.value) return
  
  await formRef.value?.validate((valid) => {
    if (!valid) return
  })
  
  dialogLoading.value = true
  
  try {
    await new Promise<ReviewResponse>((resolve) => {
      setTimeout(() => {
        resolve({ code: 0, message: '驳回成功' })
      }, 500)
    })
    
    const index = tableData.value.findIndex(item => item.taskId === currentItem.value?.taskId)
    if (index !== -1) {
      tableData.value[index].status = 'rejected'
    }
    
    ElMessage.success('已驳回')
    dialogVisible.value = false
  } catch {
    ElMessage.error('驳回失败')
  } finally {
    dialogLoading.value = false
  }
}

const handleClose = () => {
  dialogVisible.value = false
  form.rejectReason = ''
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">审核工作台</h1>
        <p class="page-subtitle">审核农户提交的病害识别结果</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-card__bar stat-card__bar--warning"></div>
        <div class="stat-card__content">
          <div class="stat-card__icon stat-card__icon--warning">
            <el-icon :size="22"><Clock /></el-icon>
          </div>
          <div class="stat-card__info">
            <span class="stat-card__value">{{ pendingCount }}</span>
            <span class="stat-card__label">待审核</span>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__bar stat-card__bar--success"></div>
        <div class="stat-card__content">
          <div class="stat-card__icon stat-card__icon--success">
            <el-icon :size="22"><CircleCheck /></el-icon>
          </div>
          <div class="stat-card__info">
            <span class="stat-card__value">{{ approvedCount }}</span>
            <span class="stat-card__label">已通过</span>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__bar stat-card__bar--danger"></div>
        <div class="stat-card__content">
          <div class="stat-card__icon stat-card__icon--danger">
            <el-icon :size="22"><CircleClose /></el-icon>
          </div>
          <div class="stat-card__info">
            <span class="stat-card__value">{{ rejectedCount }}</span>
            <span class="stat-card__label">已驳回</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-card">
      <div class="filter-bar">
        <ElSelect
          v-model="statusFilter"
          placeholder="请选择状态"
          class="filter-select"
          @change="handleFilterChange"
        >
          <ElOption v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
        </ElSelect>
        
        <ElDatePicker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="filter-date"
          @change="handleFilterChange"
        />
        
        <ElButton class="filter-reset-btn" @click="handleReset">
          <el-icon><RefreshRight /></el-icon>
          重置
        </ElButton>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <ElTable
        :data="filteredData"
        :loading="loading"
        class="custom-table"
        :header-cell-style="{ background: 'var(--color-bg-page)', color: 'var(--color-text-secondary)', fontWeight: '600', borderBottom: 'none' }"
        :cell-style="{ borderBottom: '1px solid var(--color-border-light)' }"
      >
        <ElTableColumn prop="taskId" label="任务 ID" min-width="170">
          <template #default="{ row }">
            <span class="task-id">{{ row.taskId }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="farmerName" label="农户名称" min-width="100" />
        <ElTableColumn prop="fieldName" label="地块名称" min-width="100" />
        <ElTableColumn prop="diseaseName" label="病害识别结果" min-width="130">
          <template #default="{ row }">
            <span class="disease-name">{{ row.diseaseName }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="confidence" label="置信度" min-width="160">
          <template #default="{ row }">
            <div class="confidence-bar">
              <div class="confidence-bar__track">
                <div
                  class="confidence-bar__fill"
                  :style="{
                    width: Math.round(row.confidence * 100) + '%',
                    backgroundColor: getConfidenceColor(row.confidence)
                  }"
                ></div>
              </div>
              <span class="confidence-bar__text" :style="{ color: getConfidenceColor(row.confidence) }">
                {{ Math.round(row.confidence * 100) }}%
              </span>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="submitTime" label="提交时间" min-width="170">
          <template #default="{ row }">
            <span class="time-text">{{ row.submitTime }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <ElTag :type="getStatusTag(row.status).type" size="small" effect="light">
              {{ getStatusTag(row.status).label }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <ElButton type="primary" link size="small" @click="handleViewDetail(row as ReviewItem)">查看详情</ElButton>
            <ElButton
              v-if="(row as ReviewItem).status === 'pending'"
              type="success"
              link
              size="small"
              @click="handleReview(row as ReviewItem)"
            >
              审核
            </ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
    </div>

    <!-- 审核对话框 -->
    <ElDialog
      v-if="currentItem"
      v-model="dialogVisible"
      width="640px"
      :show-close="true"
      class="review-dialog"
      @close="handleClose"
    >
      <template #header>
        <div class="dialog-header">
          <span class="dialog-title">审核 - {{ currentItem.taskId }}</span>
          <ElTag :type="getStatusTag(currentItem.status).type" size="small" effect="light">
            {{ getStatusTag(currentItem.status).label }}
          </ElTag>
        </div>
      </template>

      <div class="dialog-body">
        <!-- 识别详情 -->
        <div class="dialog-section">
          <div class="section-title">
            <div class="section-title__bar"></div>
            <span>识别详情</span>
          </div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">农户名称</span>
              <span class="detail-value">{{ currentItem.farmerName }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">地块名称</span>
              <span class="detail-value">{{ currentItem.fieldName }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">病害名称</span>
              <span class="detail-value detail-value--highlight">{{ currentItem.diseaseName }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">置信度</span>
              <div class="detail-confidence">
                <div class="confidence-bar">
                  <div class="confidence-bar__track">
                    <div
                      class="confidence-bar__fill"
                      :style="{
                        width: Math.round(currentItem.confidence * 100) + '%',
                        backgroundColor: getConfidenceColor(currentItem.confidence)
                      }"
                    ></div>
                  </div>
                  <span class="confidence-bar__text" :style="{ color: getConfidenceColor(currentItem.confidence) }">
                    {{ Math.round(currentItem.confidence * 100) }}%
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="detail-full">
            <span class="detail-label">防治建议</span>
            <div class="treatment-box">{{ currentItem.treatment }}</div>
          </div>
        </div>

        <!-- 审核操作 -->
        <div v-if="currentItem.status === 'pending'" class="dialog-section dialog-section--review">
          <div class="section-title">
            <div class="section-title__bar section-title__bar--primary"></div>
            <span>审核操作</span>
          </div>
          <ElForm ref="formRef" :model="form">
            <ElFormItem
              label="驳回理由"
              prop="rejectReason"
              :rules="[{ required: true, message: '请输入驳回理由', trigger: 'blur' }]"
            >
              <ElInput v-model="form.rejectReason" type="textarea" :rows="3" placeholder="请输入驳回理由（驳回时必填）" />
            </ElFormItem>
          </ElForm>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="handleClose">取消</ElButton>
          <template v-if="currentItem.status === 'pending'">
            <ElButton type="danger" :loading="dialogLoading" @click="handleReject" class="reject-btn">驳回</ElButton>
            <ElButton type="primary" :loading="dialogLoading" @click="handleApprove" class="approve-btn">通过</ElButton>
          </template>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.stat-card {
  display: flex;
  align-items: stretch;
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  transition: box-shadow var(--transition-normal);
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
}

.stat-card__bar {
  width: 4px;
  border-radius: 2px;
  flex-shrink: 0;
}

.stat-card__bar--warning {
  background: var(--color-warning);
}

.stat-card__bar--success {
  background: var(--color-success);
}

.stat-card__bar--danger {
  background: var(--color-danger);
}

.stat-card__content {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg) var(--spacing-lg) var(--spacing-lg) var(--spacing-md);
  flex: 1;
}

.stat-card__icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card__icon--warning {
  background: rgba(250, 173, 20, 0.1);
  color: var(--color-warning);
}

.stat-card__icon--success {
  background: rgba(82, 196, 26, 0.1);
  color: var(--color-success);
}

.stat-card__icon--danger {
  background: rgba(245, 34, 45, 0.1);
  color: var(--color-danger);
}

.stat-card__info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.stat-card__value {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1;
}

.stat-card__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 筛选栏 */
.filter-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.filter-select {
  width: 160px;
}

.filter-date {
  width: 320px;
}

.filter-reset-btn {
  margin-left: auto;
}

/* 表格 */
.table-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.custom-table {
  --el-table-border-color: transparent;
  --el-table-header-bg-color: var(--color-bg-page);
}

.custom-table :deep(.el-table__row:hover > td) {
  background-color: var(--color-bg-hover) !important;
}

.custom-table :deep(.el-table__row) {
  transition: background-color var(--transition-fast);
}

.task-id {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.disease-name {
  font-weight: 600;
  color: var(--color-primary);
}

.time-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 置信度进度条 */
.confidence-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.confidence-bar__track {
  flex: 1;
  height: 8px;
  background: var(--color-border-light);
  border-radius: 4px;
  overflow: hidden;
}

.confidence-bar__fill {
  height: 100%;
  border-radius: 4px;
  transition: width var(--transition-normal);
}

.confidence-bar__text {
  font-size: var(--font-size-sm);
  font-weight: 600;
  min-width: 36px;
  text-align: right;
}

/* 对话框 */
.review-dialog :deep(.el-dialog__header) {
  padding: var(--spacing-lg) var(--spacing-lg) var(--spacing-md);
  border-bottom: 1px solid var(--color-border-light);
  margin-right: 0;
}

.review-dialog :deep(.el-dialog__body) {
  padding: var(--spacing-md) var(--spacing-lg);
}

.review-dialog :deep(.el-dialog__footer) {
  padding: var(--spacing-md) var(--spacing-lg) var(--spacing-lg);
  border-top: 1px solid var(--color-border-light);
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.dialog-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text-primary);
}

.dialog-body {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.dialog-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.dialog-section--review {
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--color-border-light);
}

.section-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text-primary);
}

.section-title__bar {
  width: 4px;
  height: 20px;
  border-radius: 2px;
  background: var(--color-warning);
}

.section-title__bar--primary {
  background: var(--color-primary);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-md);
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.detail-value {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  font-weight: 500;
}

.detail-value--highlight {
  color: var(--color-primary);
  font-weight: 600;
}

.detail-confidence {
  display: flex;
  align-items: center;
}

.detail-full {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.treatment-box {
  background: var(--color-primary-lighter);
  padding: var(--spacing-md);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-base);
  line-height: 1.8;
  color: var(--color-text-primary);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
}

.approve-btn {
  background-color: var(--color-primary) !important;
  border-color: var(--color-primary) !important;
  min-width: 80px;
}

.approve-btn:hover {
  background-color: var(--color-primary-light) !important;
  border-color: var(--color-primary-light) !important;
}

.reject-btn {
  min-width: 80px;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    flex-wrap: wrap;
  }

  .filter-select,
  .filter-date {
    width: 100%;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
