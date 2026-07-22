<template>
  <div class="detail-page">
    <div class="dh"><el-button text @click="$router.push('/orders')">← 返回</el-button><h2 class="page-title">订单详情</h2></div>
    <div v-if="order" class="detail-content">
      <div class="food-card info-card">
        <div class="info-grid">
          <div><span class="lbl">订单编号</span><span class="val">{{ order.orderNo }}</span></div>
          <div><span class="lbl">下单时间</span><span class="val">{{ order.createTime }}</span></div>
          <div><span class="lbl">支付时间</span><span class="val">{{ order.payTime || '—' }}</span></div>
          <div><span class="lbl">备注</span><span class="val">{{ order.remark || '—' }}</span></div>
          <div><span class="lbl">状态</span><el-tag :type="st(order.status)" effect="dark" size="small">{{ sl(order.status) }}</el-tag></div>
        </div>
      </div>
      <div class="food-card" style="padding:0;overflow:hidden">
        <el-table :data="order.items" style="width:100%">
          <el-table-column label="商品" width="80">
            <template #default="{row}"><img :src="row.productImage" @error="e=>e.target.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 60 60%22%3E%3Crect fill=%22%23fef3c7%22 width=%2260%22 height=%2260%22/%3E%3Ctext x=%2230%22 y=%2240%22 text-anchor=%22middle%22 font-size=%2230%22%3E🍽%3C/text%3E%3C/svg%3E'" style="width:50px;height:50px;border-radius:8px;object-fit:cover" /></template>
          </el-table-column>
          <el-table-column prop="productName" label="名称" />
          <el-table-column label="单价" width="90"><template #default="{r}">¥{{ r.productPrice }}</template></el-table-column>
          <el-table-column prop="quantity" label="数量" width="70" />
          <el-table-column label="小计" width="100"><template #default="{r}" >¥{{ r.amount }}</template></el-table-column>
        </el-table>
      </div>
      <div class="total-bar food-card"><span>订单总额</span><span class="price-tag" style="font-size:26px">{{ order.totalAmount }}</span></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail } from '../api'
const route = useRoute(); const order = ref(null)
onMounted(async () => { try { order.value = (await getOrderDetail(route.params.id)).data.data } catch(e){} })
function st(s) { return {1:'warning',2:'success',3:'',4:'info'}[s]||'' }
function sl(s) { return {1:'待支付',2:'已支付',3:'已完成',4:'已取消'}[s]||s }
</script>

<style scoped>
.detail-page { max-width: 900px; margin: 0 auto; }
.dh { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.detail-content { display: flex; flex-direction: column; gap: 20px; }
.info-card { padding: 24px; }
.info-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 18px; }
.info-grid div { display: flex; flex-direction: column; gap: 4px; }
.lbl { color: #8b6f65; font-size: 13px; }
.val { font-weight: 600; font-size: 15px; }
.total-bar { padding: 20px 24px; display: flex; justify-content: flex-end; align-items: center; gap: 16px; font-size: 18px; font-weight: 700; }
</style>
