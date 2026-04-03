<script setup>
import { ref, onMounted } from 'vue'
import { getContentList, deleteContentAdmin } from '@/api/content'
import { ElMessage, ElMessageBox } from 'element-plus'

const contents = ref([])
const loading = ref(false)
const activeType = ref('image')
const previewVisible = ref(false)
const previewUrl = ref('')

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

const openPreview = (url) => {
  previewUrl.value = url
  previewVisible.value = true
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
      <el-table-column prop="prompt" label="提示词" min-width="180" show-overflow-tooltip />
      <el-table-column label="生成结果" width="120">
        <template #default="{ row }">
          <div v-if="row.resultUrl" class="result-preview">
            <img 
              v-if="activeType === 'image'" 
              :src="row.resultUrl" 
              class="result-thumb"
              @click="openPreview(row.resultUrl)"
            />
            <div v-else class="video-thumb" @click="openPreview(row.resultUrl)">
              <el-icon><VideoPlay /></el-icon>
              <span>点击预览</span>
            </div>
          </div>
          <span v-else class="no-result">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button text type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-dialog v-model="previewVisible" :title="activeType === 'image' ? '图片预览' : '视频预览'" width="800px">
      <img v-if="activeType === 'image'" :src="previewUrl" class="preview-image" />
      <video v-else :src="previewUrl" class="preview-video" controls autoplay />
    </el-dialog>
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

.result-preview {
  display: flex;
  align-items: center;
}

.result-thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
  transition: transform 0.2s;
}

.result-thumb:hover {
  transform: scale(1.1);
}

.video-thumb {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  background: var(--color-bg-secondary);
  border-radius: 6px;
  cursor: pointer;
  gap: 2px;
}

.video-thumb:hover {
  background: var(--color-primary-light);
}

.video-thumb .el-icon {
  font-size: 20px;
  color: var(--color-primary);
}

.video-thumb span {
  font-size: 10px;
  color: var(--color-text-muted);
}

.no-result {
  color: var(--color-text-muted);
}

.preview-image {
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.preview-video {
  width: 100%;
  max-height: 70vh;
}
</style>
