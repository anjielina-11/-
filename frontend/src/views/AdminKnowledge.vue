<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem,
  ElInput, ElSelect, ElOption, ElMessage, ElTag, ElIcon
} from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Document, Files, FolderOpened } from '@element-plus/icons-vue'

interface Knowledge {
  id: string
  title: string
  category: 'disease' | 'pest' | 'culture' | 'other'
  content: string
  keywords: string
  status: 'published' | 'draft'
  createdAt: string
  updatedAt: string
}

const mockKnowledge: Knowledge[] = [
  { id: '1', title: '稻瘟病防治技术', category: 'disease', content: '稻瘟病是水稻重要病害之一，严重影响产量。防治方法：1.选用抗病品种；2.合理施肥；3.药剂防治可选用三环唑、稻瘟灵等。发病初期及时喷药，每隔7-10天喷一次，连续喷2-3次。', keywords: '水稻,稻瘟病,三环唑', status: 'published', createdAt: '2026-01-10', updatedAt: '2026-05-15' },
  { id: '2', title: '玉米螟综合防治', category: 'pest', content: '玉米螟是玉米主要害虫，幼虫蛀茎为害。防治措施：1.农业防治：秋收后及时清除田间残株；2.生物防治：释放赤眼蜂；3.化学防治：在幼虫钻蛀前使用辛硫磷颗粒剂。', keywords: '玉米,玉米螟,赤眼蜂', status: 'published', createdAt: '2026-02-15', updatedAt: '2026-06-20' },
  { id: '3', title: '番茄栽培管理要点', category: 'culture', content: '番茄栽培技术要点：1.育苗：选择适宜品种，适时播种；2.定植：合理密植，施足基肥；3.田间管理：及时整枝打杈，注意防治晚疫病和蚜虫；4.采收：适时采收，避免过熟。', keywords: '番茄,栽培,管理', status: 'published', createdAt: '2026-03-01', updatedAt: '2026-03-01' },
  { id: '4', title: '草莓白粉病防治', category: 'disease', content: '草莓白粉病主要危害叶片和果实。防治方法：1.加强通风透光；2.及时摘除病叶；3.药剂防治可选用腈菌唑、嘧菌酯等。注意药剂交替使用，避免抗药性产生。', keywords: '草莓,白粉病,腈菌唑', status: 'draft', createdAt: '2026-06-10', updatedAt: '2026-06-10' },
  { id: '5', title: '黄瓜霜霉病识别与防治', category: 'disease', content: '黄瓜霜霉病症状：叶片出现黄色多角形病斑，背面生霜状霉层。防治方法：1.选用抗病品种；2.控制湿度；3.药剂防治：霜霉威盐酸盐、烯酰吗啉等。', keywords: '黄瓜,霜霉病,霜霉威', status: 'published', createdAt: '2026-04-20', updatedAt: '2026-04-20' },
  { id: '6', title: '有机蔬菜种植指南', category: 'culture', content: '有机蔬菜种植原则：1.禁止使用化学合成农药和化肥；2.采用生物防治和物理防治；3.轮作倒茬；4.使用有机肥。有机认证需符合国家相关标准。', keywords: '有机,蔬菜,种植', status: 'draft', createdAt: '2026-07-01', updatedAt: '2026-07-05' }
]

const knowledge = ref<Knowledge[]>([...mockKnowledge])
const dialogVisible = ref(false)
const editMode = ref(false)
const formData = ref<Knowledge>({
  id: '',
  title: '',
  category: 'disease',
  content: '',
  keywords: '',
  status: 'draft',
  createdAt: '',
  updatedAt: ''
})

// 搜索和筛选
const searchKeyword = ref('')
const filterCategory = ref('')
const filterStatus = ref('')

const filteredKnowledge = computed(() => {
  return knowledge.value.filter(item => {
    const matchKeyword = !searchKeyword.value ||
      item.title.includes(searchKeyword.value) ||
      item.keywords.includes(searchKeyword.value)
    const matchCategory = !filterCategory.value || item.category === filterCategory.value
    const matchStatus = !filterStatus.value || item.status === filterStatus.value
    return matchKeyword && matchCategory && matchStatus
  })
})

