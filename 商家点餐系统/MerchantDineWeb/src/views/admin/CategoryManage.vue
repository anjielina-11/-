<template>
  <div class="admin-page">
    <div class="ap-head"><h2 class="page-title">📂 分类管理</h2><el-button class="add-btn" round @click="openAdd">+ 添加分类</el-button></div>
    <div class="cat-grid">
      <div v-for="cat in categories" :key="cat.id" class="food-card cat-card">
        <div class="cc-body">
          <div class="cc-top"><span class="cc-id">#{{ cat.id }}</span><el-tag :type="cat.status===1?'success':'info'" size="small">{{cat.status===1?'启用':'禁用'}}</el-tag></div>
          <h4>{{ cat.name }}</h4>
          <div class="cc-meta">排序 {{ cat.sortOrder }} · {{ cat.createTime }}</div>
        </div>
        <el-button class="del-btn" text @click="handleDelete(cat)">🗑️</el-button>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="添加分类" width="420px" class="food-dialog">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" class="food-input" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button round @click="dialogVisible=false">取消</el-button><el-button round type="primary" @click="handleAdd" :loading="a">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCategories, createCategory, deleteCategory } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
const categories = ref([]), dialogVisible = ref(false), a = ref(false)
const form = reactive({ name:'', sortOrder:0, status:1 })
onMounted(async()=>{categories.value=((await getCategories()).data.data)||[]})
function openAdd(){Object.assign(form,{name:'',sortOrder:0,status:1});dialogVisible.value=true}
async function handleAdd(){a.value=true;try{await createCategory({...form});ElMessage.success('创建成功');dialogVisible.value=false;categories.value=((await getCategories()).data.data)||[]}catch(e){ElMessage.error(e.response?.data?.message||'失败')}finally{a.value=false}}
async function handleDelete(cat){await ElMessageBox.confirm(`删除"${cat.name}"？`,'确认',{type:'warning'});try{await deleteCategory(cat.id);ElMessage.success('已删除');categories.value=((await getCategories()).data.data)||[]}catch(e){ElMessage.error(e.response?.data?.message||'失败')}}
</script>

<style scoped>
.admin-page { max-width: 1000px; }
.ap-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.add-btn { background: #e65d3e; color: #fff; border: none; font-weight: 700; }
.cat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 14px; }
.cat-card { padding: 20px; display: flex; justify-content: space-between; align-items: center; }
.cc-body h4 { font-size: 17px; margin-bottom: 6px; }
.cc-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.cc-id { color: #b08b7a; font-size: 12px; }
.cc-meta { color: #8b6f65; font-size: 12px; }
.del-btn { opacity: 0.3; transition: opacity .2s; padding: 4px; }
.del-btn:hover { opacity: 1; color: #ef4444; }
</style>
