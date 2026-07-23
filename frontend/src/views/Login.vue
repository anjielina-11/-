<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElForm, ElFormItem, ElInput, ElButton, ElCheckbox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const userStore = useUserStore()

const form = reactive({
  username: '',
  password: ''
})

const loading = ref(false)
const showPassword = ref(false)
const rememberMe = ref(false)

const testAccounts = [
  { role: '农户端', username: 'farmer', password: 'farmer123', color: '#52C41A' },
  { role: '农技员端', username: 'tech', password: 'tech123', color: '#1890FF' },
  { role: '合作社端', username: 'coop', password: 'coop123', color: '#FAAD14' },
  { role: '管理员端', username: 'admin', password: 'admin123', color: '#F5222D' }
]

const fillAccount = (username: string, password: string) => {
  form.username = username
  form.password = password
}

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true

  try {
    await userStore.login(form)
    router.push(userStore.getDefaultPath())
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 左侧品牌展示区 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo">
          <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M16 2C9 2 4 8 4 14c0 4 2 7 5 9v5a2 2 0 002 2h10a2 2 0 002-2v-5c3-2 5-5 5-9 0-6-5-12-12-12z" fill="#52C41A"/>
            <path d="M12 16c0-2.2 1.8-4 4-4s4 1.8 4 4" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
            <path d="M10 22h12M13 26h6" stroke="#fff" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <span class="brand-name">云农智诊</span>
        </div>

        <h1 class="brand-title">云南特色农业<br/>智能诊断与生产管理平台</h1>
        <p class="brand-subtitle">AI驱动的智慧农业解决方案</p>

        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            </div>
            <div class="feature-text">
              <h4>智能病害识别</h4>
              <p>AI模型精准识别20+种作物病害</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-4"/></svg>
            </div>
            <div class="feature-text">
              <h4>生产全程管理</h4>
              <p>从种植到收获的数字化管理</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2z"/><path d="M22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7z"/></svg>
            </div>
            <div class="feature-text">
              <h4>农技知识赋能</h4>
              <p>RAG检索+多Agent综合决策</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 装饰元素 -->
      <div class="brand-decoration">
        <div class="deco-circle deco-circle-1"></div>
        <div class="deco-circle deco-circle-2"></div>
        <div class="deco-circle deco-circle-3"></div>
        <svg class="deco-leaf deco-leaf-1" viewBox="0 0 60 80" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M30 5C30 5 5 20 5 45C5 60 16 75 30 75C44 75 55 60 55 45C55 20 30 5 30 5Z" fill="rgba(255,255,255,0.04)" stroke="rgba(255,255,255,0.08)" stroke-width="1"/>
          <path d="M30 15V70" stroke="rgba(255,255,255,0.06)" stroke-width="1"/>
          <path d="M30 30L18 42" stroke="rgba(255,255,255,0.06)" stroke-width="1"/>
          <path d="M30 40L42 52" stroke="rgba(255,255,255,0.06)" stroke-width="1"/>
        </svg>
        <svg class="deco-leaf deco-leaf-2" viewBox="0 0 60 80" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M30 5C30 5 5 20 5 45C5 60 16 75 30 75C44 75 55 60 55 45C55 20 30 5 30 5Z" fill="rgba(255,255,255,0.03)" stroke="rgba(255,255,255,0.06)" stroke-width="1"/>
          <path d="M30 15V70" stroke="rgba(255,255,255,0.05)" stroke-width="1"/>
          <path d="M30 30L18 42" stroke="rgba(255,255,255,0.05)" stroke-width="1"/>
          <path d="M30 40L42 52" stroke="rgba(255,255,255,0.05)" stroke-width="1"/>
        </svg>
      </div>
    </div>

    <!-- 右侧登录表单区 -->
    <div class="login-form-section">
      <div class="form-wrapper">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>请登录您的账户</p>
        </div>

        <ElForm :model="form" class="login-form" @submit.prevent="handleLogin">
          <ElFormItem>
            <ElInput
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
            >
              <template #prefix>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:16px;height:16px"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </template>
            </ElInput>
          </ElFormItem>
          <ElFormItem>
            <ElInput
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              size="large"
            >
              <template #prefix>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:16px;height:16px"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
              </template>
              <template #suffix>
                <span class="toggle-password" @click="showPassword = !showPassword">
                  <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:16px;height:16px;cursor:pointer"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:16px;height:16px;cursor:pointer"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                </span>
              </template>
            </ElInput>
          </ElFormItem>

          <div class="form-options">
            <ElCheckbox v-model="rememberMe">记住我</ElCheckbox>
          </div>

          <ElButton
            type="primary"
            class="login-btn"
            size="large"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </ElButton>
        </ElForm>

        <div class="test-accounts">
          <div class="test-title">测试账号 · 一键注入</div>
          <div class="test-list">
            <div
              v-for="account in testAccounts"
              :key="account.username"
              class="test-item"
              @click="fillAccount(account.username, account.password)"
            >
              <span class="test-role" :style="{ color: account.color }">{{ account.role }}</span>
              <div class="test-cred">
                <span class="test-user"><strong>{{ account.username }}</strong></span>
                <span class="test-sep">/</span>
                <span class="test-pass">{{ account.password }}</span>
              </div>
              <span class="test-fill">点击注入</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg-card, #FFFFFF);
}