// 统计数据
const publishedCount = computed(() => knowledge.value.filter(k => k.status === 'published').length)
const draftCount = computed(() => knowledge.value.filter(k => k.status === 'draft').length)
const totalCount = computed(() => knowledge.value.length)

const handleSearch = () => {
  // 前端过滤由 computed 自动处理
}

const handleReset = () => {
  searchKeyword.value = ''
  filterCategory.value = ''
  filterStatus.value = ''
}

const categoryLabels: Record<string, string> = { disease: '病害防治', pest: '虫害防治', culture: '栽培技术', other: '其他' }
const categoryColors: Record<string, string> = { disease: 'danger', pest: 'warning', culture: 'success', other: 'info' }
const statusLabels: Record<string, string> = { published: '已发布', draft: '草稿' }
const statusColors: Record<string, string> = { published: 'success', draft: 'info' }

const handleAdd = () => {
  editMode.value = false
  formData.value = {
    id: '',
    title: '',
    category: 'disease',
    content: '',
    keywords: '',
    status: 'draft',
    createdAt: '',
    updatedAt: ''
  }
  dialogVisible.value = true
}

const handleEdit = (row: Knowledge) => {
  editMode.value = true
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = (id: string) => {
  knowledge.value = knowledge.value.filter(k => k.id !== id)
  ElMessage.success('删除成功')
}

const handleSubmit = () => {
  const now = new Date().toISOString().split('T')[0]
  if (editMode.value) {
    const index = knowledge.value.findIndex(k => k.id === formData.value.id)
    if (index !== -1) {
      formData.value.updatedAt = now
      knowledge.value[index] = { ...formData.value }
      ElMessage.success('修改成功')
    }
  } else {
    formData.value.id = String(Date.now())
    formData.value.createdAt = now
    formData.value.updatedAt = now
    knowledge.value.unshift({ ...formData.value })
    ElMessage.success('添加成功')
  }
  dialogVisible.value = false
}

const getKeywordList = (keywords: string): string[] => {
  if (!keywords) return []
  return keywords.split(',').map(k => k.trim()).filter(k => k)
}
</script>

<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">知识库管理</h1>
        <p class="page-subtitle">管理农业技术知识文档</p>
      </div>
    </div>

    <!-- 统计卡片行 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-indicator stat-indicator--success"></div>
        <div class="stat-content">
          <div class="stat-value">{{ publishedCount }}</div>
          <div class="stat-label">已发布</div>
        </div>
        <div class="stat-icon stat-icon--success">
          <ElIcon :size="28"><Document /></ElIcon>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-indicator stat-indicator--info"></div>
        <div class="stat-content">
          <div class="stat-value">{{ draftCount }}</div>
          <div class="stat-label">草稿</div>
        </div>
        <div class="stat-icon stat-icon--info">
          <ElIcon :size="28"><Files /></ElIcon>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-indicator stat-indicator--primary"></div>
        <div class="stat-content">
          <div class="stat-value">{{ totalCount }}</div>
          <div class="stat-label">总文档数</div>
        </div>
        <div class="stat-icon stat-icon--primary">
          <ElIcon :size="28"><FolderOpened /></ElIcon>
        </div>
      </div>
    </div>

    <!-- 搜索筛选栏 -->
    <div class="filter-card">
      <div class="filter-row">
        <div class="filter-items">
          <ElInput
            v-model="searchKeyword"
            placeholder="搜索标题/关键词"
            :prefix-icon="Search"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
          <ElSelect v-model="filterCategory" placeholder="分类筛选" clearable style="width: 150px">
            <ElOption label="病害防治" value="disease" />
            <ElOption label="虫害防治" value="pest" />
            <ElOption label="栽培技术" value="culture" />
            <ElOption label="其他" value="other" />
          </ElSelect>
          <ElSelect v-model="filterStatus" placeholder="状态筛选" clearable style="width: 130px">
            <ElOption label="已发布" value="published" />
            <ElOption label="草稿" value="draft" />
          </ElSelect>
          <ElButton type="primary" :icon="Search" @click="handleSearch">搜索</ElButton>
          <ElButton :icon="Refresh" @click="handleReset">重置</ElButton>
        </div>
        <ElButton type="primary" :icon="Plus" @click="handleAdd">添加知识</ElButton>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <ElTable :data="filteredKnowledge" style="width: 100%" :header-cell-style="{ background: 'var(--color-bg-page)', color: 'var(--color-text-secondary)', fontWeight: '600' }">
        <ElTableColumn prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <span class="cell-text-primary">{{ row.title }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="category" label="分类" min-width="110" align="center">
          <template #default="{ row }">
            <ElTag :type="(categoryColors[row.category] as any)" size="small" effect="light">{{ categoryLabels[row.category] }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="keywords" label="关键词" min-width="200">
          <template #default="{ row }">
            <div class="keyword-tags">
              <span
                v-for="kw in getKeywordList(row.keywords)"
                :key="kw"
                class="keyword-tag"
              >{{ kw }}</span>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="status" label="状态" min-width="90" align="center">
          <template #default="{ row }">
            <ElTag :type="(statusColors[row.status] as any)" size="small" effect="light">{{ statusLabels[row.status] }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="createdAt" label="创建时间" min-width="110">
          <template #default="{ row }">
            <span class="cell-text-secondary">{{ row.createdAt }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="updatedAt" label="更新时间" min-width="110">
          <template #default="{ row }">
            <span class="cell-text-secondary">{{ row.updatedAt }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" min-width="120" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <ElButton type="primary" link :icon="Edit" @click="handleEdit(row as Knowledge)" />
              <ElButton type="danger" link :icon="Delete" @click="handleDelete((row as Knowledge).id)" />
            </div>
          </template>
        </ElTableColumn>
      </ElTable>
    </div>

    <!-- 添加/编辑对话框 -->
    <ElDialog
      :title="editMode ? '编辑知识' : '添加知识'"
      v-model="dialogVisible"
      width="680px"
      :close-on-click-modal="false"
      class="custom-dialog"
    >
      <ElForm :model="formData" label-width="90px" label-position="right" class="dialog-form">
        <ElFormItem label="标题" required>
          <ElInput v-model="formData.title" placeholder="请输入标题" />
        </ElFormItem>
        <ElFormItem label="分类" required>
          <ElSelect v-model="formData.category" placeholder="请选择分类" style="width: 100%">
            <ElOption label="病害防治" value="disease" />
            <ElOption label="虫害防治" value="pest" />
            <ElOption label="栽培技术" value="culture" />
            <ElOption label="其他" value="other" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="关键词">
          <ElInput v-model="formData.keywords" placeholder="多个关键词用逗号分隔" />
        </ElFormItem>
        <ElFormItem label="内容" required>
          <ElInput
            v-model="formData.content"
            type="textarea"
            :rows="10"
            placeholder="请输入知识内容"
            class="content-textarea"
          />
        </ElFormItem>
        <ElFormItem label="状态">
          <ElSelect v-model="formData.status" style="width: 100%">
            <ElOption label="草稿" value="draft" />
            <ElOption label="已发布" value="published" />
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

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.stat-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--spacing-lg);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  position: relative;
  overflow: hidden;
  transition: box-shadow var(--transition-normal), transform var(--transition-normal);
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.stat-indicator {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: 0 4px 4px 0;
}

.stat-indicator--success {
  background: var(--color-success);
}

.stat-indicator--info {
  background: var(--color-info);
}

.stat-indicator--primary {
  background: var(--color-primary);
}

.stat-content {
  flex: 1;
  padding-left: var(--spacing-sm);
}

.stat-value {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: var(--spacing-xs);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon--success {
  background: rgba(82, 196, 26, 0.1);
  color: var(--color-success);
}

.stat-icon--info {
  background: rgba(24, 144, 255, 0.1);
  color: var(--color-info);
}

.stat-icon--primary {
  background: rgba(45, 125, 70, 0.1);
  color: var(--color-primary);
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

/* 关键词标签 */
.keyword-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.keyword-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  background: var(--color-primary-lighter);
  border-radius: 4px;
  line-height: 1.6;
  white-space: nowrap;
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

.content-textarea :deep(.el-textarea__inner) {
  min-height: 200px !important;
  border-radius: var(--radius-sm);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
}
</style>
