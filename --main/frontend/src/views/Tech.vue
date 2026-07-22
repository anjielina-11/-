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
  <ElContainer class="tech-container">
    <ElHeader class="tech-header">
      <div class="header-title">农技人员端 - 云南特色农业智能诊断与生产管理平台</div>
      <ElButton type="danger" @click="handleLogout">退出登录</ElButton>
    </ElHeader>
    <ElContainer>
      <Sidebar />
      <ElMain class="tech-main">
        <router-view />
      </ElMain>
    </ElContainer>
  </ElContainer>
</template>

<style scoped>
.tech-container {
  height: 100vh;
}

.tech-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
}

.tech-main {
  background: #fafafa;
  padding: 0;
}
</style>