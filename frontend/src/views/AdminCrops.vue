<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ElButton,
  ElCard,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElSelect,
  ElOption,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import { Edit, Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface CropItem {
  id: string
  name: string
  category?: string
  variety?: string
  growthDays?: number
  description?: string
  status: 'active' | 'inactive'
}

interface PageResult<T> {
  list: T[]
  total: number
}

const crops = ref<CropItem[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editMode = ref(false)
const keyword = ref('')
const formData = ref<CropItem>({
  id: '',
  name: '',
  category: '',
  variety: '',
  growthDays: undefined,
  description: '',
  status: 'active'
})

const categoryOptions = ['粮食作物', '经济作物', '蔬菜', '水果', '其他']
const filteredCrops = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return crops.value
  return crops.value.filter(crop =>
    `${crop.name} ${crop.category || ''} ${crop.variety || ''}`.toLowerCase().includes(query)
  )
})

const loadCrops = async () => {
  loading.value = true
  try {
    const page = await request.get<PageResult<CropItem>>('/crops', {
      params: { size: 100, includeInactive: true }
    })
    crops.value = page.list
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取作物品种失败')
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editMode.value = false
  formData.value = {
    id: '',
    name: '',
    category: '',
    variety: '',
    growthDays: undefined,
    description: '',
    status: 'active'
  }
  dialogVisible.value = true
}

const openEdit = (row: CropItem) => {
  editMode.value = true
  formData.value = { ...row }
  dialogVisible.value = true
}

const submitCrop = async () => {
  if (submitting.value) return
  const name = formData.value.name.trim()
  if (!name) {
    ElMessage.warning('请输入作物名称')
    return
  }

  submitting.value = true
  try {
    const payload = {
      name,
      category: formData.value.category?.trim() || undefined,
      variety: formData.value.variety?.trim() || undefined,
      growthDays: formData.value.growthDays || undefined,
      description: formData.value.description?.trim() || undefined
    }
    if (editMode.value) {
      await request.put(`/crops/${formData.value.id}`, payload, { silent: true })
      ElMessage.success('作物品种已更新')
    } else {
      await request.post('/crops', payload, { silent: true })
      ElMessage.success('作物品种已新增')
    }
    dialogVisible.value = false
    await loadCrops()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : editMode.value ? '更新作物品种失败' : '新增作物品种失败')
  } finally {
    submitting.value = false
  }
}

const toggleStatus = async (row: CropItem) => {
  const disabling = row.status === 'active'
  try {
    await ElMessageBox.confirm(
      disabling
        ? `停用作物「${row.name}」后，新建种植记录时将不能选择它，历史记录不会删除。`
        : `确定要启用作物「${row.name}」吗？`,
      disabling ? '确认停用' : '确认启用',
      {
        confirmButtonText: disabling ? '停用' : '启用',
        cancelButtonText: '取消',
        type: disabling ? 'warning' : 'info'
      }
    )
    await request.put(`/crops/${row.id}/status`, {
      status: disabling ? 'inactive' : 'active'
    }, { silent: true })
    ElMessage.success(disabling ? '作物已停用' : '作物已启用')
    await loadCrops()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error instanceof Error ? error.message : '作物状态修改失败')
  }
}

onMounted(loadCrops)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">作物品种管理</h1>
        <p class="page-subtitle">维护可选作物，停用后仅禁止新建引用，不删除历史种植数据</p>
      </div>
      <ElButton type="primary" :icon="Plus" @click="openCreate">新增作物品种</ElButton>
    </div>

    <ElCard class="filter-card" shadow="never">
      <ElInput v-model="keyword" clearable placeholder="搜索作物名称、类别或品种" />
    </ElCard>

    <ElCard class="table-card" shadow="never">
      <ElTable :data="filteredCrops" :loading="loading" class="custom-table">
        <ElTableColumn prop="name" label="作物名称" min-width="130">
          <template #default="{ row }"><span class="cell-primary">{{ row.name }}</span></template>
        </ElTableColumn>
        <ElTableColumn prop="category" label="类别" min-width="120">
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </ElTableColumn>
        <ElTableColumn prop="variety" label="品种" min-width="150">
          <template #default="{ row }">{{ row.variety || '-' }}</template>
        </ElTableColumn>
        <ElTableColumn prop="growthDays" label="生长周期（天）" min-width="130" align="right">
          <template #default="{ row }">{{ row.growthDays ?? '-' }}</template>
        </ElTableColumn>
        <ElTableColumn prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <ElTag :type="row.status === 'active' ? 'success' : 'info'">
              {{ row.status === 'active' ? '启用' : '停用' }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <ElButton type="primary" link :icon="Edit" @click="openEdit(row as CropItem)">编辑</ElButton>
            <ElButton
              :type="row.status === 'active' ? 'warning' : 'success'"
              link
              @click="toggleStatus(row as CropItem)"
            >
              {{ row.status === 'active' ? '停用' : '启用' }}
            </ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
    </ElCard>

    <ElDialog
      v-model="dialogVisible"
      :title="editMode ? '编辑作物品种' : '新增作物品种'"
      width="520px"
      :close-on-click-modal="false"
      class="custom-dialog"
    >
      <ElForm :model="formData" label-width="110px" label-position="left">
        <ElFormItem label="作物名称" required>
          <ElInput v-model="formData.name" maxlength="100" />
        </ElFormItem>
        <ElFormItem label="类别">
          <ElSelect v-model="formData.category" clearable style="width: 100%">
            <ElOption v-for="category in categoryOptions" :key="category" :label="category" :value="category" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="品种">
          <ElInput v-model="formData.variety" maxlength="100" />
        </ElFormItem>
        <ElFormItem label="生长周期">
          <ElInputNumber v-model="formData.growthDays" :min="1" :max="1000" controls-position="right" />
          <span class="unit-text">天</span>
        </ElFormItem>
        <ElFormItem label="说明">
          <ElInput v-model="formData.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton :disabled="submitting" @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="submitting" @click="submitCrop">保存</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.filter-card {
  margin-bottom: var(--spacing-md);
}

.filter-card :deep(.el-card__body) {
  max-width: 420px;
  padding: var(--spacing-md) var(--spacing-lg);
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

.cell-primary {
  color: var(--color-primary);
  font-weight: 600;
}

.unit-text {
  margin-left: var(--spacing-sm);
  color: var(--color-text-secondary);
}
</style>
