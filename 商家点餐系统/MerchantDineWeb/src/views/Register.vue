<template>
  <div class="register-page">
    <div class="register-card">
      <div class="reg-left">
        <div class="brand-showcase">
          <div class="step-indicator">
            <div class="step" :class="{ active: step >= 0, done: step >= 0 }"><span>1</span>填写信息</div>
            <div class="step-line" :class="{ done: step >= 1 }"></div>
            <div class="step" :class="{ active: step >= 1, done: step >= 1 }"><span>2</span>注册成功</div>
            <div class="step-line" :class="{ done: step >= 2 }"></div>
            <div class="step" :class="{ active: step >= 2, done: step >= 2 }"><span>3</span>开始点餐</div>
          </div>
          <div class="reg-emoji">🍽️</div>
          <p class="reg-tip">加入我们，享受美味生活</p>
        </div>
      </div>
      <div class="reg-right">
        <h2>创建账号 ✨</h2>
        <p class="subtitle">填写信息，即刻开启美食之旅</p>
        <el-form :model="form" :rules="rules" ref="formRef" size="large">
          <el-form-item prop="username"><el-input v-model="form.username" placeholder="用户名" prefix-icon="UserFilled" class="food-input" /></el-form-item>
          <el-form-item prop="password"><el-input v-model="form.password" type="password" placeholder="密码（6-20位）" prefix-icon="Lock" show-password class="food-input" /></el-form-item>
          <el-form-item prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" show-password class="food-input" /></el-form-item>
          <el-form-item prop="nickname"><el-input v-model="form.nickname" placeholder="昵称" prefix-icon="EditPen" class="food-input" /></el-form-item>
          <el-form-item prop="phone"><el-input v-model="form.phone" placeholder="手机号" prefix-icon="Phone" class="food-input" /></el-form-item>
          <el-form-item><el-button class="btn-submit" @click="handleRegister" :loading="loading" round>注 册</el-button></el-form-item>
        </el-form>
        <p class="switch-link">已有账号？<router-link to="/login">返回登录 →</router-link></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null), loading = ref(false), step = ref(0)
const form = reactive({ username: '', password: '', confirmPassword: '', nickname: '', phone: '' })
const validatePass = (rule, value, callback) => { if (value !== form.password) callback(new Error('两次密码不一致')); else callback() }
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码6-20位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validatePass, trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}
async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.registerAction({ username: form.username, password: form.password, nickname: form.nickname, phone: form.phone })
    step.value = 1
    ElMessage.success('注册成功，即将跳转登录页！')
    setTimeout(() => { step.value = 2; router.push('/login') }, 1200)
  } catch (e) { ElMessage.error(e.response?.data?.message || '注册失败') }
  finally { loading.value = false }
}
</script>

<style scoped>
.register-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #fef3e8 0%, #fde2d0 30%, #fef9f0 70%, #fde8d5 100%); padding: 24px; }
.register-card { display: flex; width: 960px; background: #fff; border-radius: 20px; overflow: hidden; box-shadow: 0 20px 60px rgba(180,80,30,.15); }
.reg-left { flex: 1; background: linear-gradient(160deg, #e65d3e 0%, #d9462a 50%, #b91c1c 100%); color: #fff; display: flex; align-items: center; justify-content: center; padding: 50px; }
.step-indicator { display: flex; align-items: center; gap: 0; margin-bottom: 32px; }
.step { display: flex; flex-direction: column; align-items: center; gap: 6px; font-size: 12px; opacity: 0.5; }
.step span { width: 28px; height: 28px; border-radius: 50%; background: rgba(255,255,255,.2); display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; }
.step.active, .step.done { opacity: 1; }
.step.done span { background: #22c55e; }
.step-line { width: 50px; height: 2px; background: rgba(255,255,255,.2); margin: 0 8px; margin-bottom: 22px; }
.step-line.done { background: #22c55e; }
.reg-emoji { font-size: 60px; text-align: center; margin-bottom: 12px; }
.reg-tip { text-align: center; opacity: .85; }
.reg-right { flex: 1; padding: 40px 48px; display: flex; flex-direction: column; justify-content: center; }
.reg-right h2 { font-size: 26px; font-weight: 800; margin-bottom: 4px; }
.subtitle { color: #8b6f65; margin-bottom: 20px; }
.food-input :deep(.el-input__wrapper) { border-radius: 12px; background: #faf7f2; box-shadow: none; padding: 4px 12px; }
.btn-submit { width: 100%; height: 48px; background: linear-gradient(135deg, #e65d3e, #d9462a); color: #fff; font-size: 16px; font-weight: 700; border: none; letter-spacing: 2px; }
.switch-link { text-align: center; font-size: 14px; color: #8b6f65; }
.switch-link a { color: #e65d3e; font-weight: 600; }
</style>
