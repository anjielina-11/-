<template>
  <div class="admin-page">
    <div class="ap-head"><h2 class="page-title">🍽️ 商品管理</h2><el-button class="add-btn" round @click="openAdd">+ 添加商品</el-button></div>
    <div class="toolbar">
      <el-select v-model="filterCat" placeholder="筛选分类" clearable style="width:160px"><el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"/></el-select>
      <el-input v-model="keyword" placeholder="🔍 搜索..." clearable style="width:240px" class="food-input" />
    </div>
    <div class="food-card" style="padding:0;overflow:hidden">
      <el-table :data="filteredProducts" stripe>
        <el-table-column type="index" label="#" width="50"/>
        <el-table-column label="商品" min-width="220">
          <template #default="{r}"><div style="display:flex;align-items:center;gap:10px"><img :src="r.image" @error="e=>e.target.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 50 50%22%3E%3Crect fill=%22%23fef3c7%22 width=%2250%22 height=%2250%22/%3E%3Ctext x=%2225%22 y=%2233%22 text-anchor=%22middle%22 font-size=%2225%22%3E🍽%3C/text%3E%3C/svg%3E'" style="width:44px;height:44px;border-radius:8px;object-fit:cover"/><div><div style="font-weight:700">{{r.name}}</div><div style="color:#8b6f65;font-size:12px">{{r.description}}</div></div></div></template>
        </el-table-column>
        <el-table-column label="分类" width="90"><template #default="{r}">{{getCatName(r.categoryId)}}</template></el-table-column>
        <el-table-column label="价格" width="90"><template #default="{r}"><span class="price-tag">{{r.price}}</span></template></el-table-column>
        <el-table-column label="库存" width="80"><template #default="{r}"><el-tag :type="r.stock>20?'success':r.stock>0?'warning':'danger'" size="small">{{r.stock}}</el-tag></template></el-table-column>
        <el-table-column prop="sales" label="销量" width="70"/>
        <el-table-column label="时间" width="100"><template #default="{r}"><span style="font-size:12px;color:#8b6f65">{{r.createTime?.substring(0,10)}}</span></template></el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{r}"><el-button size="small" text type="primary" @click="openEdit(r)">编辑</el-button><el-button size="small" text type="danger" @click="handleDelete(r)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="ed?'编辑商品':'添加商品'" width="520px" class="food-dialog">
      <el-form :model="form" label-width="70px">
        <el-form-item label="名称"><el-input v-model="form.name" class="food-input"/></el-form-item>
        <el-form-item label="分类"><el-select v-model="form.categoryId"><el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"/></el-select></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2"/></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0"/></el-form-item>
        <el-form-item label="图片"><el-input v-model="form.image" placeholder="图片URL" class="food-input"/></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" class="food-input"/></el-form-item>
        <el-form-item label="上架"><el-switch v-model="form.status" :active-value="1" :inactive-value="0"/></el-form-item>
      </el-form>
      <template #footer><el-button round @click="dialogVisible=false">取消</el-button><el-button round type="primary" @click="handleSave" :loading="s">{{ed?'更新':'创建'}}</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getProducts, getCategories, createProduct, updateProduct, deleteProduct } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
const products=ref([]),categories=ref([]),dialogVisible=ref(false),ed=ref(false),s=ref(false),filterCat=ref(null),keyword=ref('')
const form=reactive({name:'',categoryId:null,price:0,stock:0,image:'',description:'',status:1})
onMounted(async()=>{const[p,c]=await Promise.all([getProducts(),getCategories()]);products.value=p.data.data||[];categories.value=c.data.data||[]})
const filteredProducts=computed(()=>{let l=products.value;if(filterCat.value)l=l.filter(p=>p.categoryId===filterCat.value);if(keyword.value)l=l.filter(p=>p.name.includes(keyword.value));return l})
function getCatName(id){return categories.value.find(c=>c.id===id)?.name||''}
function openAdd(){ed.value=false;Object.assign(form,{name:'',categoryId:null,price:0,stock:0,image:'',description:'',status:1});dialogVisible.value=true}
function openEdit(r){ed.value=true;Object.assign(form,{name:r.name,categoryId:r.categoryId,price:r.price,stock:r.stock,image:r.image||'',description:r.description||'',status:r.status});form.id=r.id;dialogVisible.value=true}
async function handleSave(){s.value=true;try{if(ed.value)await updateProduct(form.id,form);else await createProduct(form);ElMessage.success(ed.value?'已更新':'已创建');dialogVisible.value=false;products.value=((await getProducts()).data.data)||[]}catch(e){ElMessage.error(e.response?.data?.message||'失败')}finally{s.value=false}}
async function handleDelete(r){await ElMessageBox.confirm(`删除"${r.name}"？`,'确认',{type:'warning'});try{await deleteProduct(r.id);ElMessage.success('已删除');products.value=((await getProducts()).data.data)||[]}catch(e){ElMessage.error(e.response?.data?.message||'失败')}}
</script>

<style scoped>
.admin-page{max-width:1400px}.ap-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px}.add-btn{background:#e65d3e;color:#fff;border:none;font-weight:700}.toolbar{display:flex;gap:12px;margin-bottom:16px}
</style>
