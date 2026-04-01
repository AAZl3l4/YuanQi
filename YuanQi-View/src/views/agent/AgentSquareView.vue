<script setup>
import { ref, onMounted, computed } from 'vue'
import { getAgentList, deleteAgent, createAgent, updateAgent, getAgent } from '@/api/agent'
import { getMyKnowledgeList } from '@/api/knowledge'
import { getEnabledTools } from '@/api/mcp'
import { uploadFile } from '@/api/file'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const currentUserId = computed(() => userStore.user?.id)
const agents = ref([])
const loading = ref(false)
const onlyMine = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const isEdit = ref(false)
const knowledgeBases = ref([])
const mcpTools = ref([])
const avatarLoading = ref(false)
const searchText = ref('')

const form = ref({
  id: null,
  name: '',
  description: '',
  avatar: '',
  systemPrompt: '',
  welcomeMessage: '',
  knowledgeBaseId: null,
  toolIds: [],
  isPublic: 0
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  systemPrompt: [{ required: true, message: '请输入系统提示词', trigger: 'blur' }]
}

const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/jpg']

const filteredAgents = computed(() => {
  if (!searchText.value) return agents.value
  const keyword = searchText.value.toLowerCase()
  return agents.value.filter(a => 
    a.name?.toLowerCase().includes(keyword) ||
    a.description?.toLowerCase().includes(keyword)
  )
})

const handleAvatarUpload = async (file) => {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    ElMessage.warning('图片格式不支持，仅支持 jpg、jpeg、png 格式')
    return false
  }
  
  avatarLoading.value = true
  try {
    const res = await uploadFile(file)
    if (typeof res === 'string') {
      form.value.avatar = res
      ElMessage.success('上传成功')
    }
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    avatarLoading.value = false
  }
  return false
}

