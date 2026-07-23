<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { Sunny, Warning, BellFilled, RefreshRight } from '@element-plus/icons-vue'

interface WeatherData {
  date: string
  highTemp: number
  lowTemp: number
}

interface PestData {
  month: string
  count: number
}

const weatherChartRef = ref<HTMLElement | null>(null)
const pestChartRef = ref<HTMLElement | null>(null)

let weatherChart: echarts.ECharts | null = null
let pestChart: echarts.ECharts | null = null

const weatherData: WeatherData[] = [
  { date: '7/16', highTemp: 28, lowTemp: 18 },
  { date: '7/17', highTemp: 30, lowTemp: 19 },
  { date: '7/18', highTemp: 32, lowTemp: 20 },
  { date: '7/19', highTemp: 29, lowTemp: 19 },
  { date: '7/20', highTemp: 27, lowTemp: 18 },
  { date: '7/21', highTemp: 26, lowTemp: 17 },
  { date: '7/22', highTemp: 28, lowTemp: 18 }
]

const pestData: PestData[] = [
  { month: '1月', count: 12 },
  { month: '2月', count: 8 },
  { month: '3月', count: 25 },
  { month: '4月', count: 45 },
  { month: '5月', count: 68 },
  { month: '6月', count: 85 },
  { month: '7月', count: 92 },
  { month: '8月', count: 78 },
  { month: '9月', count: 55 },
  { month: '10月', count: 35 },
  { month: '11月', count: 20 },
  { month: '12月', count: 15 }
]

const lastUpdateTime = ref('')

const todayTemp = ref('28°C')
const pestCount = ref('92')
const warningCount = ref('3')
const dataUpdate = ref('实时')

const getCurrentTime = () => {
  const now = new Date()
  return now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const initWeatherChart = () => {
  if (!weatherChartRef.value) return

  weatherChart = echarts.init(weatherChartRef.value)

  const option: echarts.EChartsOption = {
    title: {
      text: '近7天天气趋势',
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
      },
      formatter: (params: any) => {
        const date = params[0].axisValue
        let result = `<div style="font-weight: 600; margin-bottom: 8px; color: #1F2937;">${date}</div>`
        params.forEach((item: any) => {
          result += `<div style="display: flex; align-items: center; margin: 4px 0;">
            <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${item.color}; margin-right: 8px;"></span>
            <span style="color: #374151;">${item.seriesName}: ${item.value}°C</span>
          </div>`
        })
        return result
      }
    },
    legend: {
      data: ['最高温', '最低温'],
      top: 46,
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
      bottom: '3%',
      top: 100,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: weatherData.map(item => item.date),
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
      name: '温度 (°C)',
      nameTextStyle: {
        color: 'var(--color-text-secondary)',
        fontSize: 12,
        padding: [0, 0, 0, 40]
      },
      axisLabel: {
        color: 'var(--color-text-secondary)',
        fontSize: 12,
        formatter: '{value}°C'
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
        name: '最高温',
        type: 'line',
        data: weatherData.map(item => item.highTemp),
        smooth: true,
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
            { offset: 0, color: 'rgba(45, 125, 70, 0.25)' },
            { offset: 1, color: 'rgba(45, 125, 70, 0.02)' }
          ])
        }
      },
      {
        name: '最低温',
        type: 'line',
        data: weatherData.map(item => item.lowTemp),
        smooth: true,
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
            { offset: 0, color: 'rgba(58, 157, 92, 0.2)' },
            { offset: 1, color: 'rgba(58, 157, 92, 0.02)' }
          ])
        }
      }
    ]
  }

  weatherChart.setOption(option)
}

