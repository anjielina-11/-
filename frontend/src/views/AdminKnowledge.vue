<script setup lang="ts">
import { ref } from 'vue'
import { ElCard, ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElMessage, ElTag } from 'element-plus'

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

const categoryLabels: Record<string, string> = { disease: '病害防治', pest: '虫害防治', culture: '栽培技术', other: '其他' }
const categoryColors: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = { disease: 'danger', pest: 'warning', culture: 'success', other: 'info' }
const statusColors: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = { published: 'success', draft: 'info' }

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
</script>

<template>
  <div class="admin-knowledge-container">
    <ElCard class="main-card" header="知识库管理">
      <div class="card-header">
        <ElButton type="primary" @click="handleAdd">添加知识</ElButton>
      </div>
      <ElTable :data="knowledge" border style="width: 100%">
        <ElTableColumn prop="title" label="标题" min-width="200" />
        <ElTableColumn prop="category" label="分类" min-width="120">
          <template #default="{ row }">
            <ElTag :type="categoryColors[row.category]">{{ categoryLabels[row.category] }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="keywords" label="关键词" min-width="150" />
        <ElTableColumn prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <ElTag :type="statusColors[row.status]">{{ row.status === 'published' ? '已发布' : '草稿' }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="createdAt" label="创建时间" min-width="120" />
        <ElTableColumn prop="updatedAt" label="更新时间" min-width="120" />
        <ElTableColumn label="操作" min-width="150" fixed="right">
          <template #default="{ row }">
            <ElButton type="primary" link @click="handleEdit(row as Knowledge)">编辑</ElButton>
            <ElButton type="danger" link @click="handleDelete((row as Knowledge).id)">删除</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
    </ElCard>

    <ElDialog :title="editMode ? '编辑知识' : '添加知识'" v-model="dialogVisible" width="700px">
      <ElForm :model="formData" label-width="100px">
        <ElFormItem label="标题" required>
          <ElInput v-model="formData.title" placeholder="请输入标题" />
        </ElFormItem>
        <ElFormItem label="分类" required>
          <ElSelect v-model="formData.category" placeholder="请选择分类">
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
          <ElInput v-model="formData.content" type="textarea" :rows="8" placeholder="请输入知识内容" />
        </ElFormItem>
        <ElFormItem label="状态">
          <ElSelect v-model="formData.status">
            <ElOption label="草稿" value="draft" />
            <ElOption label="已发布" value="published" />
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
.admin-knowledge-container {
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