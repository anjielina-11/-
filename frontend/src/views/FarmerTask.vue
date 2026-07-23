<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElCard, ElTable, ElTableColumn, ElDialog, ElButton, ElTag, ElMessage } from 'element-plus'
import * as echarts from 'echarts'

interface Task {
  id: string
  title: string
  fieldName: string
  dueDate: string
  status: '待执行' | '执行中' | '已完成'
  description?: string
}

const mockTasks: Task[] = [
  { id: '1', title: '喷洒三环唑', fieldName: '水稻田A', dueDate: '2026-07-25', status: '待执行', description: '针对稻瘟病进行预防性喷洒，每亩用量200毫升' },
  { id: '2', title: '除草作业', fieldName: '玉米地B', dueDate: '2026-07-26', status: '待执行', description: '清除田间杂草，确保作物正常生长' },
  { id: '3', title: '施肥管理', fieldName: '蔬菜基地C', dueDate: '2026-07-24', status: '已完成', description: '施加有机肥，提高土壤肥力' },
  { id: '4', title: '病虫害巡查', fieldName: '水果园D', dueDate: '2026-07-27', status: '执行中', description: '全面巡查果园，及时发现病虫害迹象' },
  { id: '5', title: '灌溉调度', fieldName: '水稻田A', dueDate: '2026-07-23', status: '已完成', description: '根据天气情况调整灌溉水量' }
]

const tasks = ref<Task[]>([])
const dialogVisible = ref(false)
const selectedTask = ref<Task | null>(null)
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const statusColors: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
  '待执行': 'warning',
  '执行中': 'info',
  '已完成': 'success'
}

const pendingCount = computed(() => tasks.value.filter(t => t.status === '待执行').length)
const executingCount = computed(() => tasks.value.filter(t => t.status === '执行中').length)
const completedCount = computed(() => tasks.value.filter(t => t.status === '已完成').length)

const isToday = (dateStr: string) => {
  const today = new Date()
  const date = new Date(dateStr)
  return today.getFullYear() === date.getFullYear() &&
    today.getMonth() === date.getMonth() &&
    today.getDate() === date.getDate()
}

const loadTasks = () => {
  tasks.value = [...mockTasks]
}

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#E5E7EB',
      borderWidth: 1,
      textStyle: { color: '#1F2937', fontSize: 13 },
      padding: [12, 16],
      extraCssText: 'border-radius: 8px; box-shadow: 0 4px 12px rgba(45, 125, 70, 0.1);'
    },
    grid: {
      left: '3%',
      right: '4%',
      top: '16%',
      bottom: '8%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['待执行', '执行中', '已完成'],
      axisLabel: {
        fontSize: 13,
        color: '#6B7280'
      },
      axisLine: {
        lineStyle: { color: '#E5E7EB' }
      },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        fontSize: 12,
        color: '#9CA3AF'
      },
      splitLine: {
        lineStyle: { color: '#F3F4F6', type: 'dashed' }
      },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      {
        name: '任务数',
        type: 'bar',
        barWidth: '40%',
        data: [
          {
            value: pendingCount.value,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#FAAD14' },
                { offset: 1, color: '#FFD666' }
              ]),
              borderRadius: [6, 6, 0, 0]
            }
          },
          {
            value: executingCount.value,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#1890FF' },
                { offset: 1, color: '#69C0FF' }
              ]),
              borderRadius: [6, 6, 0, 0]
            }
          },
          {
            value: completedCount.value,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#2D7D46' },
                { offset: 1, color: '#52C41A' }
              ]),
              borderRadius: [6, 6, 0, 0]
            }
          }
        ],
        label: {
          show: true,
          position: 'top',
          fontSize: 14,
          fontWeight: 'bold',
          color: '#1F2937'
        }
      }
    ]
  }
  chartInstance.setOption(option)
}

const handleRowClick = (row: Task) => {
  selectedTask.value = row
  dialogVisible.value = true
}

