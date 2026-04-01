<script setup>
import { ref, onMounted } from 'vue'
import { getAdminAgentList, deleteAgentAdmin } from '@/api/agent'
import { ElMessage, ElMessageBox } from 'element-plus'

const agents = ref([])
const loading = ref(false)

const loadAgents = async () => {
  loading.value = true
  try {
    const res = await getAdminAgentList({ page: 1, size: 100 })
    if (res.code === 200) {
      agents.value = res.data.records || []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该智能体吗？', '提示', { type: 'warning' })
    const res = await deleteAgentAdmin(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadAgents()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  loadAgents()
})
</script>

<template>
  <div class="agent-manage-view page-container">
    <h2 class="page-title">智能体管理</h2>
    
    <el-table :data="agents" v-loading="loading" class="card">
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="username" label="创建者" width="120" />
      <el-table-column prop="isPublic" label="公开" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isPublic ? 'success' : 'info'" size="small">
            {{ row.isPublic ? '是' : '否' }}
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
.agent-manage-view {
  max-width: 1200px;
}
</style>
