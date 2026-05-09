<script setup>
import { ref, onMounted, computed } from 'vue'
import { getAdminAgentList, deleteAgentAdmin } from '@/api/agent'
import { ElMessage, ElMessageBox } from 'element-plus'

const agents = ref([])
const loading = ref(false)
const searchText = ref('')
const expandedItems = ref(new Set())

const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

const filteredAgents = computed(() => {
  if (!searchText.value) return agents.value
  const keyword = searchText.value.toLowerCase()
  return agents.value.filter(a =>
    a.name?.toLowerCase().includes(keyword) ||
    a.username?.toLowerCase().includes(keyword) ||
    a.description?.toLowerCase().includes(keyword)
  )
})

const totalPublic = computed(() => agents.value.filter(a => a.isPublic === 1).length)

const loadAgents = async () => {
  loading.value = true
  try {
    const res = await getAdminAgentList({ page: pagination.value.page, size: pagination.value.size })
    if (res.code === 200) {
      agents.value = res.data.records || []
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
  loadAgents()
}

const handleDelete = async (agent) => {
  try {
    await ElMessageBox.confirm(
      `确定删除「${agent.name}」吗？此操作不可恢复。`, 
      '删除确认', 
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    const res = await deleteAgentAdmin(agent.id)
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

const toggleExpand = (id) => {
  if (expandedItems.value.has(id)) {
    expandedItems.value.delete(id)
  } else {
    expandedItems.value.add(id)
  }
}

const isExpanded = (id) => expandedItems.value.has(id)

const getAvatarColor = (name) => {
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#00d4aa', '#9b59b6', '#3498db']
  let hash = 0
  for (let i = 0; i < (name || '').length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

onMounted(() => {
  loadAgents()
})
</script>

<template>
  <div class="agent-manage-view page-container">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon class="title-icon"><UserFilled /></el-icon>
          智能体管理
        </h2>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-value">{{ agents.length }}</span>
            <span class="stat-label">总数</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value public">{{ totalPublic }}</span>
            <span class="stat-label">公开</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-input
          v-model="searchText"
          placeholder="搜索名称、创建者..."
          prefix-icon="Search"
          clearable
          class="search-input"
        />
      </div>
    </div>
    
    <div class="agent-grid" v-loading="loading">
      <div v-for="agent in filteredAgents" :key="agent.id" class="agent-card card">
        <div class="card-header">
          <div class="agent-avatar" :style="{ background: getAvatarColor(agent.name) }">
            <img v-if="agent.avatar" :src="agent.avatar" alt="" />
            <span v-else>{{ (agent.name || '智')[0] }}</span>
          </div>
          <div class="agent-info">
            <div class="agent-name">{{ agent.name }}</div>
            <div class="agent-meta">
              <div class="user-info">
                <span class="username">{{ agent.username }}</span>
              </div>
            </div>
          </div>
          <el-tag :type="agent.isPublic === 1 ? 'success' : 'info'" size="small">
            {{ agent.isPublic === 1 ? '公开' : '私有' }}
          </el-tag>
        </div>
        
        <div class="card-body">
          <div class="info-item">
            <span class="info-label">描述</span>
            <span class="info-value">{{ agent.description || '暂无描述' }}</span>
          </div>
          
          <div class="prompt-section">
            <div class="prompt-header" @click="toggleExpand('s' + agent.id)">
              <span class="prompt-label">
                <el-icon><Document /></el-icon>
                系统提示词
              </span>
              <el-icon class="expand-icon" :class="{ expanded: isExpanded('s' + agent.id) }">
                <ArrowDown />
              </el-icon>
            </div>
            <div class="prompt-content" :class="{ expanded: isExpanded('s' + agent.id) }">
              {{ agent.systemPrompt || '暂无系统提示词' }}
            </div>
          </div>
          
          <div class="prompt-section">
            <div class="prompt-header" @click="toggleExpand('w' + agent.id)">
              <span class="prompt-label">
                <el-icon><ChatDotRound /></el-icon>
                欢迎语
              </span>
              <el-icon class="expand-icon" :class="{ expanded: isExpanded('w' + agent.id) }">
                <ArrowDown />
              </el-icon>
            </div>
            <div class="prompt-content" :class="{ expanded: isExpanded('w' + agent.id) }">
              {{ agent.welcomeMessage || '暂无欢迎语' }}
            </div>
          </div>
          
          <div class="tags-row">
            <el-tag v-if="agent.knowledgeBaseId" type="success" size="small" effect="light">
              知识库: {{ agent.knowledgeBaseId }}
            </el-tag>
            <el-tag v-if="agent.toolIds?.length" type="warning" size="small" effect="light">
              MCP工具: {{ agent.toolIds.length }}个
            </el-tag>
          </div>
        </div>
        
        <div class="card-footer">
          <div class="footer-time">
            <el-icon><Clock /></el-icon>
            {{ agent.createTime }}
          </div>
          <el-button type="danger" size="small" plain @click="handleDelete(agent)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </div>
    </div>
    
    <div class="pagination-wrapper" v-if="pagination.total > 0">
      <el-pagination
        v-model:current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <el-empty v-if="!loading && filteredAgents.length === 0" description="暂无智能体数据">
      <template #image>
        <el-icon :size="60" color="#c0c4cc"><UserFilled /></el-icon>
      </template>
    </el-empty>
  </div>
</template>

<style scoped>
.agent-manage-view {
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

.stat-value.public {
  color: var(--color-success);
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.stat-divider {
  width: 1px;
  height: 16px;
  background: var(--color-border);
}

.search-input {
  width: 260px;
}

.agent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: var(--spacing-md);
}

.agent-card {
  display: flex;
  flex-direction: column;
  padding: var(--spacing-md);
  transition: all 0.3s ease;
  border-radius: 12px;
}

.agent-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-border-light);
}

.agent-avatar {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
}

.agent-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.agent-info {
  flex: 1;
  min-width: 0;
}

.agent-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-meta {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.id-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.id-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-primary);
}

.username {
  font-size: 12px;
  color: var(--color-text-muted);
}

.card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.info-value {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.prompt-section {
  background: var(--color-bg-secondary);
  border-radius: 8px;
  padding: var(--spacing-sm);
}

.prompt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.prompt-header:hover .prompt-label {
  color: var(--color-primary);
}

.prompt-label {
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

.prompt-content {
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

.prompt-content.expanded {
  -webkit-line-clamp: unset;
  display: block;
}

.tags-row {
  display: flex;
  gap: var(--spacing-xs);
  flex-wrap: wrap;
}

.card-footer {
  margin-top: var(--spacing-md);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-lg);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--color-border-light);
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }

  .agent-grid {
    grid-template-columns: 1fr;
  }
}
</style>
