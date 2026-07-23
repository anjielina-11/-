<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem,
  ElInput, ElSelect, ElOption, ElMessage, ElTag
} from 'element-plus'
import { Search, Refresh, Plus, Edit, Open, TurnOff, Delete } from '@element-plus/icons-vue'

interface User {
  id: string
  username: string
  name: string
  role: 'farmer' | 'tech' | 'coop' | 'admin'
  phone?: string
  email?: string
  status: 'active' | 'inactive'
  createdAt: string
}

const mockUsers: User[] = [
  { id: '1', username: 'farmer', name: '张农户', role: 'farmer', phone: '13800138001', email: 'farmer@example.com', status: 'active', createdAt: '2026-01-15' },
  { id: '2', username: 'tech', name: '李农技员', role: 'tech', phone: '13800138002', email: 'tech@example.com', status: 'active', createdAt: '2026-02-20' },
  { id: '3', username: 'coop', name: '王合作社长', role: 'coop', phone: '13800138003', email: 'coop@example.com', status: 'active', createdAt: '2026-03-10' },
  { id: '4', username: 'admin', name: '刘管理员', role: 'admin', phone: '13800138004', email: 'admin@example.com', status: 'active', createdAt: '2026-01-01' },
  { id: '5', username: 'farmer2', name: '陈农户', role: 'farmer', phone: '13800138005', email: 'farmer2@example.com', status: 'inactive', createdAt: '2026-04-05' },
  { id: '6', username: 'tech2', name: '赵农技员', role: 'tech', phone: '13800138006', email: 'tech2@example.com', status: 'active', createdAt: '2026-05-18' },
  { id: '7', username: 'farmer3', name: '周农户', role: 'farmer', phone: '13800138007', email: 'farmer3@example.com', status: 'active', createdAt: '2026-06-22' },
  { id: '8', username: 'coop2', name: '孙合作社长', role: 'coop', phone: '13800138008', email: 'coop2@example.com', status: 'inactive', createdAt: '2026-07-01' }
]

const users = ref<User[]>([...mockUsers])
const dialogVisible = ref(false)
const editMode = ref(false)
const formData = ref<User>({
  id: '',
  username: '',
  name: '',
  role: 'farmer',
  phone: '',
  email: '',
  status: 'active',
  createdAt: ''
})

// 搜索和筛选
const searchKeyword = ref('')
const filterRole = ref('')
const filterStatus = ref('')

const filteredUsers = computed(() => {
  return users.value.filter(user => {
    const matchKeyword = !searchKeyword.value ||
      user.username.includes(searchKeyword.value) ||
      user.name.includes(searchKeyword.value) ||
      (user.phone && user.phone.includes(searchKeyword.value))
    const matchRole = !filterRole.value || user.role === filterRole.value
    const matchStatus = !filterStatus.value || user.status === filterStatus.value
    return matchKeyword && matchRole && matchStatus
  })
})

const handleSearch = () => {
  // 前端过滤由 computed 自动处理
}

const handleReset = () => {
  searchKeyword.value = ''
  filterRole.value = ''
  filterStatus.value = ''
}

const roleLabels: Record<string, string> = { farmer: '农户', tech: '农技人员', coop: '合作社管理人员', admin: '管理员' }
const roleColors: Record<string, string> = { farmer: 'success', tech: '', coop: 'warning', admin: 'danger' }
const statusLabels: Record<string, string> = { active: '正常', inactive: '禁用' }
const statusColors: Record<string, string> = { active: 'success', inactive: 'danger' }

const handleAdd = () => {
  editMode.value = false
  formData.value = {
    id: '',
    username: '',
    name: '',
    role: 'farmer',
    phone: '',
    email: '',
    status: 'active',
    createdAt: ''
  }
  dialogVisible.value = true
}

