<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElCard, ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElDatePicker, ElSelect, ElOption, ElMessage, ElMessageBox, ElTag } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import * as echarts from '@/utils/echarts'
import request from '@/utils/request'
import { cycleStatusLabel } from '@/utils/domainMappers'

interface Crop {
  id: string
  cropId: string
  fieldId: string
  name: string
  fieldName: string
  plantedDate: string
  expectedHarvestDate: string
  growthStage: string
  status: '生长中' | '待收获' | '已收获'
  area: number
  variety: string
  notes?: string
}

interface PlantingCycle {
  id: string
  cropId: string
  fieldId: string
  plantingDate?: string
  expectedHarvestDate?: string
  growthStage?: string
  status: string
  areaMu?: number
  remark?: string
}

interface PageResult<T> {
  list: T[]
  total: number
}

interface CropOption {
  id: string
  name: string
  category?: string
  variety?: string
  growthDays?: number
  status?: 'active' | 'inactive'
}

interface NewCropForm {
  name: string
  category: string
  variety: string
  growthDays?: number
}

interface FarmOption {
  id: string
  name: string
}

interface FieldOption {
  id: string
  name: string
  farmName: string
}

const crops = ref<Crop[]>([])
const cropOptions = ref<CropOption[]>([])
const fieldOptions = ref<FieldOption[]>([])
const growthStageOptions = [
  { label: '播种期', value: 'sowing' },
  { label: '苗期', value: 'seedling' },
  { label: '分蘖期', value: 'tillering' },
  { label: '开花期', value: 'flowering' },
  { label: '结果期', value: 'fruiting' },
  { label: '成熟期', value: 'maturity' }
]
const dialogVisible = ref(false)
const cropDialogVisible = ref(false)
const cropSubmitting = ref(false)
const editMode = ref(false)
const newCropForm = ref<NewCropForm>({
  name: '',
  category: '',
  variety: '',
  growthDays: undefined
})
const formData = ref<Crop>({
  id: '',
  cropId: '',
  fieldId: '',
  name: '',
  fieldName: '',
  plantedDate: '',
  expectedHarvestDate: '',
  growthStage: 'seedling',
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
const activeCropOptions = computed(() =>
  cropOptions.value.filter(crop => crop.status !== 'inactive')
)
const selectableCropOptions = computed(() => {
  if (!editMode.value) return activeCropOptions.value
  const selected = cropOptions.value.find(crop => crop.id === formData.value.cropId)
  if (!selected || selected.status !== 'inactive') return activeCropOptions.value
  return [selected, ...activeCropOptions.value]
})

const loadCrops = async () => {
  try {
    const [cycles, availableCrops, farmPage] = await Promise.all([
      request.get<PageResult<PlantingCycle>>('/planting-cycles?size=100'),
      request.get<PageResult<CropOption>>('/crops', { params: { size: 100, includeInactive: true } }),
      request.get<PageResult<FarmOption>>('/farms?size=100')
    ])
    cropOptions.value = availableCrops.list
    const fieldPages = await Promise.all(farmPage.list.map(async farm => ({
      farm,
      page: await request.get<PageResult<{ id: string; name: string }>>(`/farms/${farm.id}/fields`)
    })))
    fieldOptions.value = fieldPages.flatMap(({ farm, page }) => page.list.map(field => ({
      id: field.id,
      name: field.name,
      farmName: farm.name
    })))
    const cropMap = new Map(cropOptions.value.map(crop => [crop.id, crop]))
    const fieldMap = new Map(fieldOptions.value.map(field => [field.id, field]))
    crops.value = cycles.list.map(cycle => {
      const crop = cropMap.get(cycle.cropId)
      const field = fieldMap.get(cycle.fieldId)
      return {
        id: cycle.id,
        cropId: cycle.cropId,
        fieldId: cycle.fieldId,
        name: crop?.name || '未知作物',
        fieldName: field ? `${field.farmName} / ${field.name}` : '未知地块',
        plantedDate: cycle.plantingDate || '-',
        expectedHarvestDate: cycle.expectedHarvestDate || '-',
        growthStage: cycle.growthStage || '',
        status: cycleStatusLabel(cycle.status),
        area: Number(cycle.areaMu) || 0,
        variety: crop?.variety || '-',
        notes: cycle.remark
      }
    })
    updateChart()
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

const openCropDialog = () => {
  newCropForm.value = {
    name: '',
    category: '',
    variety: '',
    growthDays: undefined
  }
  cropDialogVisible.value = true
}

const handleCreateCrop = async () => {
  const name = newCropForm.value.name.trim()
  if (!name) {
    ElMessage.warning('请输入作物名称')
    return
  }

  cropSubmitting.value = true
  try {
    const createdCrop = await request.post<CropOption>('/crops', {
      name,
      category: newCropForm.value.category.trim() || undefined,
      variety: newCropForm.value.variety.trim() || undefined,
      growthDays: newCropForm.value.growthDays || undefined
    })
    cropOptions.value.unshift({ ...createdCrop, status: createdCrop.status || 'active' })
    formData.value.cropId = createdCrop.id
    cropDialogVisible.value = false
    ElMessage.success('作物品种创建成功，已自动选中')
  } catch {
    ElMessage.error('创建作物品种失败')
  } finally {
    cropSubmitting.value = false
  }
}

const handleAdd = () => {
  editMode.value = false
  formData.value = {
    id: '',
    cropId: activeCropOptions.value[0]?.id || '',
    fieldId: fieldOptions.value[0]?.id || '',
    name: '',
    fieldName: '',
    plantedDate: '',
    expectedHarvestDate: '',
    growthStage: 'seedling',
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
  const cycle = crops.value.find(item => item.id === id)
  try {
    await ElMessageBox.confirm(
      `确定删除种植记录「${cycle?.name || ''}」吗？如已产生观测或农事任务，系统将阻止删除。`,
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await request.delete(`/planting-cycles/${id}`, { silent: true })
    crops.value = crops.value.filter(c => c.id !== id)
    ElMessage.success('删除成功')
    updateChart()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error instanceof Error ? error.message : '删除种植记录失败')
  }
}

const completeCycle = async (row: Crop) => {
  try {
    await ElMessageBox.confirm(
      `确定将「${row.name}」的种植周期标记为已收获吗？`,
      '结束种植周期',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const actualHarvestDate = new Date().toISOString().slice(0, 10)
    await request.put(`/planting-cycles/${row.id}`, {
      status: 'completed',
      actualHarvestDate
    }, { silent: true })
    ElMessage.success('种植周期已结束')
    await loadCrops()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error instanceof Error ? error.message : '结束种植周期失败')
  }
}

const handleSubmit = async () => {
  try {
    if (editMode.value) {
      await request.put(`/planting-cycles/${formData.value.id}`, {
        cropId: formData.value.cropId,
        plantingDate: formData.value.plantedDate,
        expectedHarvestDate: formData.value.expectedHarvestDate,
        growthStage: formData.value.growthStage,
        status: formData.value.status === '生长中' ? 'active' : formData.value.status === '待收获' ? 'pending_harvest' : 'completed',
        areaMu: formData.value.area,
        remark: formData.value.notes
      })
      ElMessage.success('修改成功')
    } else {
      await request.post('/planting-cycles', {
        cropId: formData.value.cropId,
        fieldId: formData.value.fieldId,
        plantingDate: formData.value.plantedDate,
        expectedHarvestDate: formData.value.expectedHarvestDate,
        growthStage: formData.value.growthStage,
        areaMu: formData.value.area,
        remark: formData.value.notes
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
          <el-table-column label="操作" width="190" fixed="right" align="center">
            <template #default="{ row }">
              <div class="action-btns">
                <ElButton type="primary" link :icon="Edit" @click="handleEdit(row as Crop)" />
                <ElButton
                  v-if="(row as Crop).status !== '已收获'"
                  type="success"
                  link
                  @click="completeCycle(row as Crop)"
                >结束周期</ElButton>
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
          <div class="crop-select-row">
            <ElSelect v-model="formData.cropId" placeholder="请选择作物" style="width: 100%">
              <ElOption v-for="crop in selectableCropOptions" :key="crop.id" :label="crop.variety ? `${crop.name} / ${crop.variety}` : crop.name" :value="crop.id" :disabled="crop.status === 'inactive' && crop.id !== formData.cropId" />
            </ElSelect>
            <ElButton type="primary" plain :icon="Plus" @click="openCropDialog">新建作物品种</ElButton>
          </div>
        </ElFormItem>
        <ElFormItem label="所属地块" required>
          <ElSelect v-model="formData.fieldId" placeholder="请选择地块" style="width: 100%" :disabled="editMode">
            <ElOption v-for="field in fieldOptions" :key="field.id" :label="`${field.farmName} / ${field.name}`" :value="field.id" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="生育期" required>
          <ElSelect v-model="formData.growthStage" placeholder="请选择生育期" style="width: 100%">
            <ElOption
              v-for="stage in growthStageOptions"
              :key="stage.value"
              :label="stage.label"
              :value="stage.value"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="种植日期" required>
          <ElDatePicker v-model="formData.plantedDate" type="date" value-format="YYYY-MM-DD" placeholder="选择种植日期" style="width: 100%" />
        </ElFormItem>
        <ElFormItem label="预计收获日期" required>
          <ElDatePicker v-model="formData.expectedHarvestDate" type="date" value-format="YYYY-MM-DD" placeholder="选择预计收获日期" style="width: 100%" />
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

    <ElDialog
      v-model="cropDialogVisible"
      title="新建作物品种"
      width="480px"
      append-to-body
      :close-on-click-modal="false"
      class="custom-dialog"
    >
      <ElForm :model="newCropForm" label-width="90px" label-position="left" class="custom-form">
        <ElFormItem label="作物名称" required>
          <ElInput v-model="newCropForm.name" maxlength="100" placeholder="例如：水稻、玉米、番茄" />
        </ElFormItem>
        <ElFormItem label="作物类别">
          <ElSelect v-model="newCropForm.category" clearable placeholder="请选择类别" style="width: 100%">
            <ElOption label="粮食作物" value="粮食作物" />
            <ElOption label="经济作物" value="经济作物" />
            <ElOption label="蔬菜" value="蔬菜" />
            <ElOption label="水果" value="水果" />
            <ElOption label="其他" value="其他" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="品种名称">
          <ElInput v-model="newCropForm.variety" maxlength="100" placeholder="例如：滇粳优8号" />
        </ElFormItem>
        <ElFormItem label="生长周期">
          <ElInput v-model.number="newCropForm.growthDays" type="number" min="1" max="1000" placeholder="天数，例如：120" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="cropDialogVisible = false">取消</ElButton>
          <ElButton type="primary" :loading="cropSubmitting" @click="handleCreateCrop">创建并选中</ElButton>
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

.crop-select-row {
  display: flex;
  width: 100%;
  gap: var(--spacing-sm);
}

.crop-select-row .el-select {
  flex: 1;
}

.crop-select-row .el-button {
  flex-shrink: 0;
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
