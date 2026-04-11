<script setup>
import { ref, onMounted } from 'vue'
import { getKnowledgeList, deleteKnowledgeAdmin } from '@/api/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'

const knowledgeBases = ref([])
const loading = ref(false)
const searchId = ref('')

const loadKnowledge = async () => {
  loading.value = true
  try {
    const params = { page: 1, size: 100 }
    if (searchId.value) {
      params.id = searchId.value
    }
    const res = await getKnowledgeList(params)
    if (res.code === 200) {
      knowledgeBases.value = res.data.records || []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadKnowledge()
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

const getFileName = (url) => {
  if (!url) return ''
  return url.split('/').pop()
}

const getDownloadUrl = (url) => {
  if (!url) return ''
  const parts = url.split('/api/file/')
  if (parts.length === 2) {
    return parts[0] + '/api/file/download/' + parts[1]
  }
  return url
}

onMounted(() => {
  loadKnowledge()
})
</script>

<template>
  <div class="knowledge-manage-view page-container">
    <div class="page-header">
      <h2 class="page-title">知识库管理</h2>
      <div class="header-right">
        <div class="search-filters">
          <el-input
            v-model="searchId"
            placeholder="ID"
            clearable
            class="filter-input"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
    
    <el-table :data="knowledgeBases" v-loading="loading" class="card">
      <el-table-column label="ID" width="100">
        <template #default="{ row }">
          <div class="id-item">
            <span class="id-label">ID</span>
            <span class="id-value">{{ row.id }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="username" label="创建者" width="120" />
      <el-table-column label="文档" min-width="180">
        <template #default="{ row }">
          <a v-if="row.documentUrl" :href="getDownloadUrl(row.documentUrl)" target="_blank" class="file-link">
            {{ getFileName(row.documentUrl) }}
          </a>
          <span v-else class="no-file">-</span>
        </template>
      </el-table-column>
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.header-right {
  display: flex;
  gap: var(--spacing-sm);
  align-items: center;
}

.search-filters {
  display: flex;
  gap: var(--spacing-xs);
  align-items: center;
}

.filter-input {
  width: 100px;
}

.id-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.id-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.id-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-primary);
}

.file-link {
  color: var(--color-primary);
  text-decoration: none;
}

.file-link:hover {
  text-decoration: underline;
}

.no-file {
  color: var(--color-text-muted);
}
</style>
