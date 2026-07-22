<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElCard, ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElMessage, ElTag, ElProgress } from 'element-plus'
import * as echarts from 'echarts'

interface ModelVersion {
  id: string
  name: string
  version: string
  type: 'classification' | 'detection' | 'segmentation'
  status: 'training' | 'deployed' | 'deprecated'
  accuracy: number
  recall: number
  f1Score: number
  trainDate: string
  description?: string
}

const mockModels: ModelVersion[] = [
  { id: '1', name: '病害识别模型', version: 'v2.1.0', type: 'classification', status: 'deployed', accuracy: 96.5, recall: 95.8, f1Score: 96.1, trainDate: '2026-07-15', description: '支持20种常见农作物病害识别' },
  { id: '2', name: '害虫检测模型', version: 'v1.3.0', type: 'detection', status: 'deployed', accuracy: 94.2, recall: 93.5, f1Score: 93.8, trainDate: '2026-06-20', description: '检测15种主要农业害虫' },
  { id: '3', name: '病害识别模型', version: 'v2.0.0', type: 'classification', status: 'deprecated', accuracy: 92.8, recall: 91.5, f1Score: 92.1, trainDate: '2026-03-10', description: '旧版本，已被v2.1.0替代' },
  { id: '4', name: '作物分类模型', version: 'v1.0.0', type: 'classification', status: 'training', accuracy: 0, recall: 0, f1Score: 0, trainDate: '2026-07-22', description: '训练中...预计2天完成' },
  { id: '5', name: '杂草识别模型', version: 'v1.1.0', type: 'segmentation', status: 'deployed', accuracy: 89.5, recall: 88.2, f1Score: 88.8, trainDate: '2026-05-05', description: '支持10种常见杂草识别' }
]

const models = ref<ModelVersion[]>([...mockModels])
const dialogVisible = ref(false)
const editMode = ref(false)
const formData = ref<ModelVersion>({
  id: '',
  name: '',
  version: '',
  type: 'classification',
  status: 'training',
  accuracy: 0,
  recall: 0,
  f1Score: 0,
  trainDate: '',
  description: ''
})

const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const typeLabels: Record<string, string> = { classification: '分类', detection: '检测', segmentation: '分割' }
const typeColors: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = { classification: 'primary', detection: 'success', segmentation: 'warning' }
const statusColors: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = { training: 'warning', deployed: 'success', deprecated: 'info' }

const deployedCount = computed(() => models.value.filter(m => m.status === 'deployed').length)
const trainingCount = computed(() => models.value.filter(m => m.status === 'training').length)
const deprecatedCount = computed(() => models.value.filter(m => m.status === 'deprecated').length)

const avgAccuracy = computed(() => {
  const deployed = models.value.filter(m => m.status === 'deployed')
  if (deployed.length === 0) return 0
  return deployed.reduce((sum, m) => sum + m.accuracy, 0) / deployed.length
})

const handleAdd = () => {
  editMode.value = false
  formData.value = {
    id: '',
    name: '',
    version: '',
    type: 'classification',
    status: 'training',
    accuracy: 0,
    recall: 0,
    f1Score: 0,
    trainDate: '',
    description: ''
  }
  dialogVisible.value = true
}

