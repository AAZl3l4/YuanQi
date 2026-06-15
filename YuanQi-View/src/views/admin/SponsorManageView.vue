<script setup>
import { ref, onMounted } from 'vue'
import { getSponsorPage, addSponsor, updateSponsor, deleteSponsor } from '@/api/sponsor'
import { ElMessage, ElMessageBox } from 'element-plus'

const sponsorList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)

const pagination = ref({
  page: 1,
  size: 10,
  total: 0
})

const form = ref({
  id: null,
  name: '',
  adContent: '',
  amount: null,
  remark: ''
})

// 加载列表
const loadList = async () => {
  loading.value = true
  try {
    const res = await getSponsorPage({ page: pagination.value.page, size: pagination.value.size })
    if (res.code === 200) {
      sponsorList.value = res.data.records || []
      pagination.value.total = res.data.total || 0
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 分页切换
const handlePageChange = (page) => {
  pagination.value.page = page
  loadList()
}

// 打开新增弹窗
const openAddDialog = () => {
  isEdit.value = false
  form.value = { id: null, name: '', adContent: '', amount: null, remark: '' }
  dialogVisible.value = true
}

// 打开编辑弹窗
const openEditDialog = (row) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  if (!form.value.name || !form.value.amount) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    if (isEdit.value) {
      await updateSponsor(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await addSponsor(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } catch (error) {
    console.error(error)
  }
}

// 删除
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该记录吗？', '提示', { type: 'warning' })
    await deleteSponsor(id)
    ElMessage.success('删除成功')
    loadList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  loadList()
})
</script>

<template>
  <div class="sponsor-manage-view page-container">
    <div class="page-header">
      <h2 class="page-title">
        <el-icon class="title-icon"><Trophy /></el-icon>
        赞助记录管理
      </h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        新增记录
      </el-button>
    </div>

    <el-table :data="sponsorList" v-loading="loading" class="card">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="赞助者名称" width="150" />
      <el-table-column prop="adContent" label="广告位内容" min-width="200" show-overflow-tooltip />
      <el-table-column prop="amount" label="赞助金额" width="120">
        <template #default="{ row }">
          <span class="amount-text">¥{{ Number(row.amount).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" width="150" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper" v-if="pagination.total > 0">
      <el-pagination
        v-model:current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑记录' : '新增记录'" width="500px" class="custom-dialog">
      <el-form :model="form" label-width="100px">
        <el-form-item label="赞助者名称" required>
          <el-input v-model="form.name" placeholder="请输入赞助者名称" />
        </el-form-item>
        <el-form-item label="广告位内容">
          <el-input v-model="form.adContent" placeholder="请输入广告位展示内容" />
        </el-form-item>
        <el-form-item label="赞助金额" required>
          <el-input-number v-model="form.amount" :precision="2" :min="0" style="width: 100%" placeholder="请输入金额" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.sponsor-manage-view {
  max-width: 1200px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.page-title {
  margin: 0;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 22px;
}

.title-icon {
  color: var(--color-primary);
  font-size: 24px;
}

.amount-text {
  color: #e6a23c;
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-lg);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--color-border-light);
}
</style>
