<script setup lang="ts">
import { ElContainer, ElHeader, ElMain, ElButton } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import Sidebar from '@/components/Sidebar.vue'

const userStore = useUserStore()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <ElContainer class="farmer-container">
    <ElHeader class="farmer-header">
      <div class="header-title">农户端 - 云南特色农业智能诊断与生产管理平台</div>
      <ElButton type="danger" @click="handleLogout">退出登录</ElButton>
    </ElHeader>
    <ElContainer>
      <Sidebar />
      <ElMain class="farmer-main">
        <router-view />
      </ElMain>
    </ElContainer>
  </ElContainer>
</template>

<style scoped>
.farmer-container {
  height: 100vh;
}

.farmer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
}

.farmer-main {
  background: #fafafa;
  padding: 0;
}
</style>