<script setup>
import { ref, onMounted, computed } from 'vue'
import { getUserList, deleteUser, getOnlineUsers, kickoutUser, sendEmailToUser, updateUser } from '@/api/user'
import { getKnowledgeList } from '@/api/knowledge'
import { getAdminAgentList } from '@/api/agent'
import { getContentList } from '@/api/content'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const onlineUsers = ref([])
const loading = ref(false)
const onlineDialogVisible = ref(false)
const emailDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const editDialogVisible = ref(false)
const currentUser = ref(null)
const userKnowledge = ref([])
const userAgents = ref([])
const userContent = ref([])
const activeTab = ref('info')

const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

const emailForm = ref({ userId: null, subject: '', content: '' })
const editForm = ref({ id: null, username: '', email: '', role: 'user', status: 1 })

const statusText = computed(() => {
  return (status) => status === 1 ? '正常' : '禁用'
})

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await getUserList({ page: pagination.value.page, size: pagination.value.size })
    if (res.code === 200) {
      users.value = res.data.records || []
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
  loadUsers()
}

const loadOnlineUsers = async () => {
  try {
    const res = await getOnlineUsers()
    if (res.code === 200) {
      onlineUsers.value = res.data || []
    }
  } catch (error) {
    console.error(error)
  }
}

const handleShowOnline = async () => {
  await loadOnlineUsers()
  onlineDialogVisible.value = true
}

