<script setup>
import { ref, onMounted, computed } from 'vue'
import { getAdminRelayLogs } from '@/api/relay'

const logs = ref([])
const loading = ref(false)
const searchText = ref('')
const expandedLogs = ref(new Set())
const imagePreviewVisible = ref(false)
const previewImageUrl = ref('')

const pagination = ref({
  page: 1,
  size: 100,
  total: 0
})

const filteredLogs = computed(() => {
  if (!searchText.value) return logs.value
  const keyword = searchText.value.toLowerCase()
  return logs.value.filter(log => 
    log.inputMessage?.toLowerCase().includes(keyword) ||
    log.outputMessage?.toLowerCase().includes(keyword) ||
    log.modelUsed?.toLowerCase().includes(keyword)
  )
})

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await getAdminRelayLogs({ 
      page: pagination.value.page, 
      size: pagination.value.size 
    })
    if (res.code === 200) {
      logs.value = res.data.records || []
      pagination.value.total = res.data.total || 0
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  pagination.value.page = page
  loadLogs()
}

const toggleLog = (id) => {
  if (expandedLogs.value.has(id)) {
    expandedLogs.value.delete(id)
  } else {
    expandedLogs.value.add(id)
  }
}

const isLogExpanded = (id) => expandedLogs.value.has(id)

const openImagePreview = (url) => {
  previewImageUrl.value = url
  imagePreviewVisible.value = true
}

onMounted(() => {
  loadLogs()
})
</script>

<template>
  <div class="relay-log-view page-container">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon class="title-icon"><DataLine /></el-icon>
          中转调用记录
        </h2>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-value">{{ pagination.total }}</span>
            <span class="stat-label">总调用</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-input
          v-model="searchText"
          placeholder="搜索消息、响应、模型..."
          prefix-icon="Search"
          clearable
          class="search-input"
        />
        <el-button @click="loadLogs" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>
    
    <div class="logs-grid" v-loading="loading">
      <div v-for="log in filteredLogs" :key="log.id" class="log-card card">
        <div class="log-header">
          <div class="id-row">
            <div class="id-item">
              <span class="id-label">用户</span>
              <span class="id-value">{{ log.userId || '-' }}</span>
            </div>
            <div class="id-item">
              <span class="id-label">配置</span>
              <span class="id-value">{{ log.configId || '-' }}</span>
            </div>
          </div>
          <span class="log-time">{{ log.createTime }}</span>
        </div>
        
        <div class="log-body">
          <div class="log-message" v-if="log.inputMessage">
            <div class="message-header" @click="toggleLog('i' + log.id)">
              <span class="label">
                <el-icon><ChatDotRound /></el-icon>
                输入消息
              </span>
              <el-icon class="expand-icon" :class="{ expanded: isLogExpanded('i' + log.id) }">
                <ArrowDown />
              </el-icon>
            </div>
            <div class="message-content" :class="{ expanded: isLogExpanded('i' + log.id) }">
              {{ log.inputMessage }}
            </div>
          </div>
          
          <div class="log-response" v-if="log.outputMessage">
            <div class="response-header" @click="toggleLog('o' + log.id)">
              <span class="label">
                <el-icon><Document /></el-icon>
                输出响应
              </span>
              <el-icon class="expand-icon" :class="{ expanded: isLogExpanded('o' + log.id) }">
                <ArrowDown />
              </el-icon>
            </div>
            <div class="response-content" :class="{ expanded: isLogExpanded('o' + log.id) }">
              {{ log.outputMessage }}
            </div>
          </div>
          
          <div class="log-image" v-if="log.imageUrl">
            <span class="label">
              <el-icon><Picture /></el-icon>
              图片
            </span>
            <div class="image-wrapper" @click="openImagePreview(log.imageUrl)">
              <img :src="log.imageUrl" alt="调用图片" />
              <div class="image-tip">点击查看大图</div>
            </div>
            <div class="image-notice">
              <el-icon><Warning /></el-icon>
              <span>QQ图片可能无法正常显示</span>
            </div>
          </div>
        </div>
        
        <div class="log-footer">
          <div class="token-info">
            <span class="token-item">
              <el-icon><Upload /></el-icon>
              {{ log.inputTokens || 0 }}
            </span>
            <span class="token-item">
              <el-icon><Download /></el-icon>
              {{ log.outputTokens || 0 }}
            </span>
            <span class="model-tag" v-if="log.modelUsed">{{ log.modelUsed }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <el-empty v-if="!loading && filteredLogs.length === 0" description="暂无调用记录">
      <template #image>
        <el-icon :size="60" color="#c0c4cc"><Document /></el-icon>
      </template>
    </el-empty>
    
    <div class="pagination-wrapper" v-if="pagination.total > 0">
      <el-pagination
        v-model:current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
    
    <el-dialog 
      v-model="imagePreviewVisible" 
      title="图片预览" 
      width="800px"
      class="image-preview-dialog"
    >
      <img :src="previewImageUrl" alt="预览图片" class="preview-image-full" />
    </el-dialog>
  </div>
</template>

<style scoped>
.relay-log-view {
  max-width: 1400px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-lg);
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
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

.header-stats {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding-left: 32px;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.search-input {
  width: 260px;
}

.logs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: var(--spacing-md);
}

.log-card {
  display: flex;
  flex-direction: column;
  padding: var(--spacing-md);
  transition: all 0.3s ease;
  border-radius: 12px;
}

.log-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-sm);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-border-light);
}

.id-row {
  display: flex;
  gap: var(--spacing-md);
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

.log-time {
  font-size: 12px;
  color: var(--color-text-muted);
  white-space: nowrap;
}

.log-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.log-message,
.log-response {
  background: var(--color-bg-secondary);
  border-radius: 8px;
  padding: var(--spacing-sm);
}

.message-header,
.response-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.message-header:hover .label,
.response-header:hover .label {
  color: var(--color-primary);
}

.message-header .label,
.response-header .label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
  transition: color 0.2s;
}

.expand-icon {
  font-size: 12px;
  color: var(--color-text-muted);
  transition: transform 0.3s ease;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.message-content,
.response-content {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-top: var(--spacing-xs);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  transition: all 0.3s ease;
}

.message-content.expanded,
.response-content.expanded {
  -webkit-line-clamp: unset;
  display: block;
}

.log-image {
  background: var(--color-bg-secondary);
  border-radius: 8px;
  padding: var(--spacing-sm);
}

.log-image .label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-xs);
}

.image-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 6px;
  overflow: hidden;
}

.image-wrapper img {
  width: 100%;
  max-height: 120px;
  object-fit: cover;
  border-radius: 6px;
  transition: opacity 0.2s;
}

.image-wrapper:hover img {
  opacity: 0.8;
}

.image-wrapper:hover .image-tip {
  opacity: 1;
}

.image-tip {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-notice {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: var(--spacing-xs);
  font-size: 11px;
  color: var(--color-warning);
}

.image-notice .el-icon {
  font-size: 12px;
}

.log-footer {
  margin-top: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border-light);
}

.token-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.token-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.token-item .el-icon {
  font-size: 12px;
}

.model-tag {
  font-size: 11px;
  color: var(--color-primary);
  background: var(--color-primary-light);
  padding: 2px 8px;
  border-radius: 4px;
  margin-left: auto;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-lg);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--color-border-light);
}

.preview-image-full {
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }
  
  .search-input {
    width: 100%;
  }
  
  .logs-grid {
    grid-template-columns: 1fr;
  }
  
  .id-row {
    flex-direction: column;
    gap: var(--spacing-xs);
  }
}
</style>
