<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElTable, ElTableColumn, ElTag, ElButton } from 'element-plus'
import * as echarts from 'echarts'
import { RefreshRight, Top, Bottom, Minus } from '@element-plus/icons-vue'

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

const upCount = computed(() => prices.value.filter(item => item.trend === 'up').length)
const downCount = computed(() => prices.value.filter(item => item.trend === 'down').length)
const stableCount = computed(() => prices.value.filter(item => item.trend === 'stable').length)

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
    title: {
      text: '主要农产品价格趋势',
      left: 'center',
      top: 16,
      textStyle: {
        color: 'var(--color-text-primary)',
        fontSize: 16,
        fontWeight: '600'
      }
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: 'var(--color-border)',
      borderWidth: 1,
      textStyle: {
        color: 'var(--color-text-primary)',
        fontSize: 13
      }
    },
    legend: {
      data: ['水稻', '玉米'],
      bottom: 12,
      textStyle: {
        color: 'var(--color-text-secondary)',
        fontSize: 13
      },
      itemWidth: 16,
      itemHeight: 8,
      itemGap: 24
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: 70,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: months,
      axisLabel: {
        color: 'var(--color-text-secondary)',
        fontSize: 12
      },
      axisLine: {
        lineStyle: {
          color: 'var(--color-border)'
        }
      },
      axisTick: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      name: '价格(元/公斤)',
      nameTextStyle: {
        color: 'var(--color-text-secondary)',
        fontSize: 12,
        padding: [0, 0, 0, 50]
      },
      axisLabel: {
        color: 'var(--color-text-secondary)',
        fontSize: 12
      },
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      splitLine: {
        lineStyle: {
          color: 'var(--color-border-light)',
          type: 'dashed'
        }
      }
    },
    series: [
      {
        name: '水稻',
        type: 'line',
        smooth: true,
        data: ricePrices,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          color: '#2D7D46',
          width: 3
        },
        itemStyle: {
          color: '#2D7D46',
          borderWidth: 2,
          borderColor: '#fff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(45, 125, 70, 0.2)' },
            { offset: 1, color: 'rgba(45, 125, 70, 0.02)' }
          ])
        }
      },
      {
        name: '玉米',
        type: 'line',
        smooth: true,
        data: cornPrices,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          color: '#3A9D5C',
          width: 3
        },
        itemStyle: {
          color: '#3A9D5C',
          borderWidth: 2,
          borderColor: '#fff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(58, 157, 92, 0.15)' },
            { offset: 1, color: 'rgba(58, 157, 92, 0.02)' }
          ])
        }
      }
    ]
  })
}

const initMarketShareChart = () => {
  if (!chartInstance2) return
  chartInstance2.setOption({
    title: {
      text: '市场份额分布',
      left: 'center',
      top: 16,
      textStyle: {
        color: 'var(--color-text-primary)',
        fontSize: 16,
        fontWeight: '600'
      }
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: 'var(--color-border)',
      borderWidth: 1,
      textStyle: {
        color: 'var(--color-text-primary)',
        fontSize: 13
      },
      formatter: (params: any) => {
        return `<div style="display:flex;align-items:center;gap:8px;margin-bottom:4px;">
          <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${params.color};"></span>
          <span style="font-weight:600;">${params.name}</span>
        </div>
        <div style="color:#6B7280;">占比: <span style="color:#2D7D46;font-weight:600;">${params.percent}%</span></div>`
      }
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '55%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        fontSize: 12,
        color: 'var(--color-text-secondary)'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
        },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        }
      },
      data: [
        { value: 35, name: '昆明农产品市场', itemStyle: { color: '#2D7D46' } },
        { value: 20, name: '曲靖农贸市场', itemStyle: { color: '#3A9D5C' } },
        { value: 15, name: '大理蔬菜批发市场', itemStyle: { color: '#52C41A' } },
        { value: 15, name: '玉溪水果市场', itemStyle: { color: '#73D13D' } },
        { value: 15, name: '其他市场', itemStyle: { color: '#95DE64' } }
      ]
    }]
  })
}

