<script setup>
import { ref, onMounted } from 'vue'
import { getKnowledgeList, deleteKnowledgeAdmin } from '@/api/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'

const knowledgeBases = ref([])
const loading = ref(false)

const loadKnowledge = async () => {
  loading.value = true
  try {
    const res = await getKnowledgeList({ page: 1, size: 100 })
    if (res.code === 200) {
      knowledgeBases.value = res.data.records || []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该知识库吗？', '提示', { type: 'warning' })
    const res = await deleteKnowledgeAdmin(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadKnowledge()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  loadKnowledge()
})
</script>

<template>
  <div class="knowledge-manage-view page-container">
    <h2 class="page-title">知识库管理</h2>
    
    <el-table :data="knowledgeBases" v-loading="loading" class="card">
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="username" label="创建者" width="120" />
      <el-table-column prop="chunkCount" label="分块数" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '可用' : '处理中' }}
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
.knowledge-manage-view {
  max-width: 1200px;
}
</style>
