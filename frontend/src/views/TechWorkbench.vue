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
  <div class="tech-workbench-container">
    <div class="filter-bar">
      <ElSelect
        v-model="statusFilter"
        placeholder="请选择状态"
        style="width: 150px"
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
        style="width: 300px"
        @change="handleFilterChange"
      />
      
      <ElButton @click="handleReset">重置</ElButton>
    </div>

    <ElTable
      :data="filteredData"
      :loading="loading"
      border
      style="width: 100%"
    >
      <ElTableColumn prop="taskId" label="任务 ID" width="180" />
      <ElTableColumn prop="farmerName" label="农户名称" width="120" />
      <ElTableColumn prop="fieldName" label="地块名称" width="120" />
      <ElTableColumn prop="diseaseName" label="病害识别结果" width="150" />
      <ElTableColumn prop="confidence" label="置信度" width="120">
        <template #default="{ row }">
          <ElProgress :percentage="Math.round(row.confidence * 100)" :stroke-width="8" :show-text="true" />
        </template>
      </ElTableColumn>
      <ElTableColumn prop="submitTime" label="提交时间" width="180" />
      <ElTableColumn prop="status" label="状态" width="120">
        <template #default="{ row }">
          <ElTag :type="getStatusTag(row.status).type">{{ getStatusTag(row.status).label }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="200">
        <template #default="{ row }">
          <ElButton type="primary" link @click="handleViewDetail(row as ReviewItem)">查看详情</ElButton>
          <ElButton
            v-if="(row as ReviewItem).status === 'pending'"
            type="success"
            link
            @click="handleReview(row as ReviewItem)"
          >
            审核
          </ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <ElDialog
      v-if="currentItem"
      :title="`审核 - ${currentItem.taskId}`"
      v-model="dialogVisible"
      width="600px"
      @close="handleClose"
    >
      <div class="dialog-content">
        <div class="detail-section">
          <h4>识别详情</h4>
          <div class="detail-item">
            <span class="label">农户名称：</span>
            <span class="value">{{ currentItem.farmerName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">地块名称：</span>
            <span class="value">{{ currentItem.fieldName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">病害名称：</span>
            <span class="value">{{ currentItem.diseaseName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">置信度：</span>
            <span class="value">
              <ElProgress :percentage="Math.round(currentItem.confidence * 100)" :stroke-width="6" :show-text="true" style="width: 150px" />
            </span>
          </div>
          <div class="detail-item">
            <span class="label">防治建议：</span>
            <span class="value treatment-text">{{ currentItem.treatment }}</span>
          </div>
        </div>

        <div v-if="currentItem.status === 'pending'" class="review-section">
          <h4>审核操作</h4>
          <ElForm ref="formRef" :model="form">
            <ElFormItem
              label="驳回理由"
              prop="rejectReason"
              :rules="[{ required: true, message: '请输入驳回理由', trigger: 'blur' }]"
            >
              <ElInput v-model="form.rejectReason" type="textarea" :rows="3" placeholder="请输入驳回理由" />
            </ElFormItem>
          </ElForm>
        </div>
      </div>

      <template #footer>
        <ElButton @click="handleClose">取消</ElButton>
        <template v-if="currentItem.status === 'pending'">
          <ElButton type="danger" :loading="dialogLoading" @click="handleReject">驳回</ElButton>
          <ElButton type="success" :loading="dialogLoading" @click="handleApprove">通过</ElButton>
        </template>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.tech-workbench-container {
  padding: 20px;
  background: #fafafa;
  min-height: calc(100vh - 60px);
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.dialog-content {
  max-height: 500px;
  overflow-y: auto;
}

.detail-section {
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.detail-section h4 {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.detail-item {
  display: flex;
  margin-bottom: 12px;
}

.detail-item .label {
  width: 100px;
  color: #606266;
  flex-shrink: 0;
}

.detail-item .value {
  color: #303133;
}

.treatment-text {
  display: block;
  white-space: pre-wrap;
  line-height: 1.6;
}

.review-section {
  margin-top: 20px;
}

.review-section h4 {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

:deep(.el-progress) {
  display: inline-block;
}
</style>