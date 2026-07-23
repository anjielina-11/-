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
  <ElContainer class="admin-container">
    <ElHeader class="admin-header">
      <div class="header-title">系统管理端 - 云南特色农业智能诊断与生产管理平台</div>
      <ElButton type="danger" @click="handleLogout">退出登录</ElButton>
    </ElHeader>
    <ElContainer>
      <Sidebar />
      <ElMain class="admin-main">
        <router-view />
      </ElMain>
    </ElContainer>
  </ElContainer>
</template>

<style scoped>
.admin-container {
  height: 100vh;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: white;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
}

.admin-main {
  background: #fafafa;
  padding: 0;
}
</style>