<script setup>
import { ref, onMounted } from 'vue'
import { getAdminApiKeyList, deleteApiKeyAdmin } from '@/api/apiKey'
import { ElMessage, ElMessageBox } from 'element-plus'

const apiKeys = ref([])
const loading = ref(false)

const loadApiKeys = async () => {
  loading.value = true
  try {
    const res = await getAdminApiKeyList({ page: 1, size: 100 })
    if (res.code === 200) {
      apiKeys.value = res.data.records || []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该API Key吗？', '提示', { type: 'warning' })
    const res = await deleteApiKeyAdmin(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadApiKeys()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  loadApiKeys()
})
</script>

<template>
  <div class="api-key-manage-view page-container">
    <h2 class="page-title">API Key 管理</h2>
    
    <el-table :data="apiKeys" v-loading="loading" class="card">
      <el-table-column prop="keyName" label="名称" width="150" />
      <el-table-column prop="apiKey" label="API Key" min-width="250" />
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="expireTime" label="过期时间" width="180">
        <template #default="{ row }">
          {{ row.expireTime || '永不过期' }}
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
.api-key-manage-view {
  max-width: 1200px;
}
</style>
