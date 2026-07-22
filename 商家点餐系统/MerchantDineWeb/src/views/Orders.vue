<template>
  <div class="orders-page">
    <h2 class="page-title">📦 我的订单</h2>
    <div v-if="!orders.length" class="empty-state"><span class="empty-emoji">📦</span><h3>暂无订单</h3><p>快去点餐吧~</p><el-button class="go-shop-btn" round @click="$router.push('/products')">去点餐</el-button></div>
    <div v-else class="order-list">
      <div v-for="o in orders" :key="o.id" class="order-card food-card" @click="$router.push(`/order-detail/${o.id}`)">
        <div class="oc-head">
          <span class="oc-no">{{ o.orderNo }}</span>
          <el-tag :type="stype(o.status)" size="small" effect="dark">{{ slabel(o.status) }}</el-tag>
        </div>
        <div class="oc-items">{{ o.items?.map(i=>`${i.productName}×${i.quantity}`).join('、') }}</div>
        <div class="oc-foot">
          <span class="oc-time">{{ o.createTime }}</span>
          <span class="oc-amount price-tag">{{ o.totalAmount }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserOrders } from '../api'
import { useUserStore } from '../stores/user'
const user = useUserStore(); const orders = ref([])
onMounted(async () => { try { orders.value = (await getUserOrders(user.user.id)).data.data || [] } catch(e){} })
function stype(s) { return {1:'warning',2:'success',3:'',4:'info'}[s]||'' }
function slabel(s) { return {1:'待支付',2:'已支付',3:'已完成',4:'已取消'}[s]||s }
</script>

<style scoped>
.orders-page { max-width: 820px; margin: 0 auto; }
.empty-state { text-align: center; padding: 100px 0; }
.empty-emoji { font-size: 80px; display: block; margin-bottom: 16px; }
.empty-state h3 { font-size: 22px; margin-bottom: 8px; }
.empty-state p { color: #8b6f65; margin-bottom: 24px; }
.go-shop-btn { background: #e65d3e; color: #fff; font-weight: 700; padding: 12px 36px; border: none; }
.order-list { display: flex; flex-direction: column; gap: 16px; }
.order-card { padding: 20px; cursor: pointer; }
.oc-head { display: flex; justify-content: space-between; margin-bottom: 12px; }
.oc-no { font-weight: 800; color: #2d1b14; }
.oc-items { color: #8b6f65; font-size: 14px; margin-bottom: 12px; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.oc-foot { display: flex; justify-content: space-between; }
.oc-time { color: #b08b7a; font-size: 13px; }
.oc-amount { font-size: 20px; }
</style>