const markAsCompleted = async () => {
  if (!selectedTask.value) return
  const index = tasks.value.findIndex(t => t.id === selectedTask.value!.id)
  if (index !== -1) {
    await new Promise(resolve => setTimeout(resolve, 500))
    tasks.value[index].status = '已完成'
    selectedTask.value!.status = '已完成'
    ElMessage.success('任务已标记为完成')
    updateChart()
  }
}

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  loadTasks()
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">农事任务</h1>
        <p class="page-subtitle">管理和跟踪您的农事任务进度</p>
      </div>
    </div>

    <!-- 统计卡片行 -->
    <div class="stats-row">
      <ElCard class="stat-card" shadow="never">
        <div class="stat-card-inner">
          <div class="stat-bar stat-bar--warning"></div>
          <div class="stat-content">
            <div class="stat-value">{{ pendingCount }}</div>
            <div class="stat-label">待执行</div>
          </div>
          <div class="stat-icon-wrap stat-icon-wrap--warning">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          </div>
        </div>
      </ElCard>

      <ElCard class="stat-card" shadow="never">
        <div class="stat-card-inner">
          <div class="stat-bar stat-bar--info"></div>
          <div class="stat-content">
            <div class="stat-value">{{ executingCount }}</div>
            <div class="stat-label">执行中</div>
          </div>
          <div class="stat-icon-wrap stat-icon-wrap--info">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          </div>
        </div>
      </ElCard>

      <ElCard class="stat-card" shadow="never">
        <div class="stat-card-inner">
          <div class="stat-bar stat-bar--success"></div>
          <div class="stat-content">
            <div class="stat-value">{{ completedCount }}</div>
            <div class="stat-label">已完成</div>
          </div>
          <div class="stat-icon-wrap stat-icon-wrap--success">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          </div>
        </div>
      </ElCard>
    </div>

    <!-- 图表卡片 -->
    <ElCard class="chart-card" shadow="never">
      <template #header>
        <div class="card-header-title">任务完成情况</div>
      </template>
      <div ref="chartRef" class="chart-container"></div>
    </ElCard>

    <!-- 任务列表卡片 -->
    <ElCard class="task-card" shadow="never">
      <template #header>
        <div class="card-header-title">任务列表</div>
      </template>
      <el-table
        :data="tasks"
        row-key="id"
        class="custom-table clickable-table"
        @row-click="handleRowClick"
      >
        <el-table-column prop="title" label="任务名称" min-width="180">
          <template #default="{ row }">
            <span class="cell-primary">{{ (row as Task).title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="fieldName" label="关联地块" min-width="130" />
        <el-table-column prop="dueDate" label="截止日期" min-width="120">
          <template #default="{ row }">
            <div class="due-date-cell">
              <span>{{ (row as Task).dueDate }}</span>
              <ElTag v-if="isToday((row as Task).dueDate)" type="danger" size="small" effect="dark" class="today-tag">今日到期</ElTag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100" align="center">
          <template #default="{ row }">
            <ElTag :type="statusColors[row.status]" size="small" effect="light">{{ row.status }}</ElTag>
          </template>
        </el-table-column>
      </el-table>
    </ElCard>

    <!-- 任务详情对话框 -->
    <ElDialog
      v-model="dialogVisible"
      title="任务详情"
      width="520px"
      :close-on-click-modal="false"
      class="custom-dialog"
    >
      <div v-if="selectedTask" class="task-detail">
        <div class="detail-section">
          <div class="detail-item">
            <span class="detail-label">任务名称</span>
            <span class="detail-value detail-value--primary">{{ selectedTask.title }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">关联地块</span>
            <span class="detail-value">{{ selectedTask.fieldName }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">截止日期</span>
            <span class="detail-value">{{ selectedTask.dueDate }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">当前状态</span>
            <ElTag :type="statusColors[selectedTask.status]" effect="light">{{ selectedTask.status }}</ElTag>
          </div>
        </div>

        <div v-if="selectedTask.description" class="detail-section detail-section--desc">
          <div class="detail-section-title">任务描述</div>
          <div class="detail-desc">{{ selectedTask.description }}</div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="dialogVisible = false">关闭</ElButton>
          <ElButton
            v-if="selectedTask && selectedTask.status !== '已完成'"
            type="primary"
            @click="markAsCompleted"
          >
            标记为已完成
          </ElButton>
          <ElButton
            v-else
            type="success"
            disabled
          >
            已完成
          </ElButton>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
/* 统计卡片行 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.stat-card :deep(.el-card__body) {
  padding: var(--spacing-md) var(--spacing-lg);
}

.stat-card-inner {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.stat-bar {
  width: 4px;
  height: 48px;
  border-radius: 4px;
  flex-shrink: 0;
}

.stat-bar--warning {
  background-color: var(--color-warning);
}

.stat-bar--info {
  background-color: var(--color-info);
}

.stat-bar--success {
  background-color: var(--color-success);
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-wrap--warning {
  background-color: #FFF8E6;
  color: var(--color-warning);
}

.stat-icon-wrap--info {
  background-color: #E6F4FF;
  color: var(--color-info);
}

.stat-icon-wrap--success {
  background-color: #E8F5EC;
  color: var(--color-success);
}

/* 图表卡片 */
.chart-card {
  margin-bottom: var(--spacing-md);
}

.chart-card :deep(.el-card__header) {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-light);
}

.chart-container {
  width: 100%;
  min-height: 280px;
}

.card-header-title {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 任务列表卡片 */
.task-card :deep(.el-card__header) {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-light);
}

.task-card :deep(.el-card__body) {
  padding: 0;
}

/* 表格 */
.custom-table {
  --el-table-border-color: var(--color-border-light);
  --el-table-header-bg-color: var(--color-bg-page);
  --el-table-row-hover-bg-color: var(--color-bg-hover);
}

.custom-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.custom-table :deep(th.el-table__cell) {
  background-color: var(--color-bg-page) !important;
  color: var(--color-text-secondary) !important;
  font-weight: 600 !important;
  font-size: var(--font-size-sm) !important;
  border-bottom: 1px solid var(--color-border) !important;
}

.custom-table :deep(td.el-table__cell) {
  border-bottom: 1px solid var(--color-border-light) !important;
  color: var(--color-text-primary);
}

.custom-table :deep(.el-table__row:last-child td.el-table__cell) {
  border-bottom: none !important;
}

.clickable-table :deep(.el-table__row) {
  cursor: pointer;
  transition: background-color var(--transition-fast);
}

.cell-primary {
  font-weight: 600;
  color: var(--color-primary);
}

.due-date-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.today-tag {
  font-size: 11px;
  padding: 0 6px;
  height: 20px;
  line-height: 20px;
}

/* 对话框 */
.custom-dialog :deep(.el-dialog) {
  border-radius: var(--radius-lg) !important;
}

.custom-dialog :deep(.el-dialog__header) {
  padding: var(--spacing-lg) var(--spacing-lg) var(--spacing-md);
  border-bottom: 1px solid var(--color-border-light);
  margin-right: 0;
}

.custom-dialog :deep(.el-dialog__title) {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text-primary);
}

.custom-dialog :deep(.el-dialog__body) {
  padding: var(--spacing-lg);
}

.custom-dialog :deep(.el-dialog__footer) {
  padding: var(--spacing-md) var(--spacing-lg) var(--spacing-lg);
  border-top: 1px solid var(--color-border-light);
}

/* 任务详情 */
.task-detail {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.detail-section--desc {
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--color-border-light);
}

.detail-section-title {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-xs);
}

.detail-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.detail-label {
  width: 80px;
  flex-shrink: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.detail-value {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
}

.detail-value--primary {
  font-weight: 600;
  color: var(--color-primary);
  font-size: var(--font-size-lg);
}

.detail-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-regular);
  line-height: 1.6;
  padding: var(--spacing-md);
  background-color: var(--color-bg-page);
  border-radius: var(--radius-sm);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
}
</style>