const loadAgents = async () => {
  loading.value = true
  try {
    const res = await getAgentList({ page: 1, size: 100, onlyMine: onlyMine.value })
    if (res.code === 200) {
      agents.value = res.data.records || []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadKnowledgeBases = async () => {
  try {
    const res = await getMyKnowledgeList({ page: 1, size: 100 })
    if (res.code === 200) {
      knowledgeBases.value = res.data.records || []
    }
  } catch (error) {
    console.error(error)
  }
}

const loadMcpTools = async () => {
  try {
    const res = await getEnabledTools()
    if (res.code === 200) {
      mcpTools.value = res.data || []
    }
  } catch (error) {
    console.error(error)
  }
}

const handleUse = (agent) => {
  router.push(`/agent/${agent.id}`)
}

const openCreateDialog = () => {
  isEdit.value = false
  form.value = {
    id: null,
    name: '',
    description: '',
    avatar: '',
    systemPrompt: '',
    welcomeMessage: '',
    knowledgeBaseId: null,
    toolIds: [],
    isPublic: 0
  }
  dialogVisible.value = true
}

const openEditDialog = async (id) => {
  try {
    const res = await getAgent(id)
    if (res.code === 200) {
      isEdit.value = true
      form.value = {
        id: res.data.id,
        name: res.data.name,
        description: res.data.description || '',
        avatar: res.data.avatar || '',
        systemPrompt: res.data.systemPrompt,
        welcomeMessage: res.data.welcomeMessage || '',
        knowledgeBaseId: res.data.knowledgeBaseId,
        toolIds: res.data.toolIds || [],
        isPublic: res.data.isPublic || 0
      }
      dialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取智能体信息失败')
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  try {
    let res
    if (isEdit.value) {
      res = await updateAgent(form.value)
    } else {
      res = await createAgent(form.value)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadAgents()
    }
  } catch (error) {
    console.error(error)
  }
}

const handleDelete = async (agent) => {
  try {
    await ElMessageBox.confirm(
      `确定删除「${agent.name}」吗？此操作不可恢复。`, 
      '删除确认', 
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    const res = await deleteAgent(agent.id)
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
  loadKnowledgeBases()
  loadMcpTools()
})
</script>

<template>
  <div class="agent-square-view page-container">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon class="title-icon"><UserFilled /></el-icon>
          智能体广场
        </h2>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-value">{{ agents.length }}</span>
            <span class="stat-label">{{ onlyMine ? '我的智能体' : '智能体' }}</span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <el-input
          v-model="searchText"
          placeholder="搜索智能体..."
          prefix-icon="Search"
          clearable
          class="search-input"
        />
        <el-radio-group v-model="onlyMine" @change="loadAgents" size="small">
          <el-radio-button :value="false">全部</el-radio-button>
          <el-radio-button :value="true">我的</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建智能体
        </el-button>
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
              <el-tag :type="agent.isPublic === 1 ? 'success' : 'info'" size="small">
                {{ agent.isPublic === 1 ? '公开' : '私有' }}
              </el-tag>
              <span v-if="!onlyMine && agent.username" class="creator">
                {{ agent.username }}
              </span>
            </div>
          </div>
        </div>
        
        <div class="card-body">
          <p class="agent-desc">{{ agent.description || '暂无描述' }}</p>
          <div class="agent-tags">
            <el-tag v-if="agent.knowledgeBaseId" type="success" size="small" effect="light">
              <el-icon><Collection /></el-icon>
              知识库
            </el-tag>
            <el-tag v-if="agent.toolIds?.length" type="warning" size="small" effect="light">
              <el-icon><Tools /></el-icon>
              MCP工具
            </el-tag>
          </div>
        </div>
        
        <div class="card-footer">
          <div class="footer-time">
            <el-icon><Clock /></el-icon>
            {{ agent.createTime }}
          </div>
          <div class="footer-actions">
            <el-button type="primary" size="small" @click="handleUse(agent)">
              使用
            </el-button>
            <template v-if="agent.userId === currentUserId">
              <el-button text type="primary" size="small" @click="openEditDialog(agent.id)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button text type="danger" size="small" @click="handleDelete(agent)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </div>
        </div>
      </div>
    </div>
    
    <el-empty v-if="!loading && filteredAgents.length === 0" description="暂无智能体">
      <template #image>
        <el-icon :size="60" color="#c0c4cc"><UserFilled /></el-icon>
      </template>
    </el-empty>
    
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑智能体' : '新建智能体'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入智能体名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <div class="avatar-upload">
            <el-avatar :size="64" :src="form.avatar" class="avatar-preview">
              {{ form.name?.charAt(0) || 'A' }}
            </el-avatar>
            <el-upload
              :show-file-list="false"
              :before-upload="handleAvatarUpload"
              accept=".jpg,.jpeg,.png"
            >
              <el-button :loading="avatarLoading" size="small">上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input
            v-model="form.systemPrompt"
            type="textarea"
            :rows="4"
            placeholder="请输入系统提示词，定义智能体的角色和行为"
          />
        </el-form-item>
        <el-form-item label="欢迎语" prop="welcomeMessage">
          <el-input v-model="form.welcomeMessage" type="textarea" placeholder="用户开始对话时显示的欢迎消息" />
        </el-form-item>
        <el-form-item label="关联知识库" prop="knowledgeBaseId">
          <el-select v-model="form.knowledgeBaseId" placeholder="选择知识库" clearable style="width: 100%">
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="MCP工具" prop="toolIds">
          <el-select v-model="form.toolIds" multiple placeholder="选择工具" clearable style="width: 100%">
            <el-option
              v-for="tool in mcpTools"
              :key="tool.id"
              :label="tool.description || tool.name"
              :value="tool.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="是否公开" prop="isPublic">
          <el-radio-group v-model="form.isPublic">
            <el-radio :value="0">私有</el-radio>
            <el-radio :value="1">公开</el-radio>
          </el-radio-group>
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
.agent-square-view {
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

.header-actions {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.search-input {
  width: 200px;
}

.agent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
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
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
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
  gap: var(--spacing-sm);
}

.creator {
  font-size: 12px;
  color: var(--color-text-muted);
}

.card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.agent-desc {
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 40px;
}

.agent-tags {
  display: flex;
  gap: var(--spacing-xs);
  flex-wrap: wrap;
}

.agent-tags .el-tag {
  display: flex;
  align-items: center;
  gap: 2px;
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

.footer-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.avatar-preview {
  background: var(--color-primary);
  color: var(--color-white);
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }
  
  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }
  
  .search-input {
    width: 100%;
  }
  
  .agent-grid {
    grid-template-columns: 1fr;
  }
}
</style>
