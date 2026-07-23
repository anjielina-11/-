<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElCard, ElForm, ElFormItem, ElInput, ElButton, ElMessage, ElDivider, ElTag } from 'element-plus'
import { useUserStore, type Role } from '@/stores/user'
import router from '@/router'

const userStore = useUserStore()

const isEditing = ref(false)
const isSubmitting = ref(false)

const roleLabels: Record<Role, string> = {
  farmer: '农户',
  tech: '农技人员',
  coop: '合作社管理人员',
  admin: '系统管理员'
}

const roleColors: Record<Role, string> = {
  farmer: '#52C41A',
  tech: '#1890FF',
  coop: '#FAAD14',
  admin: '#F5222D'
}

interface Permission {
  title: string
  desc: string
  iconClass: string
}

const rolePermissions: Record<Role, Permission[]> = {
  farmer: [
    { title: '地块管理', desc: '管理个人地块信息', iconClass: 'farmer-icon' },
    { title: '种植档案', desc: '查看和管理种植记录', iconClass: 'crop-icon' },
    { title: '病害上报', desc: '上传图片进行病害识别', iconClass: 'disease-icon' },
    { title: '任务管理', desc: '查看和完成农事任务', iconClass: 'task-icon' }
  ],
  tech: [
    { title: '待审核列表', desc: '审核农户上报的病害识别结果', iconClass: 'review-icon' },
    { title: '识别结果查询', desc: '查询历史识别记录和防治建议', iconClass: 'result-icon' }
  ],
  coop: [
    { title: '生产趋势看板', desc: '查看合作社生产数据和趋势', iconClass: 'dashboard-icon' },
    { title: '市场价格监控', desc: '监控农产品市场价格动态', iconClass: 'market-icon' }
  ],
  admin: [
    { title: '用户管理', desc: '管理系统所有用户账户', iconClass: 'user-icon' },
    { title: '知识库管理', desc: '维护病害知识库和防治方案', iconClass: 'knowledge-icon' },
    { title: '模型版本管理', desc: '管理AI识别模型版本', iconClass: 'model-icon' }
  ]
}

const currentPermissions = computed(() => {
  return rolePermissions[userStore.user?.role || 'farmer']
})

const form = reactive({
  name: userStore.user?.name || '',
  phone: '',
  email: '',
  address: '',
  avatar: ''
})

const originalForm = reactive({ ...form })

const avatarText = computed(() => {
  return userStore.user?.name?.charAt(0) || '?'
})

const roleColor = computed(() => {
  return roleColors[userStore.user?.role || 'farmer']
})

const handleEdit = () => {
  isEditing.value = true
}

const handleCancel = () => {
  isEditing.value = false
  Object.assign(form, originalForm)
}

