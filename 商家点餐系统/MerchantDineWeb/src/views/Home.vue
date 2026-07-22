<template>
  <div class="home">
    <section class="hero-banner">
      <div class="hero-bg"></div>
      <div class="hero-overlay">
        <span class="hero-badge">🔥 新店开业特惠</span>
        <h1>美味即刻享<br>点餐更轻松</h1>
        <p>精选新鲜食材 · 匠心烹饪 · 极致美食体验</p>
        <el-button class="hero-btn" size="large" round @click="$router.push('/products')">
          立即点餐 <span style="font-size:18px">→</span>
        </el-button>
      </div>
    </section>

    <section class="home-section">
      <h2 class="section-head">
        <span class="head-icon">📂</span>热门分类
        <router-link to="/products" class="head-link">查看全部 →</router-link>
      </h2>
      <div class="cat-scroll">
        <div v-for="cat in categories" :key="cat.id" class="cat-chip" @click="$router.push({path:'/products',query:{categoryId:cat.id}})">
          <span class="cat-emoji">{{ catEmoji(cat.name) }}</span>
          <span class="cat-name">{{ cat.name }}</span>
        </div>
      </div>
    </section>

    <section class="home-section">
      <h2 class="section-head"><span class="head-icon">🔥</span>热销菜品<span class="head-badge">TOP 4</span></h2>
      <div class="product-grid">
        <div v-for="p in hotProducts" :key="p.id" class="food-card product-card">
          <div class="food-img-wrap" style="height:200px">
            <img :src="p.image" @error="onImgError" :alt="p.name" />
            <div v-if="p.sales" class="sales-badge">🔥 已售 {{ p.sales }}</div>
          </div>
          <div class="prod-body">
            <h4 class="prod-name">{{ p.name }}</h4>
            <p class="prod-desc">{{ p.description || '美味可口，值得品尝' }}</p>
            <div class="prod-footer">
              <span class="price-tag">{{ p.price }}</span>
              <el-button class="add-btn" round size="small" @click.stop="addToCart(p)">
                <span>+</span> 加购
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCategories, getProducts } from '../api'
import { useCartStore } from '../stores/cart'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const cart = useCartStore()
const user = useUserStore()
const categories = ref([])
const hotProducts = ref([])

const catEmojis = { '招牌推荐':'🌟','热菜':'🔥','凉菜':'🥒','汤类':'🍲','主食':'🍚','酒水饮料':'🥤','双人套餐':'👫','四人套餐':'👨‍👩‍👧‍👦' }
function catEmoji(name) { return catEmojis[name] || '📋' }
function onImgError(e) { e.target.style.display = 'none'; e.target.nextElementSibling && (e.target.nextElementSibling.style.display = 'flex') }

onMounted(async () => {
  const [cr, pr] = await Promise.all([getCategories(), getProducts()])
  categories.value = cr.data.data || []
  hotProducts.value = ((pr.data.data) || []).sort((a,b)=>(b.sales||0)-(a.sales||0)).slice(0,4)
})

function addToCart(p) {
  if (!user.isLoggedIn) { ElMessage.warning('请先登录'); return }
  cart.addItem(p); ElMessage.success(`已加购 ${p.name}`)
}
</script>

<style scoped>
.hero-banner { position: relative; border-radius: var(--radius-lg); overflow: hidden; margin-bottom: 40px; min-height: 320px; display: flex; align-items: center; }
.hero-bg { position: absolute; inset: 0; background: linear-gradient(135deg, #e65d3e 0%, #d9462a 40%, #f59e0b 100%); }
.hero-bg::after { content: ''; position: absolute; right: 0; top: 0; width: 50%; height: 100%; background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 200'%3E%3Ccircle cx='100' cy='100' r='80' fill='rgba(255,255,255,.05)'/%3E%3Ccircle cx='160' cy='40' r='40' fill='rgba(255,255,255,.04)'/%3E%3C/svg%3E") no-repeat center/cover; }
.hero-overlay { position: relative; z-index: 1; padding: 60px; color: #fff; }
.hero-badge { display: inline-block; background: rgba(255,255,255,.2); backdrop-filter: blur(8px); padding: 6px 16px; border-radius: 20px; font-size: 13px; font-weight: 600; margin-bottom: 16px; }
.hero-overlay h1 { font-size: 44px; font-weight: 900; line-height: 1.2; margin-bottom: 12px; letter-spacing: -1px; }
.hero-overlay p { font-size: 17px; opacity: .9; margin-bottom: 28px; }
.hero-btn { background: #fff; color: #e65d3e; font-weight: 800; font-size: 16px; padding: 14px 36px; border: none; }
.hero-btn:hover { background: #fef3e8; }
.home-section { margin-bottom: 40px; }
.section-head { display: flex; align-items: center; gap: 10px; font-size: 22px; font-weight: 800; margin-bottom: 20px; }
.head-icon { font-size: 26px; }
.head-link { margin-left: auto; font-size: 14px; color: var(--food-primary); font-weight: 600; }
.head-badge { font-size: 12px; background: #fef3e8; color: #e65d3e; padding: 2px 10px; border-radius: 10px; font-weight: 700; }
.cat-scroll { display: flex; gap: 12px; flex-wrap: wrap; }
.cat-chip { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 20px 28px; background: #fff; border-radius: var(--radius-md); cursor: pointer; box-shadow: var(--shadow-sm); transition: all var(--transition); min-width: 100px; }
.cat-chip:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); background: #fef3e8; }
.cat-emoji { font-size: 32px; }
.cat-name { font-size: 13px; font-weight: 700; color: var(--food-text); }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.product-card { position: relative; }
.food-img-wrap { background: linear-gradient(135deg, #fef3c7, #fde68a); position: relative; }
.food-img-wrap img { width: 100%; height: 100%; object-fit: cover; }
.sales-badge { position: absolute; top: 12px; right: 12px; background: rgba(0,0,0,.55); color: #fff; font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 10px; }
.prod-body { padding: 18px; }
.prod-name { font-size: 17px; font-weight: 700; margin-bottom: 6px; }
.prod-desc { color: #8b6f65; font-size: 13px; margin-bottom: 14px; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.prod-footer { display: flex; justify-content: space-between; align-items: center; }
.add-btn { background: #fef3e8; color: #e65d3e; border: none; font-weight: 700; }
.add-btn:hover { background: #e65d3e; color: #fff; }
.add-btn span { font-size: 16px; margin-right: 2px; }
</style>
