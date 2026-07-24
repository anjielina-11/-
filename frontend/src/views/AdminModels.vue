<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem,
  ElInput, ElSelect, ElOption, ElMessage, ElTag, ElIcon
} from 'element-plus'
import { Edit, Delete, Upload, Loading, CircleClose, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/utils/request'
import { percentToRatio, ratioToPercent } from '@/utils/domainMappers'

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

const models = ref<ModelVersion[]>([])
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
const typeColors: Record<string, string> = { classification: '', detection: 'success', segmentation: 'warning' }
const statusLabels: Record<string, string> = { training: '训练中', deployed: '已部署', deprecated: '已废弃' }
const statusColors: Record<string, string> = { training: 'warning', deployed: 'success', deprecated: 'info' }

const deployedCount = computed(() => models.value.filter(m => m.status === 'deployed').length)
const trainingCount = computed(() => models.value.filter(m => m.status === 'training').length)
const deprecatedCount = computed(() => models.value.filter(m => m.status === 'deprecated').length)

const avgAccuracy = computed(() => {
  const deployed = models.value.filter(m => m.status === 'deployed')
  if (deployed.length === 0) return 0
  return deployed.reduce((sum, m) => sum + m.accuracy, 0) / deployed.length
})

const getAccuracyColor = (accuracy: number): string => {
  if (accuracy >= 95) return 'var(--color-success)'
  if (accuracy >= 90) return 'var(--color-primary)'
  if (accuracy >= 80) return 'var(--color-warning)'
  return 'var(--color-danger)'
}

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

const loadModels = async () => {
  try {
    const page = await request.get<{ list: Array<{
      id: string
      modelName: string
      modelType: ModelVersion['type']
      version: string
      status: ModelVersion['status']
      accuracy: number
      recallVal: number
      f1Score: number
      createdAt: string
      description?: string
    }>; total: number }>('/model-versions?size=50')
    models.value = page.list.map(item => ({
      id: item.id,
      name: item.modelName,
      version: item.version,
      type: item.modelType,
      status: item.status,
      accuracy: ratioToPercent(item.accuracy),
      recall: ratioToPercent(item.recallVal),
      f1Score: ratioToPercent(item.f1Score),
      trainDate: item.createdAt,
      description: item.description
    }))
    updateChart()
  } catch {
    ElMessage.error('获取模型列表失败')
  }
}

const handleEdit = (row: ModelVersion) => {
  editMode.value = true
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (id: string) => {
  try {
    await request.delete('/model-versions/' + id)
    models.value = models.value.filter(m => m.id !== id)
    ElMessage.success('删除成功')
    updateChart()
  } catch {
    ElMessage.error('删除失败')
  }
}

const handleSubmit = async () => {
  try {
    const payload = {
      modelName: formData.value.name,
      modelType: formData.value.type,
      version: formData.value.version,
      accuracy: percentToRatio(formData.value.accuracy),
      precisionVal: percentToRatio(formData.value.recall),
      recallVal: percentToRatio(formData.value.recall),
      f1Score: percentToRatio(formData.value.f1Score),
      description: formData.value.description,
      status: formData.value.status
    }
    if (editMode.value) {
      await request.put('/model-versions/' + formData.value.id, payload)
      ElMessage.success('修改成功')
    } else {
      await request.post('/model-versions', payload)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadModels()
  } catch {
    ElMessage.error(editMode.value ? '修改失败' : '添加失败')
  }
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
    title: {
      text: '模型性能对比',
      left: 'center',
      top: 8,
      textStyle: {
        fontSize: 15,
        fontWeight: 600,
        color: 'var(--color-text-primary)'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: 'var(--color-border)',
      borderWidth: 1,
      textStyle: {
        color: 'var(--color-text-primary)',
        fontSize: 13
      }
    },
    legend: {
      data: ['准确率', '召回率', 'F1分数'],
      bottom: 8,
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 20,
      textStyle: {
        color: 'var(--color-text-secondary)',
        fontSize: 12
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      top: '18%',
      bottom: '16%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: deployedModels.map(m => `${m.name}(${m.version})`),
      axisLabel: {
        color: 'var(--color-text-secondary)',
        fontSize: 11,
        rotate: deployedModels.length > 3 ? 15 : 0
      },
      axisLine: {
        lineStyle: { color: 'var(--color-border)' }
      },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: {
        color: 'var(--color-text-secondary)',
        fontSize: 11
      },
      splitLine: {
        lineStyle: {
          color: 'var(--color-border-light)',
          type: 'dashed'
        }
      },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      {
        name: '准确率',
        type: 'bar',
        barWidth: 16,
        data: deployedModels.map(m => m.accuracy),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#3A9D5C' },
            { offset: 1, color: '#2D7D46' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      },
      {
        name: '召回率',
        type: 'bar',
        barWidth: 16,
        data: deployedModels.map(m => m.recall),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#6DD58C' },
            { offset: 1, color: '#52C41A' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      },
      {
        name: 'F1分数',
        type: 'bar',
        barWidth: 16,
        data: deployedModels.map(m => m.f1Score),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#FFD666' },
            { offset: 1, color: '#FAAD14' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  })
}

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  loadModels()
  nextTick(() => {
    initChart()
  })
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">模型版本管理</h1>
        <p class="page-subtitle">管理AI模型版本和性能监控</p>
      </div>
    </div>

    <!-- 统计卡片行 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-indicator stat-indicator--success"></div>
        <div class="stat-content">
          <div class="stat-value">{{ deployedCount }}</div>
          <div class="stat-label">已部署</div>
        </div>
        <div class="stat-icon stat-icon--success">
          <ElIcon :size="28"><Upload /></ElIcon>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-indicator stat-indicator--warning"></div>
        <div class="stat-content">
          <div class="stat-value">{{ trainingCount }}</div>
          <div class="stat-label">训练中</div>
        </div>
        <div class="stat-icon stat-icon--warning">
          <ElIcon :size="28"><Loading /></ElIcon>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-indicator stat-indicator--info"></div>
        <div class="stat-content">
          <div class="stat-value">{{ deprecatedCount }}</div>
          <div class="stat-label">已废弃</div>
        </div>
        <div class="stat-icon stat-icon--info">
          <ElIcon :size="28"><CircleClose /></ElIcon>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-indicator stat-indicator--primary"></div>
        <div class="stat-content">
          <div class="stat-value">{{ avgAccuracy.toFixed(1) }}<span class="stat-unit">%</span></div>
          <div class="stat-label">平均准确率</div>
        </div>
        <div class="stat-icon stat-icon--primary">
          <ElIcon :size="28"><TrendCharts /></ElIcon>
        </div>
      </div>
    </div>

    <!-- 内容区：左侧图表 + 右侧表格 -->
    <div class="content-row">
      <!-- 图表卡片 -->
      <div class="chart-card">
        <div ref="chartRef" class="chart"></div>
      </div>

      <!-- 表格卡片 -->
      <div class="table-card">
        <div class="table-card-header">
          <h3 class="table-card-title">模型列表</h3>
          <ElButton type="primary" :icon="Edit" size="small" @click="handleAdd">添加模型</ElButton>
        </div>
        <ElTable :data="models" style="width: 100%" :header-cell-style="{ background: 'var(--color-bg-page)', color: 'var(--color-text-secondary)', fontWeight: '600' }">
          <ElTableColumn prop="name" label="模型名称" min-width="130">
            <template #default="{ row }">
              <span class="cell-text-primary">{{ row.name }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="version" label="版本" min-width="80">
            <template #default="{ row }">
              <span class="version-tag">{{ row.version }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="type" label="类型" min-width="80" align="center">
            <template #default="{ row }">
              <ElTag :type="(typeColors[row.type] as any)" size="small" effect="light">{{ typeLabels[row.type] }}</ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="status" label="状态" min-width="80" align="center">
            <template #default="{ row }">
              <ElTag :type="(statusColors[row.status] as any)" size="small" effect="light">{{ statusLabels[row.status] }}</ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="accuracy" label="准确率" min-width="140">
            <template #default="{ row }">
              <div v-if="row.status !== 'training'" class="accuracy-bar-wrapper">
                <div class="accuracy-bar">
                  <div
                    class="accuracy-bar-fill"
                    :style="{
                      width: row.accuracy + '%',
                      backgroundColor: getAccuracyColor(row.accuracy)
                    }"
                  ></div>
                </div>
                <span class="accuracy-text">{{ row.accuracy }}%</span>
              </div>
              <span v-else class="cell-text-secondary">-</span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="trainDate" label="训练日期" min-width="100">
            <template #default="{ row }">
              <span class="cell-text-secondary">{{ row.trainDate }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" min-width="100" fixed="right" align="center">
            <template #default="{ row }">
              <div class="action-btns">
                <ElButton type="primary" link :icon="Edit" @click="handleEdit(row as ModelVersion)" />
                <ElButton type="danger" link :icon="Delete" @click="handleDelete((row as ModelVersion).id)" />
              </div>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
    </div>

    <!-- 添加/编辑对话框 -->
    <ElDialog
      :title="editMode ? '编辑模型' : '添加模型'"
      v-model="dialogVisible"
      width="600px"
      :close-on-click-modal="false"
      class="custom-dialog"
    >
      <ElForm :model="formData" label-width="90px" label-position="right" class="dialog-form">
        <ElFormItem label="模型名称" required>
          <ElInput v-model="formData.name" placeholder="请输入模型名称" />
        </ElFormItem>
        <ElFormItem label="版本号" required>
          <ElInput v-model="formData.version" placeholder="如 v1.0.0" />
        </ElFormItem>
        <ElFormItem label="模型类型" required>
          <ElSelect v-model="formData.type" placeholder="请选择类型" style="width: 100%">
            <ElOption label="分类" value="classification" />
            <ElOption label="检测" value="detection" />
            <ElOption label="分割" value="segmentation" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="状态">
          <ElSelect v-model="formData.status" style="width: 100%">
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
        <div class="dialog-footer">
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleSubmit">确定</ElButton>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: var(--spacing-lg);
  min-height: calc(100vh - var(--header-height));
  background: var(--color-bg-page);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.page-title {
  font-size: var(--font-size-2xl);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.page-subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: var(--spacing-xs);
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.stat-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--spacing-lg);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  position: relative;
  overflow: hidden;
  transition: box-shadow var(--transition-normal), transform var(--transition-normal);
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.stat-indicator {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: 0 4px 4px 0;
}

.stat-indicator--success {
  background: var(--color-success);
}

.stat-indicator--warning {
  background: var(--color-warning);
}

.stat-indicator--info {
  background: var(--color-info);
}

.stat-indicator--primary {
  background: var(--color-primary);
}

.stat-content {
  flex: 1;
  padding-left: var(--spacing-sm);
}

.stat-value {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.stat-unit {
  font-size: var(--font-size-lg);
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-left: 2px;
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: var(--spacing-xs);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon--success {
  background: rgba(82, 196, 26, 0.1);
  color: var(--color-success);
}

.stat-icon--warning {
  background: rgba(250, 173, 20, 0.1);
  color: var(--color-warning);
}

.stat-icon--info {
  background: rgba(24, 144, 255, 0.1);
  color: var(--color-info);
}

.stat-icon--primary {
  background: rgba(45, 125, 70, 0.1);
  color: var(--color-primary);
}

/* 内容区布局 */
.content-row {
  display: flex;
  gap: var(--spacing-md);
  align-items: flex-start;
}

/* 图表卡片 */
.chart-card {
  width: 420px;
  flex-shrink: 0;
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--spacing-md);
  transition: box-shadow var(--transition-normal);
}

.chart-card:hover {
  box-shadow: var(--shadow-md);
}

.chart {
  width: 100%;
  height: 360px;
}

/* 表格卡片 */
.table-card {
  flex: 1;
  min-width: 0;
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--spacing-md);
  transition: box-shadow var(--transition-normal);
}

.table-card:hover {
  box-shadow: var(--shadow-md);
}

.table-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.table-card-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

/* 表格自定义样式 */
.table-card :deep(.el-table) {
  --el-table-border-color: var(--color-border-light);
  --el-table-header-bg-color: var(--color-bg-page);
  --el-table-row-hover-bg-color: var(--color-bg-hover);
}

.table-card :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.table-card :deep(.el-table__row) {
  transition: background-color var(--transition-fast);
}

.table-card :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid var(--color-border-light);
}

.table-card :deep(.el-table th.el-table__cell) {
  border-bottom: 1px solid var(--color-border);
}

/* 单元格文字 */
.cell-text-primary {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  font-weight: 500;
}

.cell-text-secondary {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
}

/* 版本标签 */
.version-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  background: var(--color-primary-lighter);
  border-radius: 4px;
  font-weight: 500;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

/* 准确率进度条 */
.accuracy-bar-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.accuracy-bar {
  flex: 1;
  height: 8px;
  background: var(--color-border-light);
  border-radius: 4px;
  overflow: hidden;
}

.accuracy-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width var(--transition-normal);
}

.accuracy-text {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  font-weight: 500;
  min-width: 40px;
  text-align: right;
}

/* 操作按钮组 */
.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
}

/* 对话框 */
.dialog-form {
  padding: var(--spacing-sm) var(--spacing-md) 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
}
</style>
