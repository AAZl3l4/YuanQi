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

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该智能体吗？', '提示', { type: 'warning' })
    const res = await deleteAgent(id)
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
  loadKnowledgeBases()
  loadMcpTools()
})
</script>

<template>
  <div class="agent-square-view page-container">
    <div class="page-header">
      <h2 class="page-title">智能体广场</h2>
      <div class="header-actions">
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
    
    <el-row :gutter="16" v-loading="loading">
      <el-col v-for="agent in agents" :key="agent.id" :span="6">
        <div class="agent-card card">
          <div class="agent-avatar">
            <el-avatar :size="64" :src="agent.avatar">
              {{ agent.name?.charAt(0) }}
            </el-avatar>
          </div>
          <h3 class="agent-name">{{ agent.name }}</h3>
          <p class="agent-desc">{{ agent.description || '暂无描述' }}</p>
          <div v-if="agent.username" class="agent-owner">
            创建者: {{ agent.username }}
          </div>
          <div class="agent-tags">
            <el-tag v-if="agent.knowledgeBaseId" type="success" size="small">知识库</el-tag>
            <el-tag v-if="agent.toolIds?.length" type="warning" size="small">MCP工具</el-tag>
          </div>
          <div class="agent-actions">
            <el-button type="primary" size="small" @click="handleUse(agent)">
              使用
            </el-button>
            <el-button v-if="agent.userId === currentUserId" text type="primary" size="small" @click="openEditDialog(agent.id)">
              编辑
            </el-button>
            <el-button v-if="agent.userId === currentUserId" text type="danger" size="small" @click="handleDelete(agent.id)">
              删除
            </el-button>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <el-empty v-if="!loading && agents.length === 0" description="暂无智能体" />
    
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
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.page-title {
  user-select: none;
}

.header-actions {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.agent-card {
  padding: var(--spacing-lg);
  text-align: center;
  margin-bottom: var(--spacing-md);
}

.agent-avatar {
  margin-bottom: var(--spacing-md);
}

.agent-name {
  font-size: var(--font-size-lg);
  font-weight: 500;
  margin-bottom: var(--spacing-sm);
  user-select: none;
}

.agent-desc {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  margin-bottom: var(--spacing-sm);
  min-height: 40px;
}

.agent-owner {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-sm);
  user-select: none;
}

.agent-tags {
  display: flex;
  justify-content: center;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-md);
}

.agent-actions {
  display: flex;
  justify-content: center;
  gap: var(--spacing-xs);
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
</style>