const handleSave = async () => {
  isSubmitting.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    Object.assign(originalForm, form)
    isEditing.value = false
    ElMessage.success('个人信息更新成功')
  } catch {
    ElMessage.error('更新失败')
  } finally {
    isSubmitting.value = false
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const handleChangePassword = () => {
  ElMessage.info('修改密码功能开发中')
}
</script>

<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="header-content">
        <div class="profile-avatar" :style="{ borderColor: roleColor }">
          {{ avatarText }}
        </div>
        <div class="profile-info">
          <h1 class="profile-name">{{ form.name }}</h1>
          <ElTag :style="{ backgroundColor: roleColor + '15', color: roleColor, borderColor: roleColor }">
            {{ roleLabels[userStore.user?.role || 'farmer'] }}
          </ElTag>
          <p class="profile-username">用户名: {{ userStore.user?.username }}</p>
        </div>
        <div class="header-actions">
          <ElButton v-if="!isEditing" type="primary" @click="handleEdit">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:14px;height:14px;margin-right:6px">
              <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
            编辑资料
          </ElButton>
          <div v-else class="edit-actions">
            <ElButton type="primary" :loading="isSubmitting" @click="handleSave">保存</ElButton>
            <ElButton @click="handleCancel">取消</ElButton>
          </div>
        </div>
      </div>
    </div>

    <div class="profile-body">
      <div class="body-left">
        <ElCard class="info-card" header="基本信息">
          <ElForm :model="form" label-position="top">
            <div class="form-row">
              <ElFormItem label="姓名">
                <ElInput v-model="form.name" :disabled="!isEditing" />
              </ElFormItem>
              <ElFormItem label="手机号">
                <ElInput v-model="form.phone" :disabled="!isEditing" placeholder="请输入手机号" />
              </ElFormItem>
            </div>
            <div class="form-row">
              <ElFormItem label="邮箱">
                <ElInput v-model="form.email" :disabled="!isEditing" placeholder="请输入邮箱" />
              </ElFormItem>
              <ElFormItem label="地址">
                <ElInput v-model="form.address" :disabled="!isEditing" placeholder="请输入地址" />
              </ElFormItem>
            </div>
          </ElForm>
        </ElCard>

        <ElCard class="info-card" header="账户安全">
          <div class="security-item">
            <div class="security-info">
              <div class="security-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0110 0v4"/>
                </svg>
              </div>
              <div>
                <div class="security-title">修改密码</div>
                <div class="security-desc">定期更换密码，确保账户安全</div>
              </div>
            </div>
            <ElButton type="text" @click="handleChangePassword">修改</ElButton>
          </div>

          <ElDivider />

          <div class="security-item">
            <div class="security-info">
              <div class="security-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
              </div>
              <div>
                <div class="security-title">退出登录</div>
                <div class="security-desc">安全退出当前账户</div>
              </div>
            </div>
            <ElButton type="text" @click="handleLogout" style="color: #F5222D">退出</ElButton>
          </div>
        </ElCard>
      </div>

      <div class="body-right">
        <ElCard class="info-card" header="角色权限">
          <div class="permission-list">
            <div class="permission-item" v-for="(perm, index) in currentPermissions" :key="index">
              <div class="permission-icon" :class="perm.iconClass">
                <svg v-if="perm.iconClass === 'farmer-icon'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
                </svg>
                <svg v-else-if="perm.iconClass === 'crop-icon'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M7 21h10"/>
                  <path d="M12 21v-7"/>
                  <path d="M8 14l4-4 4 4"/>
                  <circle cx="12" cy="18" r="2"/>
                </svg>
                <svg v-else-if="perm.iconClass === 'disease-icon'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="3"/>
                  <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06a1.65 1.65 0 001.82.33H9a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
                </svg>
                <svg v-else-if="perm.iconClass === 'task-icon'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <path d="M9 9h6"/>
                  <path d="M9 15h6"/>
                  <path d="M9 12h6"/>
                </svg>
                <svg v-else-if="perm.iconClass === 'review-icon'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                  <path d="m9 12 2 2 4-4"/>
                </svg>
                <svg v-else-if="perm.iconClass === 'result-icon'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                  <polyline points="10 9 9 9 8 9"/>
                </svg>
                <svg v-else-if="perm.iconClass === 'dashboard-icon'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="7" height="7"/>
                  <rect x="14" y="3" width="7" height="7"/>
                  <rect x="14" y="14" width="7" height="7"/>
                  <rect x="3" y="14" width="7" height="7"/>
                </svg>
                <svg v-else-if="perm.iconClass === 'market-icon'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3 3v18h18"/>
                  <path d="M18 9l-5 5-4-4-3 3"/>
                </svg>
                <svg v-else-if="perm.iconClass === 'user-icon'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 00-3-3.87"/>
                  <path d="M16 3.13a4 4 0 010 7.75"/>
                </svg>
                <svg v-else-if="perm.iconClass === 'knowledge-icon'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/>
                  <path d="M12 17h.01"/>
                </svg>
                <svg v-else-if="perm.iconClass === 'model-icon'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                  <polyline points="10 9 9 9 8 9"/>
                  <line x1="9" y1="12" x2="9" y2="9"/>
                  <line x1="15" y1="12" x2="15" y2="9"/>
                </svg>
              </div>
              <div class="permission-info">
                <div class="permission-title">{{ perm.title }}</div>
                <div class="permission-desc">{{ perm.desc }}</div>
              </div>
            </div>
          </div>
        </ElCard>

        <ElCard class="info-card" header="登录信息">
          <div class="login-info">
            <div class="login-item">
              <span class="login-label">用户ID</span>
              <span class="login-value">{{ userStore.user?.id }}</span>
            </div>
            <div class="login-item">
              <span class="login-label">创建时间</span>
              <span class="login-value">2026-07-01</span>
            </div>
            <div class="login-item">
              <span class="login-label">最后登录</span>
              <span class="login-value">2026-07-23 10:30</span>
            </div>
          </div>
        </ElCard>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  padding: 24px;
  background: var(--color-bg-page, #F0F5F1);
  min-height: calc(100vh - 60px);
}

.profile-header {
  background: linear-gradient(135deg, #2D7D46 0%, #1B5E32 100%);
  border-radius: var(--radius-lg, 16px);
  padding: 32px;
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 24px;
}

.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  border: 3px solid;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FFFFFF;
  font-size: 32px;
  font-weight: 700;
}

.profile-info {
  flex: 1;
}

.profile-name {
  font-size: 24px;
  font-weight: 700;
  color: #FFFFFF;
  margin: 0 0 12px;
}

.profile-username {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin: 8px 0 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.edit-actions {
  display: flex;
  gap: 12px;
}

.profile-body {
  display: flex;
  gap: 24px;
}

.body-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.body-right {
  width: 360px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-card {
  border-radius: var(--radius-md, 12px);
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(45, 125, 70, 0.06));
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-row :deep(.el-form-item) {
  flex: 1;
}

.security-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}

.security-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.security-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--color-bg-hover, #F9FAFB);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary, #6B7280);
}

.security-icon svg {
  width: 18px;
  height: 18px;
}

.security-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary, #1F2937);
}

