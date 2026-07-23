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

export interface IFarm {
  id: number
  name: string
  farmName: string
  area: number
  cropType: string
  soilType: string
  createdAt: string
}

const tableData = ref<IFarm[]>([])
const loading = ref(false)
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('新增地块')
const editId = ref<number | null>(null)

const form = reactive({
  name: '',
  farmName: '',
  area: '',
  cropType: '',
  soilType: ''
})

const formRef = ref<FormInstance>()

const rules = {
  name: [
    { required: true, message: '请输入地块名称', trigger: 'blur' }
  ],
  farmName: [
    { required: true, message: '请输入所属农场', trigger: 'blur' }
  ],
  area: [
    { required: true, message: '请输入面积', trigger: 'blur' },
    { type: 'number' as const, min: 0, message: '面积必须大于0', trigger: 'blur' }
  ],
  cropType: [
    { required: true, message: '请选择作物类型', trigger: 'change' }
  ],
  soilType: [
    { required: true, message: '请选择土壤类型', trigger: 'change' }
  ]
}

const total = ref(0)
const pageSize = ref(10)
const currentPage = ref(1)

const cropTypes = ['水稻', '玉米', '小麦', '蔬菜', '水果', '花卉', '茶叶', '中药材']
const soilTypes = ['红壤', '黄壤', '黑土', '褐土', '潮土', '水稻土', '紫色土', '沙土']

const mockData: IFarm[] = [
  { id: 1, name: '一号地块', farmName: '云南农业示范园', area: 50.5, cropType: '水稻', soilType: '水稻土', createdAt: '2024-01-15 10:30:00' },
  { id: 2, name: '二号地块', farmName: '云南农业示范园', area: 35.2, cropType: '玉米', soilType: '红壤', createdAt: '2024-01-16 14:20:00' },
  { id: 3, name: '三号地块', farmName: '昆明蔬菜基地', area: 28.8, cropType: '蔬菜', soilType: '潮土', createdAt: '2024-01-18 09:15:00' },
  { id: 4, name: '四号地块', farmName: '大理水果农场', area: 62.0, cropType: '水果', soilType: '黄壤', createdAt: '2024-01-20 16:45:00' },
  { id: 5, name: '五号地块', farmName: '普洱茶叶基地', area: 88.5, cropType: '茶叶', soilType: '红壤', createdAt: '2024-01-22 11:00:00' },
  { id: 6, name: '六号地块', farmName: '丽江中药材园', area: 45.3, cropType: '中药材', soilType: '黑土', createdAt: '2024-01-25 13:30:00' },
  { id: 7, name: '七号地块', farmName: '西双版纳花卉园', area: 32.0, cropType: '花卉', soilType: '紫色土', createdAt: '2024-01-28 10:00:00' },
  { id: 8, name: '八号地块', farmName: '玉溪小麦基地', area: 75.6, cropType: '小麦', soilType: '褐土', createdAt: '2024-02-01 09:20:00' },
  { id: 9, name: '九号地块', farmName: '红河玉米基地', area: 42.1, cropType: '玉米', soilType: '潮土', createdAt: '2024-02-05 15:10:00' },
  { id: 10, name: '十号地块', farmName: '文山蔬菜基地', area: 38.4, cropType: '蔬菜', soilType: '水稻土', createdAt: '2024-02-10 11:45:00' },
  { id: 11, name: '十一号地块', farmName: '楚雄水果基地', area: 55.0, cropType: '水果', soilType: '黄壤', createdAt: '2024-02-15 14:30:00' },
  { id: 12, name: '十二号地块', farmName: '曲靖茶叶基地', area: 68.2, cropType: '茶叶', soilType: '红壤', createdAt: '2024-02-20 10:15:00' }
]

const fetchData = () => {
  loading.value = true
  setTimeout(() => {
    let filteredData = [...mockData]
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      filteredData = filteredData.filter(item =>
        item.name.toLowerCase().includes(keyword) ||
        item.farmName.toLowerCase().includes(keyword)
      )
    }
    total.value = filteredData.length
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    tableData.value = filteredData.slice(start, end)
    loading.value = false
  }, 500)
}

const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

const handleReset = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  fetchData()
}

const handleAdd = () => {
  dialogTitle.value = '新增地块'
  editId.value = null
  form.name = ''
  form.farmName = ''
  form.area = ''
  form.cropType = ''
  form.soilType = ''
  dialogVisible.value = true
}

const handleEdit = (row: IFarm) => {
  dialogTitle.value = '编辑地块'
  editId.value = row.id
  form.name = row.name
  form.farmName = row.farmName
  form.area = String(row.area)
  form.cropType = row.cropType
  form.soilType = row.soilType
  dialogVisible.value = true
}

const handleDelete = (row: IFarm) => {
  ElMessageBox.confirm(
    `确定要删除地块「${row.name}」吗？`,
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

const handleSubmit = () => {
  formRef.value?.validate((valid) => {
    if (!valid) return

    ElMessage.success(editId.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    fetchData()
  })
}

const handleClose = () => {
  dialogVisible.value = false
  formRef.value?.resetFields()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  fetchData()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  fetchData()
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
        <ElFormItem label="所属农场" prop="farmName">
          <ElInput v-model="form.farmName" placeholder="请输入所属农场" />
        </ElFormItem>
        <ElFormItem label="面积（亩）" prop="area">
          <ElInput v-model="form.area" type="number" placeholder="请输入面积" />
        </ElFormItem>
        <ElFormItem label="作物类型" prop="cropType">
          <ElSelect v-model="form.cropType" placeholder="请选择作物类型" style="width: 100%">
            <ElOption v-for="type in cropTypes" :key="type" :label="type" :value="type" />
          </ElSelect>
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
