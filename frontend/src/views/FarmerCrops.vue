<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElCard, ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElDatePicker, ElSelect, ElOption, ElMessage, ElTag } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

interface Crop {
  id: string
  name: string
  fieldName: string
  plantedDate: string
  expectedHarvestDate: string
  status: '生长中' | '待收获' | '已收获'
  area: number
  variety: string
  notes?: string
}

interface PlantingCycle {
  id: string
  cropName: string
  fieldName: string
  plantedDate: string
  expectedHarvestDate: string
  status: string
  area: number
  variety: string
  notes?: string
}

interface PageResult<T> {
  list: T[]
  total: number
}

interface Response<T> {
  code: number
  data: T
}

const crops = ref<Crop[]>([])
const dialogVisible = ref(false)
const editMode = ref(false)
const formData = ref<Crop>({
  id: '',
  name: '',
  fieldName: '',
  plantedDate: '',
  expectedHarvestDate: '',
  status: '生长中',
  area: 0,
  variety: ''
})

const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const statusColors: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
  '生长中': 'success',
  '待收获': 'warning',
  '已收获': 'info'
}

const growingCount = computed(() => crops.value.filter(c => c.status === '生长中').length)
const pendingCount = computed(() => crops.value.filter(c => c.status === '待收获').length)
const harvestedCount = computed(() => crops.value.filter(c => c.status === '已收获').length)
const totalArea = computed(() => crops.value.reduce((sum, c) => sum + c.area, 0))

const loadCrops = async () => {
  try {
    const response = await request.get<Response<PageResult<PlantingCycle>>>('/planting-cycles')
    if (response.code === 200) {
      crops.value = response.data.list.map(cycle => ({
        id: cycle.id,
        name: cycle.cropName,
        fieldName: cycle.fieldName,
        plantedDate: cycle.plantedDate,
        expectedHarvestDate: cycle.expectedHarvestDate,
        status: cycle.status === 'ACTIVE' ? '生长中' : cycle.status === 'PENDING_HARVEST' ? '待收获' : '已收获',
        area: cycle.area || 0,
        variety: cycle.variety,
        notes: cycle.notes
      }))
      updateChart()
    }
  } catch (error) {
    ElMessage.error('获取种植档案失败')
  }
}

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return
  const greenPalette = [
    '#2D7D46',
    '#3A9D5C',
    '#52C41A',
    '#73D13D',
    '#95DE64',
    '#B7EB8F',
    '#D9F7BE',
    '#1B5E32'
  ]
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: 'var(--color-border)',
      borderWidth: 1,
      textStyle: {
        color: '#1F2937',
        fontSize: 13
      },
      padding: [12, 16],
      extraCssText: 'border-radius: 8px; box-shadow: 0 4px 12px rgba(45, 125, 70, 0.1);'
    },
    legend: {
      bottom: 8,
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 16,
      textStyle: {
        fontSize: 12,
        color: '#6B7280'
      }
    },
    series: [{
      type: 'pie',
      radius: ['45%', '72%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 3
      },
      label: {
        show: true,
        fontSize: 13,
        color: '#374151',
        formatter: '{b}\n{d}%'
      },
      labelLine: {
        length: 16,
        length2: 12,
        smooth: true
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 15,
          fontWeight: 'bold'
        },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(45, 125, 70, 0.2)'
        }
      },
      data: crops.value.map((c, index) => ({
        value: c.area,
        name: c.name,
        itemStyle: { color: greenPalette[index % greenPalette.length] }
      }))
    }]
  }
  chartInstance.setOption(option)
}

const handleAdd = () => {
  editMode.value = false
  formData.value = {
    id: '',
    name: '',
    fieldName: '',
    plantedDate: '',
    expectedHarvestDate: '',
    status: '生长中',
    area: 0,
    variety: ''
  }
  dialogVisible.value = true
}