const initPestChart = () => {
  if (!pestChartRef.value) return

  pestChart = echarts.init(pestChartRef.value)

  const option: echarts.EChartsOption = {
    title: {
      text: '本年度病虫害发生趋势',
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
      },
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(45, 125, 70, 0.06)'
        }
      },
      formatter: (params: any) => {
        const item = params[0]
        return `<div style="font-weight: 600; color: #1F2937;">${item.axisValue}</div>
                <div style="color: #374151;">发生次数: <span style="color: #2D7D46; font-weight: 600;">${item.value}</span> 次</div>`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 80,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: pestData.map(item => item.month),
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
      name: '发生次数',
      nameTextStyle: {
        color: 'var(--color-text-secondary)',
        fontSize: 12,
        padding: [0, 0, 0, 40]
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
        name: '病虫害发生次数',
        type: 'bar',
        data: pestData.map(item => item.count),
        barWidth: '50%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#3A9D5C' },
            { offset: 0.5, color: '#2D7D46' },
            { offset: 1, color: '#1B5E32' }
          ]),
          borderRadius: [6, 6, 0, 0]
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#52C41A' },
              { offset: 0.5, color: '#3A9D5C' },
              { offset: 1, color: '#2D7D46' }
            ])
          }
        },
        label: {
          show: true,
          position: 'top',
          color: 'var(--color-text-secondary)',
          fontSize: 11
        }
      }
    ]
  }

  pestChart.setOption(option)
}

const handleResize = () => {
  weatherChart?.resize()
  pestChart?.resize()
}

onMounted(() => {
  lastUpdateTime.value = getCurrentTime()
  setTimeout(() => {
    initWeatherChart()
    initPestChart()
    window.addEventListener('resize', handleResize)
  }, 100)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  weatherChart?.dispose()
  pestChart?.dispose()
})
</script>

<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">农业趋势看板</h1>
        <p class="page-subtitle">实时监测天气变化与病虫害发生趋势</p>
      </div>
      <div class="header-right">
        <span class="update-time">
          <el-icon :size="14"><RefreshRight /></el-icon>
          最后更新：{{ lastUpdateTime }}
        </span>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-card__bar stat-card__bar--warning"></div>
        <div class="stat-card__content">
          <div class="stat-card__icon stat-card__icon--warning">
            <el-icon :size="22"><Sunny /></el-icon>
          </div>
          <div class="stat-card__info">
            <span class="stat-card__value">{{ todayTemp }}</span>
            <span class="stat-card__label">今日温度</span>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__bar stat-card__bar--danger"></div>
        <div class="stat-card__content">
          <div class="stat-card__icon stat-card__icon--danger">
            <el-icon :size="22"><Warning /></el-icon>
          </div>
          <div class="stat-card__info">
            <span class="stat-card__value">{{ pestCount }}</span>
            <span class="stat-card__label">病虫害数</span>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__bar stat-card__bar--primary"></div>
        <div class="stat-card__content">
          <div class="stat-card__icon stat-card__icon--primary">
            <el-icon :size="22"><BellFilled /></el-icon>
          </div>
          <div class="stat-card__info">
            <span class="stat-card__value">{{ warningCount }}</span>
            <span class="stat-card__label">预警数</span>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__bar stat-card__bar--success"></div>
        <div class="stat-card__content">
          <div class="stat-card__icon stat-card__icon--success">
            <el-icon :size="22"><RefreshRight /></el-icon>
          </div>
          <div class="stat-card__info">
            <span class="stat-card__value">{{ dataUpdate }}</span>
            <span class="stat-card__label">数据更新</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-row">
      <div class="chart-card">
        <div ref="weatherChartRef" class="chart-container"></div>
      </div>
      <div class="chart-card">
        <div ref="pestChartRef" class="chart-container"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 页面头部 */
.header-right {
  display: flex;
  align-items: center;
}

.update-time {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  background: var(--color-bg-card);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-sm);
}

/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
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

.stat-card__bar--warning {
  background: var(--color-warning);
}

.stat-card__bar--danger {
  background: var(--color-danger);
}

.stat-card__bar--primary {
  background: var(--color-primary);
}

.stat-card__bar--success {
  background: var(--color-success);
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

.stat-card__icon--warning {
  background: rgba(250, 173, 20, 0.1);
  color: var(--color-warning);
}

.stat-card__icon--danger {
  background: rgba(245, 34, 45, 0.1);
  color: var(--color-danger);
}

.stat-card__icon--primary {
  background: rgba(45, 125, 70, 0.1);
  color: var(--color-primary);
}

.stat-card__icon--success {
  background: rgba(82, 196, 26, 0.1);
  color: var(--color-success);
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
  min-height: 420px;
  padding: var(--spacing-md);
}

/* 响应式 */
@media (max-width: 1024px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }

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
