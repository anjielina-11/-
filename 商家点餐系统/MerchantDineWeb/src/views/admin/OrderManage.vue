<template>
  <div class="admin-page">
    <div class="ap-head"><h2 class="page-title">📦 订单管理</h2></div>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="🔍 搜索订单号..." clearable style="width:280px" class="food-input" />
      <span class="count-badge">共 {{ filteredOrders.length }} 笔</span>
    </div>
    <div class="food-card" style="padding:0;overflow:hidden">
      <el-table :data="filteredOrders" stripe>
        <el-table-column prop="orderNo" label="订单编号" width="180"><template #default="{r}"><span style="font-weight:700">{{r.orderNo}}</span></template></el-table-column>
        <el-table-column prop="userName" label="用户" width="100"/>
        <el-table-column label="金额" width="100"><template #default="{r}"><span class="price-tag">{{r.totalAmount}}</span></template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{r}"><el-tag :type="st(r.status)" size="small" effect="dark">{{sl(r.status)}}</el-tag></template></el-table-column>
        <el-table-column label="商品" min-width="200"><template #default="{r}">{{r.items?.map(i=>i.productName).join('、')}}</template></el-table-column>
        <el-table-column label="时间" width="170"><template #default="{r}"><span style="font-size:12px;color:#8b6f65">{{r.createTime}}</span></template></el-table-column>
        <el-table-column label="操作" width="80" fixed="right"><template #default="{r}"><el-button size="small" text type="danger" @click="handleDelete(r)">删除</el-button></template></el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAllOrders, deleteOrder } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
const orders=ref([]),keyword=ref('')
onMounted(async()=>{try{orders.value=((await getAllOrders()).data.data)||[]}catch(e){}})
const filteredOrders=computed(()=>keyword.value?orders.value.filter(o=>o.orderNo.includes(keyword.value)):orders.value)
function st(s){return{1:'warning',2:'success',3:'',4:'info'}[s]||''}
function sl(s){return{1:'待支付',2:'已支付',3:'已完成',4:'已取消'}[s]||s}
async function handleDelete(r){await ElMessageBox.confirm(`删除订单"${r.orderNo}"？`,'确认',{type:'warning'});try{await deleteOrder(r.id);ElMessage.success('已删除');orders.value=((await getAllOrders()).data.data)||[]}catch(e){ElMessage.error(e.response?.data?.message||'失败')}}
</script>

<style scoped>
.admin-page{max-width:1400px}.ap-head{margin-bottom:20px}.toolbar{display:flex;gap:12px;align-items:center;margin-bottom:16px}.count-badge{color:#8b6f65;font-size:14px;font-weight:600}
</style>
