<script setup lang="ts">
import { ref } from 'vue'
import { ElCard, ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElMessage, ElTag } from 'element-plus'

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

const roleLabels: Record<string, string> = { farmer: '农户', tech: '农技人员', coop: '合作社管理人员', admin: '管理员' }
const roleColors: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = { farmer: 'success', tech: 'primary', coop: 'warning', admin: 'danger' }
const statusColors: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = { active: 'success', inactive: 'danger' }

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
  <div class="admin-users-container">
    <ElCard class="main-card" header="用户管理">
      <div class="card-header">
        <ElButton type="primary" @click="handleAdd">添加用户</ElButton>
      </div>
      <ElTable :data="users" border style="width: 100%">
        <ElTableColumn prop="username" label="用户名" min-width="120" />
        <ElTableColumn prop="name" label="姓名" min-width="100" />
        <ElTableColumn prop="role" label="角色" min-width="120">
          <template #default="{ row }">
            <ElTag :type="roleColors[row.role]">{{ roleLabels[row.role] }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="phone" label="手机号" min-width="130" />
        <ElTableColumn prop="email" label="邮箱" min-width="150" />
        <ElTableColumn prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <ElTag :type="statusColors[row.status]">{{ row.status === 'active' ? '正常' : '禁用' }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="createdAt" label="创建时间" min-width="120" />
        <ElTableColumn label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <ElButton type="primary" link @click="handleEdit(row as User)">编辑</ElButton>
            <ElButton :type="(row as User).status === 'active' ? 'warning' : 'success'" link @click="handleToggleStatus(row as User)">
              {{ (row as User).status === 'active' ? '禁用' : '启用' }}
            </ElButton>
            <ElButton type="danger" link @click="handleDelete((row as User).id)">删除</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
    </ElCard>

    <ElDialog :title="editMode ? '编辑用户' : '添加用户'" v-model="dialogVisible" width="600px">
      <ElForm :model="formData" label-width="100px">
        <ElFormItem label="用户名" required>
          <ElInput v-model="formData.username" placeholder="请输入用户名" :disabled="editMode" />
        </ElFormItem>
        <ElFormItem label="姓名" required>
          <ElInput v-model="formData.name" placeholder="请输入姓名" />
        </ElFormItem>
        <ElFormItem label="角色" required>
          <ElSelect v-model="formData.role" placeholder="请选择角色">
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
          <ElSelect v-model="formData.status">
            <ElOption label="正常" value="active" />
            <ElOption label="禁用" value="inactive" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.admin-users-container {
  padding: 20px;
  background: #fafafa;
  min-height: calc(100vh - 60px);
}

.main-card {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>