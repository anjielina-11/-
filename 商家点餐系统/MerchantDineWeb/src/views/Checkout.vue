<template>
  <div class="checkout-page">
    <div class="co-head"><el-button text @click="$router.back()">← 返回</el-button><h2>确认订单</h2></div>
    <div v-if="cart.items.length" class="co-grid">
      <div class="co-main">
        <div class="food-card" style="padding:24px">
          <h3>📋 商品清单</h3>
          <div v-for="item in cart.items" :key="item.productId" class="co-item">
            <div class="coi-left">
              <div class="food-img-wrap" style="width:50px;height:50px;border-radius:8px;flex-shrink:0">
                <img :src="item.image" @error="e=>e.target.style.display='none'" :alt="item.name"/>
              </div>
              <div><div class="coi-name">{{ item.name }}</div><span class="coi-price">¥{{ item.price }} × {{ item.quantity }}</span></div>
            </div>
            <span class="coi-sub">¥{{ (item.price*item.quantity).toFixed(2) }}</span>
          </div>
        </div>
        <div class="food-card" style="padding:24px;margin-top:16px">
          <h3>📝 订单备注</h3>
          <el-input v-model="remark" type="textarea" :rows="3" placeholder="如有特殊需求请备注..." class="remark-input" />
        </div>
      </div>
      <div class="co-side food-card" style="padding:24px;align-self:flex-start;position:sticky;top:88px">
        <h3>💰 金额明细</h3>
        <div class="sr"><span>商品金额</span><span>¥{{ cart.totalAmount.toFixed(2) }}</span></div>
        <div class="sr"><span>配送费</span><span class="free">免费</span></div>
        <el-divider />
        <div class="st"><span>实付</span><span class="tp">¥{{ cart.totalAmount.toFixed(2) }}</span></div>
        <el-button class="submit-btn" size="large" round @click="submitOrder" :loading="submitting">提交订单</el-button>
      </div>
    </div>
    <div v-else class="empty-state"><span>🛒</span><p>购物车为空</p></div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { useUserStore } from '../stores/user'
import { createOrder } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter(); const cart = useCartStore(); const user = useUserStore()
const remark = ref(''); const submitting = ref(false)
async function submitOrder() {
  submitting.value = true
  try {
    const items = cart.items.map(i => ({ productId: i.productId, productName: i.name, productPrice: i.price, productImage: i.image, quantity: i.quantity }))
    await createOrder({ userId: user.user.id, totalAmount: cart.totalAmount, remark: remark.value, items })
    ElMessage.success('下单成功！🎉'); cart.clearCart(); router.push('/orders')
  } catch(e) { ElMessage.error(e.response?.data?.message||'下单失败') }
  finally { submitting.value = false }
}
</script>

<style scoped>
.checkout-page { max-width: 900px; margin: 0 auto; }
.co-head { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.co-grid { display: flex; gap: 24px; }
.co-main { flex: 1; display: flex; flex-direction: column; }
.co-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 0; border-bottom: 1px solid #f5f0eb; }
.coi-left { display: flex; align-items: center; gap: 12px; }
.coi-name { font-weight: 700; font-size: 15px; }
.coi-price { color: #8b6f65; font-size: 13px; }
.coi-sub { font-weight: 800; color: #e65d3e; font-size: 16px; }
.remark-input :deep(textarea) { border-radius: 10px; background: #faf7f2; }
.co-side { width: 280px; }
.sr { display: flex; justify-content: space-between; margin-bottom: 10px; color: #8b6f65; }
.st { display: flex; justify-content: space-between; font-size: 18px; font-weight: 700; margin-bottom: 18px; }
.tp { color: #e65d3e; font-size: 24px; }
.free { color: #22c55e; font-weight: 700; }
.submit-btn { width: 100%; height: 48px; background: linear-gradient(135deg, #e65d3e, #d9462a); color: #fff; border: none; font-size: 16px; font-weight: 700; letter-spacing: 1px; }
.empty-state { text-align: center; padding: 100px 0; color: #8b6f65; font-size: 60px; }
</style>
