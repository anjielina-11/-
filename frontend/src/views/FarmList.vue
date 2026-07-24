<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElTableColumn,
  ElButton,
  ElInput,
  ElDialog,
  ElForm,
  ElFormItem,
  ElMessageBox,
  ElMessage,
  ElPagination,
  ElSelect,
  ElOption,
  ElCard,
  type FormInstance
} from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'

export interface IFarm {
  id: string
  farmId: string
  name: string
  farmName: string
  area: number
  cropType: string
  soilType: string
  createdAt: string
}

interface PageResult<T> {
  list: T[]
  total: number
}

interface Farm {
  id: string
  name: string
}

interface FieldResponse {
  id: string
  name: string
  area?: number
  soilType?: string
  cropType?: string
  createdAt?: string
}

const tableData = ref<IFarm[]>([])
const allFields = ref<IFarm[]>([])
const farms = ref<Farm[]>([])
const loading = ref(false)
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('新增地块')
const editId = ref<string | null>(null)

const form = reactive({
  name: '',
  farmId: '',
  area: '',
  soilType: ''
})

const formRef = ref<FormInstance>()

const rules = {
  name: [
    { required: true, message: '请输入地块名称', trigger: 'blur' }
  ],
  farmId: [
    { required: true, message: '请选择所属农场', trigger: 'change' }
  ],
  area: [
    { required: true, message: '请输入面积', trigger: 'blur' },
    { type: 'number' as const, min: 0, message: '面积必须大于0', trigger: 'blur' }
  ],
  soilType: [
    { required: true, message: '请选择土壤类型', trigger: 'change' }
  ]
}

const total = ref(0)
const pageSize = ref(10)
const currentPage = ref(1)

const soilTypes = ['红壤', '黄壤', '黑土', '褐土', '潮土', '水稻土', '紫色土', '沙土']

const fetchData = async () => {
  loading.value = true
  try {
    const farmPage = await request.get<PageResult<Farm>>('/farms', { params: { size: 100 } })
    farms.value = farmPage.list
    const pages = await Promise.all(farms.value.map(async farm => ({
      farm,
      fields: await request.get<PageResult<FieldResponse>>(`/farms/${farm.id}/fields`)
    })))
    allFields.value = pages.flatMap(({ farm, fields }) => fields.list.map(field => ({
      id: field.id,
      farmId: farm.id,
      name: field.name,
      farmName: farm.name,
      area: Number(field.area) || 0,
      cropType: field.cropType || '未登记',
      soilType: field.soilType || '未填写',
      createdAt: field.createdAt || ''
    })))
    applyFilter()
  } catch (error) {
    ElMessage.error('获取地块列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  applyFilter()
}

const handleReset = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  applyFilter()
}

const applyFilter = () => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  const filtered = keyword
    ? allFields.value.filter(field => `${field.name} ${field.farmName}`.toLowerCase().includes(keyword))
    : allFields.value
  total.value = filtered.length
  const start = (currentPage.value - 1) * pageSize.value
  tableData.value = filtered.slice(start, start + pageSize.value)
}

const handleAdd = () => {
  dialogTitle.value = '新增地块'
  editId.value = null
  form.name = ''
  form.farmId = farms.value[0]?.id || ''
  form.area = ''
  form.soilType = ''
  dialogVisible.value = true
}

const handleEdit = (row: IFarm) => {
  dialogTitle.value = '编辑地块'
  editId.value = row.id
  form.name = row.name
  form.farmId = row.farmId
  form.area = String(row.area)
  form.soilType = row.soilType
  dialogVisible.value = true
}

