<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElCard, ElTable, ElTableColumn, ElTag, ElButton } from 'element-plus'
import * as echarts from 'echarts'

interface PriceItem {
  id: string
  cropName: string
  unit: string
  currentPrice: number
  prevPrice: number
  change: number
  market: string
  date: string
  trend: 'up' | 'down' | 'stable'
}

const mockPrices: PriceItem[] = [
  { id: '1', cropName: '水稻', unit: '元/公斤', currentPrice: 2.8, prevPrice: 2.6, change: 7.7, market: '昆明农产品市场', date: '2026-07-22', trend: 'up' },
  { id: '2', cropName: '玉米', unit: '元/公斤', currentPrice: 1.9, prevPrice: 2.1, change: -9.5, market: '曲靖农贸市场', date: '2026-07-22', trend: 'down' },
  { id: '3', cropName: '番茄', unit: '元/公斤', currentPrice: 5.2, prevPrice: 5.0, change: 4.0, market: '大理蔬菜批发市场', date: '2026-07-22', trend: 'up' },
  { id: '4', cropName: '草莓', unit: '元/公斤', currentPrice: 35.0, prevPrice: 38.0, change: -7.9, market: '玉溪水果市场', date: '2026-07-21', trend: 'down' },
  { id: '5', cropName: '黄瓜', unit: '元/公斤', currentPrice: 3.5, prevPrice: 3.5, change: 0, market: '楚雄农产品市场', date: '2026-07-22', trend: 'stable' },
  { id: '6', cropName: '土豆', unit: '元/公斤', currentPrice: 2.2, prevPrice: 2.0, change: 10.0, market: '丽江农贸市场', date: '2026-07-22', trend: 'up' },
  { id: '7', cropName: '白菜', unit: '元/公斤', currentPrice: 1.8, prevPrice: 2.0, change: -10.0, market: '普洱蔬菜市场', date: '2026-07-21', trend: 'down' },
  { id: '8', cropName: '苹果', unit: '元/公斤', currentPrice: 8.5, prevPrice: 8.2, change: 3.7, market: '昭通水果市场', date: '2026-07-22', trend: 'up' }
]

const prices = ref<PriceItem[]>([])

const chartRef1 = ref<HTMLDivElement | null>(null)
const chartRef2 = ref<HTMLDivElement | null>(null)
let chartInstance1: echarts.ECharts | null = null
let chartInstance2: echarts.ECharts | null = null

const loadPrices = () => {
  prices.value = [...mockPrices]
}

const initCharts = () => {
  if (chartRef1.value) {
    chartInstance1 = echarts.init(chartRef1.value)
    initPriceTrendChart()
  }
  if (chartRef2.value) {
    chartInstance2 = echarts.init(chartRef2.value)
    initMarketShareChart()
  }
}

const initPriceTrendChart = () => {
  if (!chartInstance1) return
  const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
  const ricePrices = [2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, null, null, null, null, null]
  const cornPrices = [1.8, 1.9, 2.0, 2.1, 2.2, 2.1, 1.9, null, null, null, null, null]
  
  chartInstance1.setOption({
    title: { text: '主要农产品价格趋势', left: 'center', textStyle: { fontSize: 16 } },
    tooltip: { trigger: 'axis' },
    legend: { data: ['水稻', '玉米'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: months },
    yAxis: { type: 'value', name: '价格(元/公斤)' },
    series: [
      { name: '水稻', type: 'line', smooth: true, data: ricePrices, itemStyle: { color: '#409eff' } },
      { name: '玉米', type: 'line', smooth: true, data: cornPrices, itemStyle: { color: '#67c23a' } }
    ]
  })
}

const initMarketShareChart = () => {
  if (!chartInstance2) return
  chartInstance2.setOption({
    title: { text: '市场份额分布', left: 'center', textStyle: { fontSize: 16 } },
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: '65%',
      center: ['50%', '50%'],
      label: { show: true, fontSize: 12 },
      data: [
        { value: 35, name: '昆明农产品市场', itemStyle: { color: '#409eff' } },
        { value: 20, name: '曲靖农贸市场', itemStyle: { color: '#67c23a' } },
        { value: 15, name: '大理蔬菜批发市场', itemStyle: { color: '#e6a23c' } },
        { value: 15, name: '玉溪水果市场', itemStyle: { color: '#f56c6c' } },
        { value: 15, name: '其他市场', itemStyle: { color: '#909399' } }
      ]
    }]
  })
}

const handleRefresh = () => {
  loadPrices()
  initCharts()
}

const handleResize = () => {
  chartInstance1?.resize()
  chartInstance2?.resize()
}

onMounted(() => {
  loadPrices()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance1?.dispose()
  chartInstance2?.dispose()
})
</script>

<template>
  <div class="coop-market-container">
    <div class="chart-row">
      <ElCard class="chart-card">
        <div ref="chartRef1" class="chart-container"></div>
      </ElCard>
      <ElCard class="chart-card">
        <div ref="chartRef2" class="chart-container"></div>
      </ElCard>
    </div>

    <ElCard class="table-card" header="今日市场价格">
      <div class="card-header">
        <span class="update-time">最后更新：2026-07-22 14:30</span>
        <ElButton type="primary" @click="handleRefresh">刷新数据</ElButton>
      </div>
      <ElTable :data="prices" border style="width: 100%">
        <ElTableColumn prop="cropName" label="农产品" min-width="100" />
        <ElTableColumn prop="market" label="市场" min-width="150" />
        <ElTableColumn prop="currentPrice" label="当前价格" min-width="120">
          <template #default="{ row }">
            <span class="price-value">{{ row.currentPrice }}</span>
            <span class="price-unit">{{ row.unit }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="prevPrice" label="昨日价格" min-width="120">
          <template #default="{ row }">
            <span>{{ row.prevPrice }}{{ row.unit }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="change" label="涨跌幅" min-width="100">
          <template #default="{ row }">
            <ElTag :type="row.trend === 'up' ? 'success' : row.trend === 'down' ? 'danger' : 'info'">
              {{ row.trend === 'up' ? '+' : '' }}{{ row.change.toFixed(1) }}%
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="date" label="更新日期" min-width="120" />
      </ElTable>
    </ElCard>
  </div>
</template>

<style scoped>
.coop-market-container {
  padding: 20px;
  background: #fafafa;
  min-height: calc(100vh - 60px);
}

.chart-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  flex: 1;
}

.chart-container {
  width: 100%;
  min-height: 350px;
}

.table-card {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.update-time {
  font-size: 14px;
  color: #909399;
}

.price-value {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.price-unit {
  font-size: 14px;
  color: #909399;
  margin-left: 4px;
}
</style>