<script setup lang="ts">
import { computed } from 'vue'
import { ElMenu, ElMenuItem, ElIcon } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  MapLocation,
  Grape,
  Camera,
  List,
  Clock,
  Search,
  PieChart,
  ArrowUp,
  User,
  Document,
  Cpu
} from '@element-plus/icons-vue'
import { useRouter, useRoute } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const iconMap: Record<string, any> = {
  MapLocation,
  Leaf: Grape,
  Camera,
  List,
  Clock,
  Search,
  PieChart,
  TrendingUp: ArrowUp,
  User,
  BookOpen: Document,
  Cpu
}

const currentPath = computed(() => route.path)

const handleMenuSelect = (path: string) => {
  router.push(path)
}
</script>

<template>
  <ElMenu
    mode="vertical"
    class="sidebar-menu"
    :default-active="currentPath"
    @select="handleMenuSelect"
  >
    <ElMenuItem v-for="menu in userStore.menus" :key="menu.id" :index="menu.path">
      <ElIcon :component="iconMap[menu.icon]" />
      <span>{{ menu.name }}</span>
    </ElMenuItem>
  </ElMenu>
</template>

<style scoped>
.sidebar-menu {
  width: 200px;
  background: #f5f5f5;
  border-right: none;
}
</style>