const handleEdit = (row: User) => {
  editMode.value = true
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = (id: string) => {
  users.value = users.value.filter(u => u.id !== id)
  ElMessage.success('删除成功')
}

const handleToggleStatus = (row: User) => {
  row.status = row.status === 'active' ? 'inactive' : 'active'
  ElMessage.success(row.status === 'active' ? '已启用' : '已禁用')
}

const handleSubmit = () => {
  if (editMode.value) {
    const index = users.value.findIndex(u => u.id === formData.value.id)
    if (index !== -1) {
      users.value[index] = { ...formData.value }
      ElMessage.success('修改成功')
    }
  } else {
    formData.value.id = String(Date.now())
    formData.value.createdAt = new Date().toISOString().split('T')[0]
    users.value.unshift({ ...formData.value })
    ElMessage.success('添加成功')
  }
  dialogVisible.value = false
}
</script>

<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">管理系统用户账号和权限</p>
      </div>
    </div>

    <!-- 搜索筛选栏 -->
    <div class="filter-card">
      <div class="filter-row">
        <div class="filter-items">
          <ElInput
            v-model="searchKeyword"
            placeholder="搜索用户名/姓名/手机号"
            :prefix-icon="Search"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
          <ElSelect v-model="filterRole" placeholder="角色筛选" clearable style="width: 160px">
            <ElOption label="农户" value="farmer" />
            <ElOption label="农技人员" value="tech" />
            <ElOption label="合作社管理人员" value="coop" />
            <ElOption label="管理员" value="admin" />
          </ElSelect>
          <ElSelect v-model="filterStatus" placeholder="状态筛选" clearable style="width: 140px">
            <ElOption label="正常" value="active" />
            <ElOption label="禁用" value="inactive" />
          </ElSelect>
          <ElButton type="primary" :icon="Search" @click="handleSearch">搜索</ElButton>
          <ElButton :icon="Refresh" @click="handleReset">重置</ElButton>
        </div>
        <ElButton type="primary" :icon="Plus" @click="handleAdd">添加用户</ElButton>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <ElTable :data="filteredUsers" style="width: 100%" :header-cell-style="{ background: 'var(--color-bg-page)', color: 'var(--color-text-secondary)', fontWeight: '600' }">
        <ElTableColumn prop="username" label="用户名" min-width="120">
          <template #default="{ row }">
            <span class="cell-text-primary">{{ row.username }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="name" label="姓名" min-width="100">
          <template #default="{ row }">
            <span class="cell-text-primary">{{ row.name }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="role" label="角色" min-width="130">
          <template #default="{ row }">
            <ElTag :type="(roleColors[row.role] as any)" size="small" effect="light">{{ roleLabels[row.role] }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="phone" label="手机号" min-width="130">
          <template #default="{ row }">
            <span class="cell-text-secondary">{{ row.phone || '-' }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">
            <span class="cell-text-secondary">{{ row.email || '-' }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="status" label="状态" min-width="90" align="center">
          <template #default="{ row }">
            <ElTag :type="(statusColors[row.status] as any)" size="small" effect="light">{{ statusLabels[row.status] }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="createdAt" label="创建时间" min-width="120">
          <template #default="{ row }">
            <span class="cell-text-secondary">{{ row.createdAt }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" min-width="160" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <ElButton type="primary" link :icon="Edit" @click="handleEdit(row as User)" />
              <ElButton
                :type="row.status === 'active' ? 'warning' : 'success'"
                link
                :icon="row.status === 'active' ? TurnOff : Open"
                @click="handleToggleStatus(row as User)"
              />
              <ElButton type="danger" link :icon="Delete" @click="handleDelete((row as User).id)" />
            </div>
          </template>
        </ElTableColumn>
      </ElTable>
    </div>

    <!-- 添加/编辑对话框 -->
    <ElDialog
      :title="editMode ? '编辑用户' : '添加用户'"
      v-model="dialogVisible"
      width="560px"
      :close-on-click-modal="false"
      class="custom-dialog"
    >
      <ElForm :model="formData" label-width="90px" label-position="right" class="dialog-form">
        <ElFormItem label="用户名" required>
          <ElInput v-model="formData.username" placeholder="请输入用户名" :disabled="editMode" />
        </ElFormItem>
        <ElFormItem label="姓名" required>
          <ElInput v-model="formData.name" placeholder="请输入姓名" />
        </ElFormItem>
        <ElFormItem label="角色" required>
          <ElSelect v-model="formData.role" placeholder="请选择角色" style="width: 100%">
            <ElOption label="农户" value="farmer" />
            <ElOption label="农技人员" value="tech" />
            <ElOption label="合作社管理人员" value="coop" />
            <ElOption label="管理员" value="admin" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="手机号">
          <ElInput v-model="formData.phone" placeholder="请输入手机号" />
        </ElFormItem>
        <ElFormItem label="邮箱">
          <ElInput v-model="formData.email" placeholder="请输入邮箱" />
        </ElFormItem>
        <ElFormItem v-if="editMode" label="状态">
          <ElSelect v-model="formData.status" style="width: 100%">
            <ElOption label="正常" value="active" />
            <ElOption label="禁用" value="inactive" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleSubmit">确定</ElButton>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: var(--spacing-lg);
  min-height: calc(100vh - var(--header-height));
  background: var(--color-bg-page);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.page-title {
  font-size: var(--font-size-2xl);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.page-subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: var(--spacing-xs);
}

/* 搜索筛选栏 */
.filter-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  margin-bottom: var(--spacing-md);
  transition: box-shadow var(--transition-normal);
}

.filter-card:hover {
  box-shadow: var(--shadow-md);
}

.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-md);
}

.filter-items {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

/* 表格卡片 */
.table-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--spacing-md);
  transition: box-shadow var(--transition-normal);
}

.table-card:hover {
  box-shadow: var(--shadow-md);
}

/* 表格自定义样式 */
.table-card :deep(.el-table) {
  --el-table-border-color: var(--color-border-light);
  --el-table-header-bg-color: var(--color-bg-page);
  --el-table-row-hover-bg-color: var(--color-bg-hover);
}

.table-card :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.table-card :deep(.el-table__row) {
  transition: background-color var(--transition-fast);
}

.table-card :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid var(--color-border-light);
}

.table-card :deep(.el-table th.el-table__cell) {
  border-bottom: 1px solid var(--color-border);
}

/* 单元格文字 */
.cell-text-primary {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  font-weight: 500;
}

.cell-text-secondary {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
}

/* 操作按钮组 */
.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
}

/* 对话框 */
.dialog-form {
  padding: var(--spacing-sm) var(--spacing-md) 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
}
</style>
