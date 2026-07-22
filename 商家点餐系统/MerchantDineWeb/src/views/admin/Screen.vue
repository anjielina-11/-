<template>
  <div class="screen-page">
    <div class="screen-top">
      <h2>📊 数据大屏</h2>
      <div class="top-right">
        <span class="auto-tip" :class="{ ticking: countdown <= 5 }">⏱ {{ countdown }}s 后自动刷新</span>
        <el-button class="refresh-btn" @click="fetchData" :loading="loading" round>🔄 刷新</el-button>
      </div>
    </div>

    <div class="kpi-row">
      <div class="kpi-card"><span class="kpi-icon">💰</span><div><div class="kpi-v">¥{{ data.todayAmount }}</div><div class="kpi-l">今日营业额</div></div></div>
      <div class="kpi-card"><span class="kpi-icon">📋</span><div><div class="kpi-v">{{ data.todayOrders }}</div><div class="kpi-l">今日订单数</div></div></div>
      <div class="kpi-card"><span class="kpi-icon">📅</span><div><div class="kpi-v">¥{{ data.monthAmount }}</div><div class="kpi-l">本月营业额</div></div></div>
      <div class="kpi-card"><span class="kpi-icon">🍽️</span><div><div class="kpi-v">{{ data.productCount }}</div><div class="kpi-l">商品总数</div></div></div>
    </div>

    <div class="chart-grid">
      <div class="chart-box"><h4>📈 今日营业额趋势</h4><div ref="lineChart" style="height:280px"></div></div>
      <div class="chart-box"><h4>🥧 分类销量占比</h4><div ref="pieChart" style="height:280px"></div></div>
      <div class="chart-box"><h4>📊 商品销量排行</h4><div ref="barChart" style="height:280px"></div></div>
      <div class="chart-box order-box">
        <h4>🕐 最新订单</h4>
        <div v-for="o in data.latestOrderList" :key="o.orderNo" class="lo-row">
          <div class="lo-info">
            <span class="lo-no">{{ o.orderNo }}</span>
            <span class="lo-user">{{ o.nickname }}</span>
          </div>
          <div class="lo-right">
            <el-tag :type="st(o.status)" size="small">{{ sl(o.status) }}</el-tag>
            <span class="lo-amt">¥{{ o.totalAmount }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { getDashboard } from '../../api'
import * as echarts from 'echarts'

const loading = ref(false), countdown = ref(30)
let timer = null
const data = reactive({ todayAmount:0, todayOrders:0, monthAmount:0, productCount:0, hourAmountList:[], categorySalesList:[], productSalesList:[], latestOrderList:[] })
const lineChart = ref(null), pieChart = ref(null), barChart = ref(null)

function st(s){return{1:'warning',2:'success',3:'',4:'info'}[s]||''}
function sl(s){return{1:'待支付',2:'已支付',3:'已完成',4:'已取消'}[s]||s}

async function fetchData() {
  loading.value = true
  try { Object.assign(data, (await getDashboard()).data.data); await nextTick(); renderCharts() }
  catch(e){}
  finally { loading.value = false; countdown.value = 30 }
}

function renderCharts() {
  const colors = ['#e65d3e','#f59e0b','#22c55e','#3b82f6','#8b5cf6','#ec4899']
  if (lineChart.value) {
    const c = echarts.getInstanceByDom(lineChart.value) || echarts.init(lineChart.value)
    c.setOption({ tooltip:{trigger:'axis'}, grid:{top:20,right:20,bottom:30,left:50}, xAxis:{type:'category',data:data.hourAmountList.map(h=>h.hour+':00')}, yAxis:{type:'value'}, series:[{data:data.hourAmountList.map(h=>h.amount),type:'line',smooth:true,areaStyle:{color:'#e65d3e15'},lineStyle:{color:'#e65d3e',width:3},itemStyle:{color:'#e65d3e'}}] })
  }
  if (pieChart.value) {
    const c = echarts.getInstanceByDom(pieChart.value) || echarts.init(pieChart.value)
    c.setOption({ tooltip:{trigger:'item'}, legend:{bottom:0}, color:colors, series:[{type:'pie',radius:['45%','75%'],center:['50%','45%'],data:data.categorySalesList.map(c=>({name:c.categoryName,value:c.quantity})),label:{show:false},emphasis:{scaleSize:8}}] })
  }
  if (barChart.value) {
    const c = echarts.getInstanceByDom(barChart.value) || echarts.init(barChart.value)
    c.setOption({ tooltip:{trigger:'axis'}, grid:{top:10,right:20,bottom:30,left:50}, xAxis:{type:'category',data:data.productSalesList.map(p=>p.productName),axisLabel:{rotate:20}}, yAxis:{type:'value'}, series:[{data:data.productSalesList.map(p=>p.quantity),type:'bar',barWidth:28,itemStyle:{color:'#e65d3e',borderRadius:[6,6,0,0]}}] })
  }
}

onMounted(()=>{fetchData();timer=setInterval(()=>{countdown.value--;if(countdown.value<=0)fetchData()},1000)})
onUnmounted(()=>clearInterval(timer))
</script>

<style scoped>
.screen-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.top-right { display: flex; align-items: center; gap: 12px; }
.auto-tip { color: #8b6f65; font-size: 13px; }
.auto-tip.ticking { color: #e65d3e; font-weight: 700; }
.refresh-btn { background: #fff; border: 1px solid #e5d5c8; font-weight: 600; }
.kpi-row { display: grid; grid-template-columns: repeat(4,1fr); gap: 16px; margin-bottom: 24px; }
.kpi-card { background: #fff; border-radius: 14px; padding: 22px; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 10px rgba(0,0,0,.04); }
.kpi-icon { font-size: 36px; }
.kpi-v { font-size: 26px; font-weight: 800; color: #e65d3e; }
.kpi-l { color: #8b6f65; font-size: 13px; margin-top: 2px; }
.chart-grid { display: grid; grid-template-columns: repeat(2,1fr); gap: 16px; }
.chart-box { background: #fff; border-radius: 14px; padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,.04); }
.chart-box h4 { margin-bottom: 8px; font-size: 15px; font-weight: 700; }
.order-box { max-height: 340px; overflow-y: auto; }
.lo-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f5f0eb; font-size: 13px; }
.lo-info { display: flex; gap: 10px; }
.lo-no { font-weight: 700; }
.lo-user { color: #8b6f65; }
.lo-right { display: flex; gap: 8px; align-items: center; }
.lo-amt { font-weight: 700; color: #e65d3e; }
</style>
