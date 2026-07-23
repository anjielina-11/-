<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElCard, ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElDatePicker, ElSelect, ElOption, ElMessage, ElTag } from 'element-plus'
import * as echarts from 'echarts'

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

const mockCrops: Crop[] = [
  { id: '1', name: '水稻', fieldName: '水稻田A', plantedDate: '2026-05-10', expectedHarvestDate: '2026-08-20', status: '生长中', area: 5.5, variety: '籼稻', notes: '采用节水灌溉技术' },
  { id: '2', name: '玉米', fieldName: '玉米地B', plantedDate: '2026-04-15', expectedHarvestDate: '2026-09-01', status: '生长中', area: 3.2, variety: '甜玉米' },
  { id: '3', name: '番茄', fieldName: '蔬菜基地C', plantedDate: '2026-06-01', expectedHarvestDate: '2026-08-15', status: '待收获', area: 1.8, variety: '大红番茄', notes: '有机种植' },
  { id: '4', name: '草莓', fieldName: '水果园D', plantedDate: '2025-10-01', expectedHarvestDate: '2026-05-30', status: '已收获', area: 2.0, variety: '红颜', notes: '冬季温室栽培' },
  { id: '5', name: '黄瓜', fieldName: '蔬菜基地C', plantedDate: '2026-03-20', expectedHarvestDate: '2026-06-20', status: '已收获', area: 1.5, variety: '密刺黄瓜' }
]

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

const loadCrops = () => {
  crops.value = [...mockCrops]
}

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return
  const option: echarts.EChartsOption = {
    title: { text: '种植面积分布', left: 'center', textStyle: { fontSize: 16 } },
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, fontSize: 14 },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      data: crops.value.map(c => ({
        value: c.area,
        name: c.name,
        itemStyle: { color: ['#67c23a', '#409eff', '#e6a23c', '#909399', '#f56c6c'][crops.value.indexOf(c) % 5] }
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

const handleDelete = (id: string) => {
  crops.value = crops.value.filter(c => c.id !== id)
  ElMessage.success('删除成功')
  updateChart()
}

const handleSubmit = () => {
  if (editMode.value) {
    const index = crops.value.findIndex(c => c.id === formData.value.id)
    if (index !== -1) {
      crops.value[index] = { ...formData.value }
      ElMessage.success('修改成功')
    }
  } else {
    formData.value.id = String(Date.now())
    crops.value.unshift({ ...formData.value })
    ElMessage.success('添加成功')
  }
  dialogVisible.value = false
  updateChart()
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
  <div class="farmer-crops-container">
    <div class="stats-row">
      <ElCard class="stat-card">
        <div class="stat-icon growing"></div>
        <div class="stat-info">
          <div class="stat-value">{{ growingCount }}</div>
          <div class="stat-label">生长中</div>
        </div>
      </ElCard>
      <ElCard class="stat-card">
        <div class="stat-icon pending"></div>
        <div class="stat-info">
          <div class="stat-value">{{ pendingCount }}</div>
          <div class="stat-label">待收获</div>
        </div>
      </ElCard>
      <ElCard class="stat-card">
        <div class="stat-icon harvested"></div>
        <div class="stat-info">
          <div class="stat-value">{{ harvestedCount }}</div>
          <div class="stat-label">已收获</div>
        </div>
      </ElCard>
      <ElCard class="stat-card">
        <div class="stat-icon total"></div>
        <div class="stat-info">
          <div class="stat-value">{{ totalArea.toFixed(1) }}亩</div>
          <div class="stat-label">总种植面积</div>
        </div>
      </ElCard>
    </div>

    <div class="content-row">
      <ElCard class="chart-card">
        <div ref="chartRef" class="chart-container"></div>
      </ElCard>

      <ElCard class="table-card" header="种植档案列表">
        <div class="card-header">
          <ElButton type="primary" @click="handleAdd">添加种植记录</ElButton>
        </div>
        <ElTable :data="crops" border style="width: 100%">
          <ElTableColumn prop="name" label="作物名称" min-width="100" />
          <ElTableColumn prop="fieldName" label="所属地块" min-width="120" />
          <ElTableColumn prop="variety" label="品种" min-width="100" />
          <ElTableColumn prop="plantedDate" label="种植日期" min-width="120" />
          <ElTableColumn prop="expectedHarvestDate" label="预计收获" min-width="120" />
          <ElTableColumn prop="area" label="面积(亩)" min-width="100" />
          <ElTableColumn prop="status" label="状态" min-width="100">
            <template #default="{ row }">
              <ElTag :type="statusColors[row.status]">{{ row.status }}</ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" min-width="150" fixed="right">
            <template #default="{ row }">
            <ElButton type="primary" link @click="handleEdit(row as Crop)">编辑</ElButton>
            <ElButton type="danger" link @click="handleDelete((row as Crop).id)">删除</ElButton>
          </template>
          </ElTableColumn>
        </ElTable>
      </ElCard>
    </div>

    <ElDialog :title="editMode ? '编辑种植记录' : '添加种植记录'" v-model="dialogVisible" width="600px">
      <ElForm :model="formData" label-width="120px">
        <ElFormItem label="作物名称" required>
          <ElInput v-model="formData.name" placeholder="请输入作物名称" />
        </ElFormItem>
        <ElFormItem label="所属地块" required>
          <ElSelect v-model="formData.fieldName" placeholder="请选择地块">
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
          <ElDatePicker v-model="formData.plantedDate" type="date" placeholder="选择种植日期" />
        </ElFormItem>
        <ElFormItem label="预计收获日期" required>
          <ElDatePicker v-model="formData.expectedHarvestDate" type="date" placeholder="选择预计收获日期" />
        </ElFormItem>
        <ElFormItem label="面积(亩)" required>
          <ElInput v-model.number="formData.area" placeholder="请输入面积" />
        </ElFormItem>
        <ElFormItem label="备注">
          <ElInput v-model="formData.notes" type="textarea" :rows="3" placeholder="请输入备注信息" />
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
.farmer-crops-container {
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
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon.growing { background: #e8f5e9; color: #67c23a; }
.stat-icon.pending { background: #fdf6ec; color: #e6a23c; }
.stat-icon.harvested { background: #ecf5ff; color: #409eff; }
.stat-icon.total { background: #f0f9eb; color: #52c41a; }

.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 24px; font-weight: bold; color: #303133; }
.stat-label { font-size: 12px; color: #909399; }

.content-row {
  display: flex;
  gap: 20px;
}

.chart-card {
  width: 400px;
  flex-shrink: 0;
}

.chart-container {
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