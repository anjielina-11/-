<template>
  <div class="products-page">
    <aside class="prod-sidebar">
      <h3>📂 商品分类</h3>
      <div class="cat-item" :class="{ active: activeCat === null }" @click="activeCat = null">
        <span>📋</span> 全部
      </div>
      <div v-for="cat in categories" :key="cat.id" class="cat-item" :class="{ active: activeCat === cat.id }" @click="activeCat = cat.id">
        <span>{{ catEmoji(cat.name) }}</span> {{ cat.name }}
      </div>
    </aside>
    <div class="prod-main">
      <div class="prod-toolbar">
        <el-input v-model="keyword" placeholder="🔍 搜索菜品..." clearable class="search-input" />
        <span class="prod-count">共 {{ filteredProducts.length }} 件</span>
      </div>
      <div class="product-grid">
        <div v-for="p in filteredProducts" :key="p.id" class="food-card prod-card">
          <div class="food-img-wrap" style="height:190px">
            <img :src="p.image" @error="e => e.target.style.display='none'" :alt="p.name" />
            <div class="img-fallback" v-if="!p.image">🍽️</div>
          </div>
          <div class="prod-body">
            <div class="prod-header">
              <h4>{{ p.name }}</h4>
              <el-tag v-if="p.stock <= 10" size="small" type="warning">仅剩{{ p.stock }}</el-tag>
            </div>
            <p class="prod-desc">{{ p.description || '暂无描述' }}</p>
            <div class="prod-footer">
              <span class="price-tag">{{ p.price }}</span>
              <div class="qty-ctrl">
                <el-button size="small" circle class="qty-btn" @click="decreaseQty(p)">−</el-button>
                <span class="qty-num">{{ getQty(p.id) }}</span>
                <el-button size="small" circle class="qty-btn plus" @click="increaseQty(p)">+</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!filteredProducts.length" class="empty-state">
        <span style="font-size:60px">🍽️</span>
        <p>暂无匹配商品</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getCategories, getProducts } from '../api'
import { useCartStore } from '../stores/cart'

const route = useRoute()
const cart = useCartStore()
const categories = ref([]), products = ref([]), activeCat = ref(null), keyword = ref(''), qtyMap = ref({})
const catEmojis = { '招牌推荐':'🌟','热菜':'🔥','凉菜':'🥒','汤类':'🍲','主食':'🍚','酒水饮料':'🥤','双人套餐':'👫','四人套餐':'👨‍👩‍👧‍👦' }
function catEmoji(n) { return catEmojis[n] || '📋' }

onMounted(async () => {
  const [cr, pr] = await Promise.all([getCategories(), getProducts()])
  categories.value = cr.data.data || []
  products.value = pr.data.data || []
  if (route.query.categoryId) activeCat.value = Number(route.query.categoryId)
})

const filteredProducts = computed(() => {
  let list = products.value
  if (activeCat.value) list = list.filter(p => p.categoryId === activeCat.value)
  if (keyword.value) list = list.filter(p => p.name.includes(keyword.value))
  return list
})

function getQty(pid) { return qtyMap.value[pid] || 0 }
function increaseQty(p) { qtyMap.value[p.id] = getQty(p.id) + 1; cart.addItem(p) }
function decreaseQty(p) { const q = getQty(p.id); if (q > 0) { qtyMap.value[p.id] = q - 1; cart.removeItem(p.id) } }
</script>

<style scoped>
.products-page { display: flex; gap: 24px; }
.prod-sidebar { width: 190px; flex-shrink: 0; }
.prod-sidebar h3 { margin-bottom: 14px; font-size: 16px; font-weight: 700; }
.cat-item { display: flex; align-items: center; gap: 8px; padding: 11px 14px; border-radius: 10px; cursor: pointer; font-size: 14px; font-weight: 600; color: #8b6f65; transition: all .2s; margin-bottom: 2px; }
.cat-item:hover, .cat-item.active { background: #fef3e8; color: #e65d3e; }
.prod-main { flex: 1; }
.prod-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; gap: 16px; }
.search-input { width: 320px; }
.search-input :deep(.el-input__wrapper) { border-radius: 12px; background: #fff; }
.prod-count { color: #8b6f65; font-size: 14px; font-weight: 600; white-space: nowrap; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(270px, 1fr)); gap: 20px; }
.prod-card { position: relative; }
.img-fallback { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: 60px; background: linear-gradient(135deg, #fef3c7, #fde68a); }
.prod-body { padding: 16px; }
.prod-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px; }
.prod-header h4 { font-size: 16px; font-weight: 700; }
.prod-desc { color: #8b6f65; font-size: 13px; margin-bottom: 14px; line-height: 1.4; }
.prod-footer { display: flex; justify-content: space-between; align-items: center; }
.qty-ctrl { display: flex; align-items: center; gap: 6px; }
.qty-btn { background: #f5f5f5; border: none; font-weight: 700; color: #666; width: 32px; height: 32px; }
.qty-btn.plus { background: #fef3e8; color: #e65d3e; }
.qty-num { width: 24px; text-align: center; font-weight: 700; }
.empty-state { text-align: center; padding: 80px 0; color: #8b6f65; }
.empty-state p { margin-top: 16px; font-size: 16px; }
</style>