const handleEdit = (row: ModelVersion) => {
  editMode.value = true
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = (id: string) => {
  models.value = models.value.filter(m => m.id !== id)
  ElMessage.success('删除成功')
  updateChart()
}

const handleSubmit = () => {
  const now = new Date().toISOString().split('T')[0]
  if (editMode.value) {
    const index = models.value.findIndex(m => m.id === formData.value.id)
    if (index !== -1) {
      models.value[index] = { ...formData.value }
      ElMessage.success('修改成功')
    }
  } else {
    formData.value.id = String(Date.now())
    formData.value.trainDate = now
    models.value.unshift({ ...formData.value })
    ElMessage.success('添加成功')
  }
  dialogVisible.value = false
  updateChart()
}

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return
  const deployedModels = models.value.filter(m => m.status === 'deployed')
  
  chartInstance.setOption({
    title: { text: '模型性能对比', left: 'center', textStyle: { fontSize: 16 } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['准确率', '召回率', 'F1分数'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', data: deployedModels.map(m => `${m.name}(${m.version})`) },
    yAxis: { type: 'value', max: 100 },
    series: [
      { name: '准确率', type: 'bar', data: deployedModels.map(m => m.accuracy), itemStyle: { color: '#409eff' } },
      { name: '召回率', type: 'bar', data: deployedModels.map(m => m.recall), itemStyle: { color: '#67c23a' } },
      { name: 'F1分数', type: 'bar', data: deployedModels.map(m => m.f1Score), itemStyle: { color: '#e6a23c' } }
    ]
  })
}

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<template>
  <div class="admin-models-container">
    <div class="stats-row">
      <ElCard class="stat-card">
        <div class="stat-info">
          <div class="stat-value">{{ deployedCount }}</div>
          <div class="stat-label">已部署</div>
        </div>
      </ElCard>
      <ElCard class="stat-card">
        <div class="stat-info">
          <div class="stat-value">{{ trainingCount }}</div>
          <div class="stat-label">训练中</div>
        </div>
      </ElCard>
      <ElCard class="stat-card">
        <div class="stat-info">
          <div class="stat-value">{{ deprecatedCount }}</div>
          <div class="stat-label">已废弃</div>
        </div>
      </ElCard>
      <ElCard class="stat-card">
        <div class="stat-info">
          <div class="stat-value">{{ avgAccuracy.toFixed(1) }}%</div>
          <div class="stat-label">平均准确率</div>
        </div>
      </ElCard>
    </div>

    <div class="content-row">
      <ElCard class="chart-card">
        <div ref="chartRef" class="chart"></div>
      </ElCard>

      <ElCard class="table-card" header="模型版本管理">
        <div class="card-header">
          <ElButton type="primary" @click="handleAdd">添加模型</ElButton>
        </div>
        <ElTable :data="models" border style="width: 100%">
          <ElTableColumn prop="name" label="模型名称" min-width="150" />
          <ElTableColumn prop="version" label="版本号" min-width="100" />
          <ElTableColumn prop="type" label="模型类型" min-width="120">
            <template #default="{ row }">
              <ElTag :type="typeColors[row.type]">{{ typeLabels[row.type] }}</ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="status" label="状态" min-width="100">
            <template #default="{ row }">
              <ElTag :type="statusColors[row.status]">{{ row.status === 'training' ? '训练中' : row.status === 'deployed' ? '已部署' : '已废弃' }}</ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="accuracy" label="准确率" min-width="100">
            <template #default="{ row }">
              <ElProgress v-if="row.status !== 'training'" :percentage="row.accuracy" :stroke-width="12" :text-inside="true" />
              <span v-else>-</span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="recall" label="召回率" min-width="100">
            <template #default="{ row }">{{ row.status !== 'training' ? `${row.recall}%` : '-' }}</template>
          </ElTableColumn>
          <ElTableColumn prop="f1Score" label="F1分数" min-width="100">
            <template #default="{ row }">{{ row.status !== 'training' ? `${row.f1Score}%` : '-' }}</template>
          </ElTableColumn>
          <ElTableColumn prop="trainDate" label="训练日期" min-width="120" />
          <ElTableColumn label="操作" min-width="150" fixed="right">
            <template #default="{ row }">
            <ElButton type="primary" link @click="handleEdit(row as ModelVersion)">编辑</ElButton>
            <ElButton type="danger" link @click="handleDelete((row as ModelVersion).id)">删除</ElButton>
          </template>
          </ElTableColumn>
        </ElTable>
      </ElCard>
    </div>

    <ElDialog :title="editMode ? '编辑模型' : '添加模型'" v-model="dialogVisible" width="600px">
      <ElForm :model="formData" label-width="100px">
        <ElFormItem label="模型名称" required>
          <ElInput v-model="formData.name" placeholder="请输入模型名称" />
        </ElFormItem>
        <ElFormItem label="版本号" required>
          <ElInput v-model="formData.version" placeholder="如 v1.0.0" />
        </ElFormItem>
        <ElFormItem label="模型类型" required>
          <ElSelect v-model="formData.type" placeholder="请选择类型">
            <ElOption label="分类" value="classification" />
            <ElOption label="检测" value="detection" />
            <ElOption label="分割" value="segmentation" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="状态">
          <ElSelect v-model="formData.status">
            <ElOption label="训练中" value="training" />
            <ElOption label="已部署" value="deployed" />
            <ElOption label="已废弃" value="deprecated" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="准确率">
          <ElInput v-model.number="formData.accuracy" placeholder="0-100" />
        </ElFormItem>
        <ElFormItem label="召回率">
          <ElInput v-model.number="formData.recall" placeholder="0-100" />
        </ElFormItem>
        <ElFormItem label="F1分数">
          <ElInput v-model.number="formData.f1Score" placeholder="0-100" />
        </ElFormItem>
        <ElFormItem label="描述">
          <ElInput v-model="formData.description" type="textarea" :rows="3" placeholder="请输入模型描述" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.admin-models-container {
  padding: 20px;
  background: #fafafa;
  min-height: calc(100vh - 60px);
}

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
}

.stat-info {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.content-row {
  display: flex;
  gap: 20px;
}

.chart-card {
  width: 450px;
  flex-shrink: 0;
}

.chart {
  width: 100%;
  min-height: 350px;
}

.table-card {
  flex: 1;
}

.card-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>