const handleRefresh = () => {
  loadPrices()
  if (chartInstance1) {
    chartInstance1.dispose()
    chartInstance1 = null
  }
  if (chartInstance2) {
    chartInstance2.dispose()
    chartInstance2 = null
  }
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
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">市场价格监控</h1>
        <p class="page-subtitle">实时追踪农产品价格变动与市场趋势</p>
      </div>
      <div class="header-actions">
        <ElButton type="primary" @click="handleRefresh">
          <el-icon><RefreshRight /></el-icon>
          刷新数据
        </ElButton>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-card__bar stat-card__bar--success"></div>
        <div class="stat-card__content">
          <div class="stat-card__icon stat-card__icon--success">
            <el-icon :size="22"><Top /></el-icon>
          </div>
          <div class="stat-card__info">
            <span class="stat-card__value">{{ upCount }}</span>
            <span class="stat-card__label">上涨品种</span>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__bar stat-card__bar--danger"></div>
        <div class="stat-card__content">
          <div class="stat-card__icon stat-card__icon--danger">
            <el-icon :size="22"><Bottom /></el-icon>
          </div>
          <div class="stat-card__info">
            <span class="stat-card__value">{{ downCount }}</span>
            <span class="stat-card__label">下跌品种</span>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__bar stat-card__bar--info"></div>
        <div class="stat-card__content">
          <div class="stat-card__icon stat-card__icon--info">
            <el-icon :size="22"><Minus /></el-icon>
          </div>
          <div class="stat-card__info">
            <span class="stat-card__value">{{ stableCount }}</span>
            <span class="stat-card__label">持平品种</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-row">
      <div class="chart-card">
        <div ref="chartRef1" class="chart-container"></div>
      </div>
      <div class="chart-card">
        <div ref="chartRef2" class="chart-container"></div>
      </div>
    </div>

    <!-- 价格表格 -->
    <div class="table-card">
      <div class="table-header">
        <div class="table-header__left">
          <h3 class="table-title">今日市场价格</h3>
          <span class="table-update">更新于 2026-07-22 14:30</span>
        </div>
      </div>
      <ElTable
        :data="prices"
        class="custom-table"
        :header-cell-style="{ background: 'var(--color-bg-page)', color: 'var(--color-text-secondary)', fontWeight: '600', borderBottom: 'none' }"
        :cell-style="{ borderBottom: '1px solid var(--color-border-light)' }"
      >
        <ElTableColumn prop="cropName" label="农产品" min-width="100">
          <template #default="{ row }">
            <span class="crop-name">{{ row.cropName }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="market" label="市场" min-width="160">
          <template #default="{ row }">
            <span class="market-text">{{ row.market }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="currentPrice" label="当前价格" min-width="130">
          <template #default="{ row }">
            <span class="price-current">{{ row.currentPrice.toFixed(1) }}</span>
            <span class="price-unit">{{ row.unit }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="prevPrice" label="昨日价格" min-width="120">
          <template #default="{ row }">
            <span class="price-prev">{{ row.prevPrice.toFixed(1) }} {{ row.unit }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="change" label="涨跌幅" min-width="110">
          <template #default="{ row }">
            <ElTag
              :type="row.trend === 'up' ? 'success' : row.trend === 'down' ? 'danger' : 'info'"
              size="small"
              effect="light"
              class="change-tag"
            >
              <span v-if="row.trend === 'up'">↑</span>
              <span v-else-if="row.trend === 'down'">↓</span>
              <span v-else>—</span>
              {{ row.trend === 'up' ? '+' : '' }}{{ row.change.toFixed(1) }}%
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="date" label="更新日期" min-width="120">
          <template #default="{ row }">
            <span class="date-text">{{ row.date }}</span>
          </template>
        </ElTableColumn>
      </ElTable>
    </div>
  </div>
</template>

<style scoped>
/* 页面头部 */
.header-actions {
  display: flex;
  align-items: center;
}

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

.stat-card__bar--success {
  background: var(--color-success);
}

.stat-card__bar--danger {
  background: var(--color-danger);
}

.stat-card__bar--info {
  background: var(--color-info);
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

.stat-card__icon--success {
  background: rgba(82, 196, 26, 0.1);
  color: var(--color-success);
}

.stat-card__icon--danger {
  background: rgba(245, 34, 45, 0.1);
  color: var(--color-danger);
}

.stat-card__icon--info {
  background: rgba(24, 144, 255, 0.1);
  color: var(--color-info);
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

/* 图表区域 */
.chart-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
}

.chart-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  transition: box-shadow var(--transition-normal);
}

.chart-card:hover {
  box-shadow: var(--shadow-md);
}

.chart-container {
  width: 100%;
  min-height: 380px;
  padding: var(--spacing-md);
}

/* 表格 */
.table-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  transition: box-shadow var(--transition-normal);
}

.table-card:hover {
  box-shadow: var(--shadow-md);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg) var(--spacing-lg) var(--spacing-md);
}

.table-header__left {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-md);
}

.table-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-text-primary);
}

.table-update {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
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

.crop-name {
  font-weight: 600;
  color: var(--color-primary);
}

.market-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.price-current {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--color-text-primary);
}

.price-unit {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-left: 4px;
}

.price-prev {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.change-tag {
  min-width: 72px;
  text-align: center;
  font-weight: 600;
}

.date-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 响应式 */
@media (max-width: 1024px) {
  .chart-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stat-cards {
    grid-template-columns: 1fr;
  }
}
</style>
