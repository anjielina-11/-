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
  type FormInstance
} from 'element-plus'

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
    { type: 'number', min: 0, message: '面积必须大于0', trigger: 'blur' }
  ],
  cropType: [
    { required: true, message: '请输入作物类型', trigger: 'blur' }
  ],
  soilType: [
    { required: true, message: '请输入土壤类型', trigger: 'blur' }
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
  <div class="farm-list-container">
    <div class="search-bar">
      <ElInput
        v-model="searchKeyword"
        placeholder="按名称模糊查询"
        clearable
        style="width: 300px"
        @keyup.enter="handleSearch"
      />
      <ElButton type="primary" @click="handleSearch">搜索</ElButton>
      <ElButton @click="handleReset">重置</ElButton>
      <ElButton type="success" @click="handleAdd" style="margin-left: auto">
        新增地块
      </ElButton>
    </div>

    <ElTable
      :data="tableData"
      :loading="loading"
      border
      style="width: 100%"
    >
      <ElTableColumn prop="name" label="地块名称" />
      <ElTableColumn prop="farmName" label="所属农场" />
      <ElTableColumn prop="area" label="面积（亩）">
        <template #default="{ row }">
          {{ (row as IFarm).area.toFixed(1) }}
        </template>
      </ElTableColumn>
      <ElTableColumn prop="cropType" label="作物类型" />
      <ElTableColumn prop="soilType" label="土壤类型" />
      <ElTableColumn prop="createdAt" label="创建时间" width="180" />
      <ElTableColumn label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <ElButton type="primary" link @click="handleEdit(row as IFarm)">编辑</ElButton>
          <ElButton type="danger" link @click="handleDelete(row as IFarm)">删除</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <div class="pagination-bar">
      <ElPagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>

    <ElDialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      @close="handleClose"
    >
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="100px">
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
          <ElInput v-model="form.cropType" placeholder="请输入作物类型" list="cropTypeList" />
          <datalist id="cropTypeList">
            <option v-for="type in cropTypes" :key="type" :value="type" />
          </datalist>
        </ElFormItem>
        <ElFormItem label="土壤类型" prop="soilType">
          <ElInput v-model="form.soilType" placeholder="请输入土壤类型" list="soilTypeList" />
          <datalist id="soilTypeList">
            <option v-for="type in soilTypes" :key="type" :value="type" />
          </datalist>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="handleClose">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.farm-list-container {
  padding: 20px;
  background: #fafafa;
  min-height: calc(100vh - 60px);
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
</style>