const handleEdit = (row: Crop) => {
  editMode.value = true
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (id: string) => {
  try {
    await request.delete(`/planting-cycles/${id}`)
    crops.value = crops.value.filter(c => c.id !== id)
    ElMessage.success('删除成功')
    updateChart()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleSubmit = async () => {
  try {
    if (editMode.value) {
      await request.put(`/planting-cycles/${formData.value.id}`, {
        cropName: formData.value.name,
        fieldName: formData.value.fieldName,
        plantedDate: formData.value.plantedDate,
        expectedHarvestDate: formData.value.expectedHarvestDate,
        status: formData.value.status === '生长中' ? 'ACTIVE' : formData.value.status === '待收获' ? 'PENDING_HARVEST' : 'HARVESTED',
        area: formData.value.area,
        variety: formData.value.variety,
        notes: formData.value.notes
      })
      ElMessage.success('修改成功')
    } else {
      await request.post('/planting-cycles', {
        cropName: formData.value.name,
        fieldName: formData.value.fieldName,
        plantedDate: formData.value.plantedDate,
        expectedHarvestDate: formData.value.expectedHarvestDate,
        status: 'ACTIVE',
        area: formData.value.area,
        variety: formData.value.variety,
        notes: formData.value.notes
      })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadCrops()
  } catch (error) {
    ElMessage.error(editMode.value ? '修改失败' : '添加失败')
  }
}

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  loadCrops()
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
        <h1 class="page-title">种植档案</h1>
        <p class="page-subtitle">跟踪和管理您的作物种植信息</p>
      </div>
    </div>

    <!-- 统计卡片行 -->
    <div class="stats-row">
      <ElCard class="stat-card" shadow="never">
        <div class="stat-card-inner">
          <div class="stat-bar stat-bar--green"></div>
          <div class="stat-content">
            <div class="stat-value">{{ growingCount }}</div>
            <div class="stat-label">生长中</div>
          </div>
          <div class="stat-icon-wrap stat-icon-wrap--green">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22c4-4 8-7.5 8-12a8 8 0 1 0-16 0c0 4.5 4 8 8 12z"/><circle cx="12" cy="10" r="3"/></svg>
          </div>
        </div>
      </ElCard>

      <ElCard class="stat-card" shadow="never">
        <div class="stat-card-inner">
          <div class="stat-bar stat-bar--gold"></div>
          <div class="stat-content">
            <div class="stat-value">{{ pendingCount }}</div>
            <div class="stat-label">待收获</div>
          </div>
          <div class="stat-icon-wrap stat-icon-wrap--gold">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>
          </div>
        </div>
      </ElCard>

      <ElCard class="stat-card" shadow="never">
        <div class="stat-card-inner">
          <div class="stat-bar stat-bar--blue"></div>
          <div class="stat-content">
            <div class="stat-value">{{ harvestedCount }}</div>
            <div class="stat-label">已收获</div>
          </div>
          <div class="stat-icon-wrap stat-icon-wrap--blue">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          </div>
        </div>
      </ElCard>

      <ElCard class="stat-card" shadow="never">
        <div class="stat-card-inner">
          <div class="stat-bar stat-bar--dark"></div>
          <div class="stat-content">
            <div class="stat-value">{{ totalArea.toFixed(1) }}<span class="stat-unit">亩</span></div>
            <div class="stat-label">总面积</div>
          </div>
          <div class="stat-icon-wrap stat-icon-wrap--dark">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>
          </div>
        </div>
      </ElCard>
    </div>

    <!-- 内容区：左侧饼图 + 右侧表格 -->
    <div class="content-row">
      <ElCard class="chart-card" shadow="never">
        <template #header>
          <div class="card-header-title">种植面积分布</div>
        </template>
        <div ref="chartRef" class="chart-container"></div>
      </ElCard>

      <ElCard class="table-card" shadow="never">
        <template #header>
          <div class="card-header-row">
            <span class="card-header-title">种植档案列表</span>
            <ElButton type="primary" :icon="Plus" size="small" @click="handleAdd">添加记录</ElButton>
          </div>
        </template>
        <el-table :data="crops" class="custom-table">
          <el-table-column prop="name" label="作物名称" min-width="90">
            <template #default="{ row }">
              <span class="cell-primary">{{ (row as Crop).name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="fieldName" label="所属地块" min-width="110" />
          <el-table-column prop="variety" label="品种" min-width="90" />
          <el-table-column prop="plantedDate" label="种植日期" min-width="110" />
          <el-table-column prop="expectedHarvestDate" label="预计收获" min-width="110" />
          <el-table-column prop="area" label="面积(亩)" min-width="85" align="right">
            <template #default="{ row }">
              <span class="cell-number">{{ (row as Crop).area.toFixed(1) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" min-width="90" align="center">
            <template #default="{ row }">
              <ElTag :type="statusColors[row.status]" size="small" effect="light">{{ row.status }}</ElTag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right" align="center">
            <template #default="{ row }">
              <div class="action-btns">
                <ElButton type="primary" link :icon="Edit" @click="handleEdit(row as Crop)" />
                <ElButton type="danger" link :icon="Delete" @click="handleDelete((row as Crop).id)" />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </ElCard>
    </div>

    <!-- 新增/编辑对话框 -->
    <ElDialog
      :title="editMode ? '编辑种植记录' : '添加种植记录'"
      v-model="dialogVisible"
      width="560px"
      :close-on-click-modal="false"
      class="custom-dialog"
    >
      <ElForm :model="formData" label-width="110px" label-position="left" class="custom-form">
        <ElFormItem label="作物名称" required>
          <ElInput v-model="formData.name" placeholder="请输入作物名称" />
        </ElFormItem>
        <ElFormItem label="所属地块" required>
          <ElSelect v-model="formData.fieldName" placeholder="请选择地块" style="width: 100%">
            <ElOption label="水稻田A" value="水稻田A" />
            <ElOption label="玉米地B" value="玉米地B" />
            <ElOption label="蔬菜基地C" value="蔬菜基地C" />
            <ElOption label="水果园D" value="水果园D" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="品种" required>
          <ElInput v-model="formData.variety" placeholder="请输入品种" />
        </ElFormItem>
        <ElFormItem label="种植日期" required>
          <ElDatePicker v-model="formData.plantedDate" type="date" placeholder="选择种植日期" style="width: 100%" />
        </ElFormItem>
        <ElFormItem label="预计收获日期" required>
          <ElDatePicker v-model="formData.expectedHarvestDate" type="date" placeholder="选择预计收获日期" style="width: 100%" />
        </ElFormItem>
        <ElFormItem label="面积(亩)" required>
          <ElInput v-model.number="formData.area" placeholder="请输入面积" />
        </ElFormItem>
        <ElFormItem label="备注">
          <ElInput v-model="formData.notes" type="textarea" :rows="3" placeholder="请输入备注信息" />
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
/* 统计卡片行 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
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

.stat-bar--green {
  background-color: var(--color-success);
}

.stat-bar--gold {
  background-color: var(--color-warning);
}

.stat-bar--blue {
  background-color: var(--color-info);
}

.stat-bar--dark {
  background-color: var(--color-primary);
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

.stat-unit {
  font-size: var(--font-size-sm);
  font-weight: 400;
  color: var(--color-text-secondary);
  margin-left: 2px;
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

.stat-icon-wrap--green {
  background-color: #E8F5EC;
  color: var(--color-success);
}

.stat-icon-wrap--gold {
  background-color: #FFF8E6;
  color: var(--color-warning);
}

.stat-icon-wrap--blue {
  background-color: #E6F4FF;
  color: var(--color-info);
}

.stat-icon-wrap--dark {
  background-color: var(--color-primary-lighter);
  color: var(--color-primary);
}

/* 内容区 */
.content-row {
  display: flex;
  gap: var(--spacing-md);
}

.chart-card {
  width: 380px;
  flex-shrink: 0;
}

.chart-card :deep(.el-card__header) {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-light);
}

.chart-container {
  width: 100%;
  min-height: 340px;
}

.table-card {
  flex: 1;
  min-width: 0;
}

.table-card :deep(.el-card__header) {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-light);
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header-title {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text-primary);
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

.cell-primary {
  font-weight: 600;
  color: var(--color-primary);
}

.cell-number {
  font-variant-numeric: tabular-nums;
  color: var(--color-text-regular);
}

/* 操作按钮 */
.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
}

.action-btns :deep(.el-button) {
  padding: 4px;
  font-size: 16px;
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

.custom-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.custom-form :deep(.el-select .el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
}

/* 响应式 */
@media (max-width: 1200px) {
  .content-row {
    flex-direction: column;
  }
  .chart-card {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
