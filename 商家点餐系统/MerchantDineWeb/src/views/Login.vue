<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-left">
        <div class="brand-showcase">
          <div class="brand-emoji-group">
            <span class="be be1">🍜</span><span class="be be2">🥘</span><span class="be be3">🍱</span><span class="be be4">🍲</span>
          </div>
          <h1>美味商家</h1>
          <p class="brand-desc">智能点餐 · 高效运营 · 数据驱动</p>
          <div class="brand-points">
            <div class="bp-item"><span>📱</span> 扫码点餐，告别排队</div>
            <div class="bp-item"><span>📊</span> 实时数据，精准运营</div>
            <div class="bp-item"><span>🚀</span> 智慧厨房，高效出餐</div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <h2>欢迎回来 👋</h2>
        <p class="subtitle">登录您的账号继续点餐</p>
        <el-form :model="form" :rules="rules" ref="formRef" size="large" class="login-form">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" :prefix-icon="UserFilled" class="food-input" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password class="food-input" />
          </el-form-item>
          <el-form-item>
            <el-button class="btn-submit" @click="handleLogin" :loading="loading" round>登 录</el-button>
          </el-form-item>
        </el-form>

        <div class="demo-section">
          <p class="demo-title">🔑 体验账号快速登录</p>
          <div class="demo-btns">
            <div class="demo-card" @click="fillDemo('admin','123456')">
              <span class="demo-emoji">👨‍🍳</span>
              <span class="demo-label">管理员</span>
              <span class="demo-hint">admin / 123456</span>
            </div>
            <div class="demo-card" @click="fillDemo('user','123456')">
              <span class="demo-emoji">👤</span>
              <span class="demo-label">普通用户</span>
              <span class="demo-hint">user / 123456</span>
            </div>
          </div>
        </div>

        <p class="switch-link">还没有账号？<router-link to="/register">立即注册 →</router-link></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { UserFilled, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null), loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
function fillDemo(u, p) { form.username = u; form.password = p }
async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const u = await userStore.loginAction(form)
    ElMessage.success(`欢迎回来，${u.nickname || u.username}`)
    router.push('/')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '登录失败')
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #fef3e8 0%, #fde2d0 30%, #fef9f0 70%, #fde8d5 100%); padding: 24px; }
.login-card { display: flex; width: 960px; min-height: 580px; background: #fff; border-radius: 20px; overflow: hidden; box-shadow: 0 20px 60px rgba(180,80,30,.15); }
.login-left { flex: 1; background: linear-gradient(160deg, #e65d3e 0%, #d9462a 50%, #b91c1c 100%); color: #fff; display: flex; align-items: center; justify-content: center; padding: 50px; position: relative; overflow: hidden; }
.login-left::before { content: ''; position: absolute; top: -60px; right: -60px; width: 200px; height: 200px; background: rgba(255,255,255,.06); border-radius: 50%; }
.login-left::after { content: ''; position: absolute; bottom: -40px; left: -40px; width: 150px; height: 150px; background: rgba(255,255,255,.04); border-radius: 50%; }
.brand-showcase { text-align: center; position: relative; z-index: 1; }
.brand-emoji-group { display: flex; justify-content: center; gap: 12px; margin-bottom: 24px; }
.be { font-size: 36px; animation: float 3s ease-in-out infinite; }
.be2 { animation-delay: .5s; }.be3 { animation-delay: 1s; }.be4 { animation-delay: 1.5s; }
@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-8px); } }
.login-left h1 { font-size: 32px; font-weight: 800; margin-bottom: 8px; letter-spacing: -1px; }
.brand-desc { opacity: .85; font-size: 15px; margin-bottom: 32px; }
.brand-points { display: flex; flex-direction: column; gap: 14px; text-align: left; }
.bp-item { display: flex; align-items: center; gap: 10px; font-size: 15px; opacity: .9; }
.login-right { flex: 1; padding: 50px 48px; display: flex; flex-direction: column; justify-content: center; }
.login-right h2 { font-size: 26px; font-weight: 800; margin-bottom: 4px; }
.subtitle { color: #8b6f65; margin-bottom: 28px; }
.login-form { margin-bottom: 20px; }
.food-input :deep(.el-input__wrapper) { border-radius: 12px; background: #faf7f2; box-shadow: none; padding: 4px 12px; }
.btn-submit { width: 100%; height: 48px; background: linear-gradient(135deg, #e65d3e, #d9462a); color: #fff; font-size: 16px; font-weight: 700; border: none; letter-spacing: 2px; }
.btn-submit:hover { background: linear-gradient(135deg, #d9462a, #b91c1c); }
.demo-section { text-align: center; margin-bottom: 16px; }
.demo-title { font-size: 13px; color: #8b6f65; margin-bottom: 10px; }
.demo-btns { display: flex; gap: 10px; }
.demo-card { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 12px; border: 2px dashed #e5d5c8; border-radius: 12px; cursor: pointer; transition: all .2s; }
.demo-card:hover { border-color: #e65d3e; background: #fef3e8; }
.demo-emoji { font-size: 24px; }
.demo-label { font-size: 13px; font-weight: 700; }
.demo-hint { font-size: 11px; color: #999; }
.switch-link { text-align: center; font-size: 14px; color: #8b6f65; }
.switch-link a { color: #e65d3e; font-weight: 600; }
</style>
