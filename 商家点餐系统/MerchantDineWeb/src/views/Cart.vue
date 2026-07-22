<template>
  <div class="cart-page">
    <div class="cart-head">
      <h2 class="page-title">🛒 购物车</h2>
      <el-button v-if="cart.items.length" class="clear-btn" @click="cart.clearCart()">🗑️ 清空</el-button>
    </div>

    <div v-if="!cart.items.length" class="empty-state">
      <span class="empty-emoji">🛒</span>
      <h3>购物车是空的</h3>
      <p>快去挑选美食吧~</p>
      <el-button class="go-shop-btn" round @click="$router.push('/products')">去点餐</el-button>
    </div>

    <div v-else class="cart-grid">
      <div class="cart-list">
        <div v-for="item in cart.items" :key="item.productId" class="cart-item food-card">
          <div class="food-img-wrap" style="width:90px;height:90px;border-radius:10px;flex-shrink:0">
            <img :src="item.image" @error="e => e.target.style.display='none'" :alt="item.name" />
          </div>
          <div class="ci-info">
            <h4>{{ item.name }}</h4>
            <span class="ci-price price-tag">{{ item.price }}</span>
          </div>
          <div class="ci-qty">
            <el-button size="small" circle class="qty-btn" @click="cart.updateQuantity(item.productId, item.quantity - 1)">−</el-button>
            <span class="qty-num">{{ item.quantity }}</span>
            <el-button size="small" circle class="qty-btn plus" @click="cart.updateQuantity(item.productId, item.quantity + 1)">+</el-button>
          </div>
          <span class="ci-sub">{{ '¥' + (item.price * item.quantity).toFixed(2) }}</span>
          <el-button class="ci-del" text @click="cart.removeItem(item.productId)">🗑️</el-button>
        </div>
      </div>

      <div class="cart-summary food-card" style="padding:24px">
        <h3>订单摘要</h3>
        <div class="summary-row"><span>商品 {{ cart.totalCount }} 件</span><span>¥{{ cart.totalAmount.toFixed(2) }}</span></div>
        <div class="summary-row"><span>配送费</span><span class="free-tag">免费</span></div>
        <el-divider />
        <div class="summary-total"><span>合计</span><span class="total-price">¥{{ cart.totalAmount.toFixed(2) }}</span></div>
        <el-button class="checkout-btn" size="large" round @click="$router.push('/checkout')">去结算 →</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useCartStore } from '../stores/cart'
const cart = useCartStore()
</script>

<style scoped>
.cart-page { max-width: 1000px; margin: 0 auto; }
.cart-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.clear-btn { color: #999; font-weight: 600; }
.empty-state { text-align: center; padding: 100px 0; }
.empty-emoji { font-size: 80px; display: block; margin-bottom: 16px; }
.empty-state h3 { font-size: 22px; margin-bottom: 8px; }
.empty-state p { color: #8b6f65; margin-bottom: 24px; }
.go-shop-btn { background: #e65d3e; color: #fff; font-weight: 700; padding: 12px 36px; border: none; }
.cart-grid { display: flex; gap: 24px; }
.cart-list { flex: 1; display: flex; flex-direction: column; gap: 14px; }
.cart-item { display: flex; align-items: center; gap: 16px; padding: 16px; }
.ci-info { flex: 1; }
.ci-info h4 { font-size: 16px; font-weight: 700; margin-bottom: 6px; }
.ci-qty { display: flex; align-items: center; gap: 8px; }
.qty-btn { background: #f5f5f5; border: none; font-weight: 700; width: 30px; height: 30px; }
.qty-btn.plus { background: #fef3e8; color: #e65d3e; }
.qty-num { width: 28px; text-align: center; font-weight: 700; font-size: 16px; }
.ci-sub { font-size: 18px; font-weight: 800; color: #e65d3e; min-width: 90px; text-align: right; }
.ci-del { opacity: 0.3; transition: opacity .2s; padding: 4px; }
.ci-del:hover { opacity: 1; }
.cart-summary { width: 300px; align-self: flex-start; position: sticky; top: 88px; }
.cart-summary h3 { margin-bottom: 18px; font-size: 18px; }
.summary-row { display: flex; justify-content: space-between; margin-bottom: 12px; color: #8b6f65; font-size: 15px; }
.summary-total { display: flex; justify-content: space-between; font-size: 18px; font-weight: 700; margin-bottom: 20px; }
.total-price { color: #e65d3e; font-size: 26px; }
.free-tag { color: #22c55e; font-weight: 700; }
.checkout-btn { width: 100%; background: linear-gradient(135deg, #e65d3e, #d9462a); color: #fff; border: none; font-size: 16px; font-weight: 700; height: 50px; letter-spacing: 1px; }
</style>
