<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElRow, ElCol, ElCard } from 'element-plus'
import * as echarts from 'echarts'

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

  weatherChart = echarts.init(weatherChartRef.value, 'dark')

  const option: echarts.EChartsOption = {
    title: {
      text: '近7天天气趋势',
      left: 'center',
      top: 10,
      textStyle: {
        color: '#fff',
        fontSize: 16,
        fontWeight: 'bold'
      }
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#ccc',
      textStyle: {
        color: '#333'
      },
      formatter: (params: any) => {
        const date = params[0].axisValue
        let result = `<div style="font-weight: bold; margin-bottom: 8px;">${date}</div>`
        params.forEach((item: any) => {
          result += `<div style="display: flex; align-items: center; margin: 4px 0;">
            <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${item.color}; margin-right: 8px;"></span>
            <span>${item.seriesName}: ${item.value}°C</span>
          </div>`
        })
        return result
      }
    },
    legend: {
      data: ['最高温', '最低温'],
      top: 40,
      textStyle: {
        color: '#fff'
      }
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
        color: '#999'
      },
      axisLine: {
        lineStyle: {
          color: '#555'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '温度 (°C)',
      nameTextStyle: {
        color: '#999',
        padding: [0, 0, 0, 50]
      },
      axisLabel: {
        color: '#999',
        formatter: '{value}°C'
      },
      axisLine: {
        lineStyle: {
          color: '#555'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#333'
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
          color: '#ff6b6b',
          width: 3
        },
        itemStyle: {
          color: '#ff6b6b',
          borderWidth: 2,
          borderColor: '#fff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 107, 107, 0.3)' },
            { offset: 1, color: 'rgba(255, 107, 107, 0.05)' }
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
          color: '#4ecdc4',
          width: 3
        },
        itemStyle: {
          color: '#4ecdc4',
          borderWidth: 2,
          borderColor: '#fff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(78, 205, 196, 0.3)' },
            { offset: 1, color: 'rgba(78, 205, 196, 0.05)' }
          ])
        }
      }
    ]
  }

  weatherChart.setOption(option)
}

const initPestChart = () => {
  if (!pestChartRef.value) return

  pestChart = echarts.init(pestChartRef.value, 'dark')

  const option: echarts.EChartsOption = {
    title: {
      text: '本年度病虫害发生趋势',
      left: 'center',
      top: 10,
      textStyle: {
        color: '#fff',
        fontSize: 16,
        fontWeight: 'bold'
      }
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#ccc',
      textStyle: {
        color: '#333'
      },
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const item = params[0]
        return `<div style="font-weight: bold;">${item.axisValue}</div>
                <div>发生次数: <span style="color: #4ade80; font-weight: bold;">${item.value}</span> 次</div>`
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
        color: '#999'
      },
      axisLine: {
        lineStyle: {
          color: '#555'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '发生次数',
      nameTextStyle: {
        color: '#999',
        padding: [0, 0, 0, 50]
      },
      axisLabel: {
        color: '#999'
      },
      axisLine: {
        lineStyle: {
          color: '#555'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#333'
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
            { offset: 0, color: '#4ade80' },
            { offset: 0.5, color: '#22c55e' },
            { offset: 1, color: '#16a34a' }
          ]),
          borderRadius: [6, 6, 0, 0]
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#86efac' },
              { offset: 0.5, color: '#4ade80' },
              { offset: 1, color: '#22c55e' }
            ])
          }
        },
        label: {
          show: true,
          position: 'top',
          color: '#fff',
          fontSize: 12
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
  <div class="dashboard-container">
    <div class="dashboard-header">
      <h2>农业趋势看板</h2>
      <span class="update-time">最后更新：{{ lastUpdateTime }}</span>
    </div>

    <ElRow :gutter="20">
      <ElCol :span="12">
        <ElCard class="chart-card">
          <div ref="weatherChartRef" class="chart-container"></div>
        </ElCard>
      </ElCol>
      <ElCol :span="12">
        <ElCard class="chart-card">
          <div ref="pestChartRef" class="chart-container"></div>
        </ElCard>
      </ElCol>
    </ElRow>
  </div>
</template>

<style scoped>
.dashboard-container {
  padding: 20px;
  background: #fafafa;
  min-height: calc(100vh - 60px);
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dashboard-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.update-time {
  font-size: 14px;
  color: #909399;
}

.chart-card {
  height: 100%;
}

.chart-container {
  width: 100%;
  min-height: 400px;
}
</style>