const handleDelete = async (row: IFarm) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除地块「${row.name}」吗？`,
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await request.delete(`/farms/${row.farmId}/fields/${row.id}`)
    ElMessage.success('删除成功')
    await fetchData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('删除失败')
  }
}

const handleSubmit = async () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return

    try {
      if (editId.value) {
        await request.put(`/farms/${form.farmId}/fields/${editId.value}`, {
          name: form.name,
          areaMu: parseFloat(form.area),
          soilType: form.soilType
        })
        ElMessage.success('修改成功')
      } else {
        await request.post(`/farms/${form.farmId}/fields`, {
          name: form.name,
          areaMu: parseFloat(form.area),
          soilType: form.soilType
        })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      fetchData()
    } catch (error) {
      ElMessage.error(editId.value ? '修改失败' : '新增失败')
    }
  })
}

const handleClose = () => {
  dialogVisible.value = false
  formRef.value?.resetFields()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  applyFilter()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  applyFilter()
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">地块管理</h1>
        <p class="page-subtitle">管理您的农场和地块信息</p>
      </div>
      <ElButton type="primary" :icon="Plus" @click="handleAdd">新增地块</ElButton>
    </div>

    <!-- 搜索栏 -->
    <ElCard class="search-card" shadow="never">
      <div class="search-bar">
        <ElInput
          v-model="searchKeyword"
          placeholder="按名称或农场模糊查询"
          clearable
          :prefix-icon="Search"
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <ElButton type="primary" :icon="Search" @click="handleSearch">搜索</ElButton>
        <ElButton :icon="Refresh" @click="handleReset">重置</ElButton>
      </div>
    </ElCard>

    <!-- 表格 -->
    <ElCard class="table-card" shadow="never">
      <el-table
        :data="tableData"
        :loading="loading"
        class="custom-table"
      >
        <el-table-column prop="name" label="地块名称" min-width="120">
          <template #default="{ row }">
            <span class="cell-primary">{{ (row as IFarm).name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="farmName" label="所属农场" min-width="150" />
        <el-table-column prop="area" label="面积（亩）" min-width="100" align="right">
          <template #default="{ row }">
            <span class="cell-number">{{ (row as IFarm).area.toFixed(1) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cropType" label="作物类型" min-width="100">
          <template #default="{ row }">
            <span class="crop-tag">{{ (row as IFarm).cropType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="soilType" label="土壤类型" min-width="100" />
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <ElButton type="primary" link :icon="Edit" @click="handleEdit(row as IFarm)" />
              <ElButton type="danger" link :icon="Delete" @click="handleDelete(row as IFarm)" />
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-bar">
        <ElPagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </ElCard>

    <!-- 新增/编辑对话框 -->
    <ElDialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="520px"
      :close-on-click-modal="false"
      @close="handleClose"
      class="custom-dialog"
    >
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="left" class="custom-form">
        <ElFormItem label="地块名称" prop="name">
          <ElInput v-model="form.name" placeholder="请输入地块名称" />
        </ElFormItem>
        <ElFormItem label="所属农场" prop="farmId">
          <ElSelect v-model="form.farmId" placeholder="请选择所属农场" style="width: 100%" :disabled="!!editId">
            <ElOption v-for="farm in farms" :key="farm.id" :label="farm.name" :value="farm.id" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="面积（亩）" prop="area">
          <ElInput v-model="form.area" type="number" placeholder="请输入面积" />
        </ElFormItem>
        <ElFormItem label="土壤类型" prop="soilType">
          <ElSelect v-model="form.soilType" placeholder="请选择土壤类型" style="width: 100%">
            <ElOption v-for="type in soilTypes" :key="type" :label="type" :value="type" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="handleClose">取消</ElButton>
          <ElButton type="primary" @click="handleSubmit">确定</ElButton>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
/* 搜索卡片 */
.search-card {
  margin-bottom: var(--spacing-md);
}

.search-card :deep(.el-card__body) {
  padding: var(--spacing-md) var(--spacing-lg);
}

.search-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.search-input {
  width: 320px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
}

/* 表格卡片 */
.table-card :deep(.el-card__body) {
  padding: 0;
}

.custom-table {
  --el-table-border-color: var(--color-border-light);
  --el-table-header-bg-color: var(--color-bg-page);
  --el-table-row-hover-bg-color: var(--color-bg-hover);
}

.custom-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.custom-table :deep(th.el-table__cell) {
  background-color: var(--color-bg-page) !important;
  color: var(--color-text-secondary) !important;
  font-weight: 600 !important;
  font-size: var(--font-size-sm) !important;
  border-bottom: 1px solid var(--color-border) !important;
}

.custom-table :deep(td.el-table__cell) {
  border-bottom: 1px solid var(--color-border-light) !important;
  color: var(--color-text-primary);
}

.custom-table :deep(.el-table__row:last-child td.el-table__cell) {
  border-bottom: none !important;
}

.cell-primary {
  font-weight: 600;
  color: var(--color-primary);
}

.cell-number {
  font-variant-numeric: tabular-nums;
  color: var(--color-text-regular);
}

.crop-tag {
  display: inline-block;
  padding: 2px 10px;
  background-color: var(--color-primary-lighter);
  color: var(--color-primary);
  border-radius: 20px;
  font-size: var(--font-size-xs);
  font-weight: 500;
}

/* 操作按钮 */
.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
}

.action-btns :deep(.el-button) {
  padding: 4px;
  font-size: 16px;
}

/* 分页 */
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--color-border-light);
}

/* 对话框 */
.custom-dialog :deep(.el-dialog) {
  border-radius: var(--radius-lg) !important;
}

.custom-dialog :deep(.el-dialog__header) {
  padding: var(--spacing-lg) var(--spacing-lg) var(--spacing-md);
  border-bottom: 1px solid var(--color-border-light);
  margin-right: 0;
}

.custom-dialog :deep(.el-dialog__title) {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text-primary);
}

.custom-dialog :deep(.el-dialog__body) {
  padding: var(--spacing-lg);
}

.custom-dialog :deep(.el-dialog__footer) {
  padding: var(--spacing-md) var(--spacing-lg) var(--spacing-lg);
  border-top: 1px solid var(--color-border-light);
}

.custom-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.custom-form :deep(.el-select) {
  width: 100%;
}

.custom-form :deep(.el-select .el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
}
</style>