const handleKickout = async (userId) => {
  try {
    await ElMessageBox.confirm('确定踢出该用户吗？', '提示', { type: 'warning' })
    const res = await kickoutUser(userId)
    if (res.code === 200) {
      ElMessage.success('已踢出')
      loadOnlineUsers()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleSendEmail = async () => {
  if (!emailForm.value.subject || !emailForm.value.content) {
    ElMessage.warning('请填写完整')
    return
  }
  try {
    const res = await sendEmailToUser(
      emailForm.value.userId,
      emailForm.value.subject,
      emailForm.value.content
    )
    if (res.code === 200) {
      ElMessage.success('发送成功')
      emailDialogVisible.value = false
      emailForm.value = { userId: null, subject: '', content: '' }
    }
  } catch (error) {
    console.error(error)
  }
}

const openEmailDialog = (userId) => {
  emailForm.value.userId = userId
  emailDialogVisible.value = true
}

const handleViewDetail = async (user) => {
  currentUser.value = user
  activeTab.value = 'info'
  detailDialogVisible.value = true
  
  try {
    const [kbRes, agentRes, contentRes] = await Promise.all([
      getKnowledgeList({ page: 1, size: 10, userId: user.id }),
      getAdminAgentList({ page: 1, size: 10, userId: user.id }),
      getContentList({ page: 1, size: 10, userId: user.id })
    ])
    userKnowledge.value = kbRes.code === 200 ? (kbRes.data.records || []) : []
    userAgents.value = agentRes.code === 200 ? (agentRes.data.records || []) : []
    userContent.value = contentRes.code === 200 ? (contentRes.data.records || []) : []
  } catch (error) {
    console.error(error)
  }
}

const openEditDialog = (user) => {
  editForm.value = {
    id: user.id,
    username: user.username,
    email: user.email,
    role: user.role || 'user',
    status: user.status ?? 1
  }
  editDialogVisible.value = true
}

const handleEditSubmit = async () => {
  try {
    const res = await updateUser(editForm.value.id, editForm.value)
    if (res.code === 200) {
      ElMessage.success('修改成功')
      editDialogVisible.value = false
      loadUsers()
    }
  } catch (error) {
    console.error(error)
  }
}

const handleToggleStatus = async (user) => {
  const newStatus = user.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'
  
  try {
    await ElMessageBox.confirm(`确定${action}该用户吗？`, '提示', { type: 'warning' })
    const res = await updateUser(user.id, { status: newStatus })
    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      loadUsers()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const formatTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="user-manage-view page-container">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon class="title-icon"><User /></el-icon>
          用户管理
        </h2>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-value">{{ pagination.total }}</span>
            <span class="stat-label">总用户</span>
          </div>
        </div>
      </div>
      <el-button type="primary" @click="handleShowOnline">
        <el-icon><User /></el-icon>
        在线用户
      </el-button>
    </div>
    
    <el-table :data="users" v-loading="loading" class="card">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="250">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="32" :src="row.avatar">
              {{ row.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <span>{{ row.username }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
            {{ row.role === 'admin' ? '管理员' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="180" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="handleViewDetail(row)">
            详情
          </el-button>
          <el-button text type="primary" size="small" @click="openEditDialog(row)">
            编辑
          </el-button>
          <el-button 
            :text="true" 
            :type="row.status === 1 ? 'warning' : 'success'" 
            size="small" 
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button text type="info" size="small" @click="openEmailDialog(row.id)">
            发邮件
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination-wrapper" v-if="pagination.total > 0">
      <el-pagination
        v-model:current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
    
    <el-dialog v-model="onlineDialogVisible" title="在线用户" width="600px" class="custom-dialog">
      <el-table :data="onlineUsers" v-if="onlineUsers.length > 0">
        <el-table-column prop="username" label="用户名" />
        <el-table-column label="登录时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button text type="danger" size="small" @click="handleKickout(row.id)">
              踢出
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无在线用户" />
    </el-dialog>
    
    <el-dialog v-model="emailDialogVisible" title="发送邮件" width="500px" class="custom-dialog">
      <el-form :model="emailForm" label-width="80px">
        <el-form-item label="主题">
          <el-input v-model="emailForm.subject" placeholder="请输入邮件主题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="emailForm.content" type="textarea" :rows="5" placeholder="请输入邮件内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="emailDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSendEmail">发送</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="500px" class="custom-dialog">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option value="user" label="普通用户" />
            <el-option value="admin" label="管理员" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option :value="1" label="正常" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>
    
    <el-dialog 
      v-model="detailDialogVisible" 
      :title="currentUser?.username + ' - 用户详情'" 
      width="800px"
      class="custom-dialog detail-dialog"
    >
      <div class="detail-content" v-if="currentUser">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="info">
            <div class="info-section">
              <div class="user-profile">
                <el-avatar :size="80" :src="currentUser.avatar" class="profile-avatar">
                  {{ currentUser.username?.charAt(0)?.toUpperCase() }}
                </el-avatar>
                <div class="profile-info">
                  <h3 class="profile-name">{{ currentUser.username }}</h3>
                  <p class="profile-email">{{ currentUser.email }}</p>
                  <div class="profile-tags">
                    <el-tag :type="currentUser.role === 'admin' ? 'danger' : 'info'">
                      {{ currentUser.role === 'admin' ? '管理员' : '普通用户' }}
                    </el-tag>
                    <el-tag :type="currentUser.status === 1 ? 'success' : 'danger'">
                      {{ currentUser.status === 1 ? '正常' : '禁用' }}
                    </el-tag>
                  </div>
                </div>
              </div>
              
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">用户ID</span>
                  <span class="info-value">{{ currentUser.id }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">注册时间</span>
                  <span class="info-value">{{ currentUser.createTime }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">更新时间</span>
                  <span class="info-value">{{ currentUser.updateTime }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">文字模型</span>
                  <span class="info-value">{{ currentUser.chatModel || '默认' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">图文模型</span>
                  <span class="info-value">{{ currentUser.chatVisionModel || '默认' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">生图模型</span>
                  <span class="info-value">{{ currentUser.imageModel || '默认' }}</span>
                </div>
              </div>
            </div>
          </el-tab-pane>
          
          <el-tab-pane name="knowledge">
            <template #label>
              <span>知识库 <el-badge :value="userKnowledge.length" :max="99" /></span>
            </template>
            <el-table :data="userKnowledge" size="small" v-if="userKnowledge.length > 0">
              <el-table-column prop="name" label="名称" />
              <el-table-column prop="chunkCount" label="分块数" width="100" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
                    {{ row.status === 1 ? '可用' : '处理中' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="180" />
            </el-table>
            <el-empty v-else description="暂无知识库" :image-size="60" />
          </el-tab-pane>
          
          <el-tab-pane name="agents">
            <template #label>
              <span>智能体 <el-badge :value="userAgents.length" :max="99" /></span>
            </template>
            <el-table :data="userAgents" size="small" v-if="userAgents.length > 0">
              <el-table-column prop="name" label="名称" />
              <el-table-column prop="isPublic" label="公开" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.isPublic ? 'success' : 'info'" size="small">
                    {{ row.isPublic ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="180" />
            </el-table>
            <el-empty v-else description="暂无智能体" :image-size="60" />
          </el-tab-pane>
          
          <el-tab-pane name="content">
            <template #label>
              <span>生成记录 <el-badge :value="userContent.length" :max="99" /></span>
            </template>
            <el-table :data="userContent" size="small" v-if="userContent.length > 0">
              <el-table-column prop="type" label="类型" width="80">
                <template #default="{ row }">
                  <el-tag size="small">{{ row.type === 'image' ? '图片' : '视频' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="prompt" label="提示词" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="180" />
            </el-table>
            <el-empty v-else description="暂无生成记录" :image-size="60" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-manage-view {
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
  color: var(--color-primary);
}

.stat-label {
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

.user-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.detail-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.detail-content {
  padding: var(--spacing-md);
}

.info-section {
  padding: var(--spacing-md) 0;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);
  background: linear-gradient(135deg, var(--color-primary-light) 0%, var(--color-bg-secondary) 100%);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-lg);
}

.profile-avatar {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  color: var(--color-white);
  font-size: var(--font-size-2xl);
  font-weight: 600;
}

.profile-info {
  flex: 1;
}

.profile-name {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 var(--spacing-xs) 0;
}

.profile-email {
  color: var(--color-text-secondary);
  margin: 0 0 var(--spacing-sm) 0;
}

.profile-tags {
  display: flex;
  gap: var(--spacing-sm);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-md);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  padding: var(--spacing-md);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
}

.info-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.info-value {
  font-size: var(--font-size-sm);
  color: var(--color-text);
  font-weight: 500;
}

:deep(.el-tabs__header) {
  margin-bottom: var(--spacing-md);
}

:deep(.el-badge__content) {
  top: 8px;
}
</style>
