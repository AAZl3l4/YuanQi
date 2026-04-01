<script setup>
import { ref, onMounted } from 'vue'
import { getContentList, deleteContentAdmin } from '@/api/content'
import { ElMessage, ElMessageBox } from 'element-plus'

const contents = ref([])
const loading = ref(false)
const activeType = ref('image')

const loadContents = async () => {
  loading.value = true
  try {
    const res = await getContentList({ page: 1, size: 100, type: activeType.value })
    if (res.code === 200) {
      contents.value = res.data.records || []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该记录吗？', '提示', { type: 'warning' })
    const res = await deleteContentAdmin(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadContents()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  loadContents()
})
</script>

<template>
  <div class="content-manage-view page-container">
    <div class="page-header">
      <h2 class="page-title">生成记录管理</h2>
      <el-radio-group v-model="activeType" @change="loadContents">
        <el-radio-button value="image">图片</el-radio-button>
        <el-radio-button value="video">视频</el-radio-button>
      </el-radio-group>
    </div>
    
    <el-table :data="contents" v-loading="loading" class="card">
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="prompt" label="提示词" min-width="200" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button text type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.content-manage-view {
  max-width: 1200px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}
</style>
