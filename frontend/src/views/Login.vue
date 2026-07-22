<script setup lang="ts">
import { ref } from 'vue'
import { ElForm, ElFormItem, ElInput, ElButton, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const userStore = useUserStore()

const form = ref({
  username: '',
  password: ''
})

const loading = ref(false)

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true

  try {
    await userStore.login(form.value)
    router.push(userStore.getDefaultPath())
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="login-title">云南特色农业智能诊断与生产管理平台</h2>
      <ElForm :model="form" label-width="80px" class="login-form">
        <ElFormItem label="用户名">
          <ElInput v-model="form.username" placeholder="请输入用户名" />
        </ElFormItem>
        <ElFormItem label="密码">
          <ElInput v-model="form.password" type="password" placeholder="请输入密码" />
        </ElFormItem>
        <ElFormItem>
          <ElButton type="primary" class="login-btn" :loading="loading" @click="handleLogin">
            登录
          </ElButton>
        </ElFormItem>
      </ElForm>
      <div class="test-accounts">
        <p>测试账号：</p>
        <p>农户端：用户名 <strong>farmer</strong>，密码 <strong>farmer123</strong></p>
        <p>农技员端：用户名 <strong>tech</strong>，密码 <strong>tech123</strong></p>
        <p>合作社端：用户名 <strong>coop</strong>，密码 <strong>coop123</strong></p>
        <p>管理员端：用户名 <strong>admin</strong>，密码 <strong>admin123</strong></p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 24px;
}

.login-form {
  margin-top: 20px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.test-accounts {
  margin-top: 20px;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 8px;
}

.test-accounts p {
  margin: 5px 0;
  font-size: 13px;
  color: #666;
}

.test-accounts strong {
  color: #409eff;
}
</style>