/* ========== 左侧品牌展示区 ========== */
.login-brand {
  flex: 0 0 60%;
  background: linear-gradient(135deg, #1B3A26 0%, #2D7D46 50%, #1B5E32 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 60px;
}

.brand-content {
  position: relative;
  z-index: 2;
  max-width: 520px;
  animation: slideInLeft 0.8s ease-out;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-40px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
}

.brand-logo svg {
  width: 44px;
  height: 44px;
}

.brand-name {
  font-size: 24px;
  font-weight: 700;
  color: #FFFFFF;
  letter-spacing: 3px;
}

.brand-title {
  font-size: 36px;
  font-weight: 700;
  color: #FFFFFF;
  line-height: 1.4;
  margin-bottom: 16px;
  letter-spacing: 1px;
}

.brand-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 48px;
  letter-spacing: 1px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-md, 12px);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all var(--transition-normal, 0.25s ease);
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.12);
  transform: translateX(4px);
}

.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(82, 196, 26, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #52C41A;
}

.feature-icon svg {
  width: 22px;
  height: 22px;
}

.feature-text h4 {
  font-size: 15px;
  font-weight: 600;
  color: #FFFFFF;
  margin-bottom: 4px;
}

.feature-text p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

/* ========== 装饰元素 ========== */
.brand-decoration {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.deco-circle-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  right: -100px;
  background: radial-gradient(circle, rgba(82, 196, 26, 0.1) 0%, transparent 70%);
}

.deco-circle-2 {
  width: 300px;
  height: 300px;
  bottom: -80px;
  left: -80px;
  background: radial-gradient(circle, rgba(250, 173, 20, 0.08) 0%, transparent 70%);
}

.deco-circle-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  right: 20%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.05) 0%, transparent 70%);
}

.deco-leaf {
  position: absolute;
  pointer-events: none;
}

.deco-leaf-1 {
  width: 120px;
  height: 160px;
  bottom: 8%;
  right: 5%;
  opacity: 0.6;
  animation: floatLeaf 8s ease-in-out infinite;
}

.deco-leaf-2 {
  width: 80px;
  height: 110px;
  top: 12%;
  right: 15%;
  opacity: 0.4;
  animation: floatLeaf 10s ease-in-out infinite reverse;
}

@keyframes floatLeaf {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-15px) rotate(5deg);
  }
}

/* ========== 右侧登录表单区 ========== */
.login-form-section {
  flex: 0 0 40%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  background: #FFFFFF;
}

.form-wrapper {
  width: 100%;
  max-width: 380px;
  animation: slideInRight 0.8s ease-out;
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(40px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.form-header {
  margin-bottom: 36px;
}

.form-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary, #1F2937);
  margin-bottom: 8px;
}

.form-header p {
  font-size: 15px;
  color: var(--color-text-secondary, #6B7280);
}

/* ========== 表单样式 ========== */
.login-form {
  margin-bottom: 32px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm, 8px);
  padding: 4px 12px;
  box-shadow: 0 0 0 1px var(--color-border, #E5E7EB) inset;
  transition: all var(--transition-fast, 0.15s ease);
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--color-primary, #2D7D46) inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-primary, #2D7D46) inset,
    0 0 0 3px rgba(45, 125, 70, 0.1);
}

.login-form :deep(.el-input__prefix) {
  color: var(--color-text-placeholder, #9CA3AF);
  transition: color var(--transition-fast, 0.15s ease);
}

.login-form :deep(.el-input__wrapper.is-focus .el-input__prefix) {
  color: var(--color-primary, #2D7D46);
}

.toggle-password {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: var(--color-text-placeholder, #9CA3AF);
  transition: color var(--transition-fast, 0.15s ease);
}

.toggle-password:hover {
  color: var(--color-primary, #2D7D46);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: var(--radius-sm, 8px);
  letter-spacing: 4px;
  transition: all var(--transition-normal, 0.25s ease);
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(45, 125, 70, 0.3);
}

.login-btn:active {
  transform: translateY(0);
}

/* ========== 测试账号 ========== */
.test-accounts {
  padding: 20px;
  background: var(--color-bg-page, #F0F5F1);
  border-radius: var(--radius-md, 12px);
  border: 1px solid var(--color-border-light, #F3F4F6);
}

.test-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary, #6B7280);
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.test-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.test-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #FFFFFF;
  border-radius: var(--radius-sm, 8px);
  font-size: 13px;
  transition: all var(--transition-fast, 0.15s ease);
  cursor: pointer;
  border: 1px solid transparent;
}

.test-item:hover {
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(45, 125, 70, 0.06));
  border-color: var(--color-border-light, #E5E7EB);
  transform: translateX(2px);
}

.test-item:active {
  transform: translateX(1px);
}

.test-role {
  font-weight: 600;
  min-width: 70px;
}

.test-cred {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  justify-content: center;
}

.test-user {
  color: var(--color-primary, #2D7D46);
}

.test-sep {
  color: var(--color-text-placeholder, #9CA3AF);
}

.test-pass {
  color: var(--color-text-secondary, #6B7280);
  font-family: monospace;
}

.test-fill {
  font-size: 12px;
  color: var(--color-primary, #2D7D46);
  font-weight: 500;
  padding: 4px 10px;
  background: rgba(45, 125, 70, 0.08);
  border-radius: 12px;
  transition: all var(--transition-fast, 0.15s ease);
}

.test-item:hover .test-fill {
  background: rgba(45, 125, 70, 0.15);
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }

  .login-brand {
    display: none;
  }

  .login-form-section {
    flex: 1;
    padding: 32px 24px;
  }

  .form-wrapper {
    max-width: 100%;
  }
}
</style>
