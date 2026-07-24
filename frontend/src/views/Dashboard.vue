<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { BellFilled, DataAnalysis, OfficeBuilding, RefreshRight } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface WeatherRecord {
  temperature?: number
  recordedAt: string
}

interface PageResult<T> {
  list: T[]
  total: number
}

interface Overview {
  totalFarms: number
  totalUsers: number
  totalDiagnoses: number
  modelStatus: string
}

interface DiagnosisStats {
  total: number
  pending: number
  approved: number
  rejected: number
}

const weatherChartRef = ref<HTMLElement | null>(null)
const diagnosisChartRef = ref<HTMLElement | null>(null)
const lastUpdateTime = ref('')
const overview = ref<Overview>({ totalFarms: 0, totalUsers: 0, totalDiagnoses: 0, modelStatus: 'unknown' })
const diagnosisStats = ref<DiagnosisStats>({ total: 0, pending: 0, approved: 0, rejected: 0 })
const weatherRecords = ref<WeatherRecord[]>([])

let weatherChart: echarts.ECharts | null = null
let diagnosisChart: echarts.ECharts | null = null

const emptyGraphic = (text: string): echarts.EChartsOption['graphic'] => ({
  type: 'text',
  left: 'center',
  top: 'middle',
  style: { text, fill: '#9CA3AF', fontSize: 14 }
})

const initWeatherChart = () => {
  if (!weatherChartRef.value) return
  weatherChart = echarts.init(weatherChartRef.value)
  const records = [...weatherRecords.value]
    .sort((a, b) => a.recordedAt.localeCompare(b.recordedAt))
    .slice(-7)

  weatherChart.setOption({
    title: { text: '最近天气记录', left: 'center', top: 16, textStyle: { fontSize: 16 } },
    tooltip: { trigger: 'axis' },
    grid: { left: '8%', right: '5%', bottom: '10%', top: 78, containLabel: true },
    xAxis: {
      type: 'category',
      data: records.map(item => new Date(item.recordedAt).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })),
      axisTick: { show: false }
    },
    yAxis: { type: 'value', name: '温度 (°C)' },
    series: [{
      name: '温度',
      type: 'line',
      smooth: true,
      data: records.map(item => Number(item.temperature) || 0),
      lineStyle: { color: '#2D7D46', width: 3 },
      itemStyle: { color: '#2D7D46' },
      areaStyle: { color: 'rgba(45, 125, 70, 0.16)' }
    }],
    graphic: records.length ? undefined : emptyGraphic('暂无天气采集数据')
  })
}

const initDiagnosisChart = () => {
  if (!diagnosisChartRef.value) return
  diagnosisChart = echarts.init(diagnosisChartRef.value)
  const values = [diagnosisStats.value.pending, diagnosisStats.value.approved, diagnosisStats.value.rejected]

  diagnosisChart.setOption({
    title: { text: '诊断审核状态', left: 'center', top: 16, textStyle: { fontSize: 16 } },
    tooltip: { trigger: 'axis' },
    grid: { left: '8%', right: '5%', bottom: '10%', top: 78, containLabel: true },
    xAxis: { type: 'category', data: ['待审核', '已通过', '已驳回'], axisTick: { show: false } },
    yAxis: { type: 'value', minInterval: 1, name: '记录数' },
    series: [{
      type: 'bar',
      data: values,
      barWidth: '42%',
      label: { show: true, position: 'top' },
      itemStyle: { color: '#2D7D46', borderRadius: [6, 6, 0, 0] }
    }],
    graphic: diagnosisStats.value.total ? undefined : emptyGraphic('暂无诊断数据')
  })
}

const fetchDashboard = async () => {
  const [overviewData, statsData, weatherData] = await Promise.all([
    request.get<Overview>('/monitor/overview'),
    request.get<DiagnosisStats>('/diagnosis/stats'),
    request.get<PageResult<WeatherRecord>>('/weather?size=100')
  ])
  overview.value = overviewData
  diagnosisStats.value = statsData
  weatherRecords.value = weatherData.list
  lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  await nextTick()
  initWeatherChart()
  initDiagnosisChart()
}

const handleResize = () => {
  weatherChart?.resize()
  diagnosisChart?.resize()
}

onMounted(async () => {
  await fetchDashboard()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  weatherChart?.dispose()
  diagnosisChart?.dispose()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">农业趋势看板</h1>
        <p class="page-subtitle">基于平台采集记录与诊断数据的实时汇总</p>
      </div>
      <span class="update-time"><el-icon><RefreshRight /></el-icon>最后更新：{{ lastUpdateTime }}</span>
    </div>

    <div class="stat-cards">
      <div class="stat-card">
        <el-icon class="stat-icon"><OfficeBuilding /></el-icon>
        <div><strong>{{ overview.totalFarms }}</strong><span>农场数</span></div>
      </div>
      <div class="stat-card">
        <el-icon class="stat-icon"><DataAnalysis /></el-icon>
        <div><strong>{{ overview.totalDiagnoses }}</strong><span>诊断总数</span></div>
      </div>
      <div class="stat-card">
        <el-icon class="stat-icon"><BellFilled /></el-icon>
        <div><strong>{{ diagnosisStats.pending }}</strong><span>待审核数</span></div>
      </div>
      <div class="stat-card">
        <el-icon class="stat-icon"><RefreshRight /></el-icon>
        <div><strong>{{ overview.modelStatus === 'healthy' ? '正常' : '异常' }}</strong><span>模型状态</span></div>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-card"><div ref="weatherChartRef" class="chart-container"></div></div>
      <div class="chart-card"><div ref="diagnosisChartRef" class="chart-container"></div></div>
    </div>
  </div>
</template>

<style scoped>
.update-time { display: flex; align-items: center; gap: 6px; color: var(--color-text-secondary); background: #fff; padding: 10px 14px; border-radius: 8px; box-shadow: var(--shadow-sm); }
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { display: flex; align-items: center; gap: 16px; padding: 22px; background: #fff; border-radius: 12px; box-shadow: var(--shadow-sm); border-left: 4px solid var(--color-primary); }
.stat-icon { width: 46px; height: 46px; padding: 11px; border-radius: 12px; color: var(--color-primary); background: rgba(45, 125, 70, 0.1); }
.stat-card div { display: flex; flex-direction: column; gap: 5px; }
.stat-card strong { font-size: 26px; line-height: 1; }
.stat-card span { color: var(--color-text-secondary); font-size: 14px; }
.chart-row { display: grid; grid-template-columns: repeat(2, 1fr); gap: 24px; }
.chart-card { background: #fff; border-radius: 16px; box-shadow: var(--shadow-sm); overflow: hidden; }
.chart-container { width: 100%; min-height: 420px; }
@media (max-width: 1024px) { .stat-cards { grid-template-columns: repeat(2, 1fr); } .chart-row { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .stat-cards { grid-template-columns: 1fr; } }
</style>