.security-desc {
  font-size: 12px;
  color: var(--color-text-placeholder, #9CA3AF);
}

.permission-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.permission-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-bg-page, #F0F5F1);
  border-radius: 8px;
}

.permission-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.farmer-icon {
  background: rgba(82, 196, 26, 0.1);
  color: #52C41A;
}

.crop-icon {
  background: rgba(24, 144, 255, 0.1);
  color: #1890FF;
}

.disease-icon {
  background: rgba(250, 173, 20, 0.1);
  color: #FAAD14;
}

.task-icon {
  background: rgba(245, 34, 45, 0.1);
  color: #F5222D;
}

.review-icon {
  background: rgba(24, 144, 255, 0.1);
  color: #1890FF;
}

.result-icon {
  background: rgba(82, 196, 26, 0.1);
  color: #52C41A;
}

.dashboard-icon {
  background: rgba(250, 173, 20, 0.1);
  color: #FAAD14;
}

.market-icon {
  background: rgba(15, 185, 129, 0.1);
  color: #0Fb981;
}

.user-icon {
  background: rgba(245, 34, 45, 0.1);
  color: #F5222D;
}

.knowledge-icon {
  background: rgba(168, 85, 247, 0.1);
  color: #A855F7;
}

.model-icon {
  background: rgba(236, 72, 153, 0.1);
  color: #EC4899;
}

.permission-icon svg {
  width: 20px;
  height: 20px;
}

.permission-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary, #1F2937);
}

.permission-desc {
  font-size: 12px;
  color: var(--color-text-placeholder, #9CA3AF);
}

.login-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.login-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.login-label {
  font-size: 13px;
  color: var(--color-text-secondary, #6B7280);
}

.login-value {
  font-size: 13px;
  color: var(--color-text-primary, #1F2937);
  font-family: monospace;
}

@media (max-width: 768px) {
  .profile-body {
    flex-direction: column;
  }
  
  .body-right {
    width: 100%;
  }
  
  .header-content {
    flex-direction: column;
    text-align: center;
  }
  
  .header-actions {
    margin-top: 16px;
  }
}
</style>