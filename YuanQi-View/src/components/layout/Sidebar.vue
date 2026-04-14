<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSessionStore } from '@/stores/session'
import { deleteSession, updateSessionTitle } from '@/api/session'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  collapsed: Boolean
})

const emit = defineEmits(['toggle'])

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const sessionStore = useSessionStore()

const adminMenuOpen = ref(false)

watch(
  () => route.path,
  (newPath) => {
    if (newPath.startsWith('/admin')) {
      adminMenuOpen.value = true
    }
  },
  { immediate: true }
)

const menuItems = computed(() => {
  const items = [
    { path: '/agent', icon: 'UserFilled', label: '智能体' },
    { path: '/knowledge', icon: 'Collection', label: '知识库' },
    { path: '/content', icon: 'Picture', label: '生成中心' },
    { path: '/usage', icon: 'DataLine', label: '用量统计' },
    { path: '/relay-config', icon: 'Connection', label: '中转配置' },
    { path: '/api-key', icon: 'Key', label: 'API Key' }
  ]
  
  return items
})

const adminItems = computed(() => {
  if (!userStore.isAdmin) return []
  return [
    { path: '/admin/user', icon: 'User', label: '用户管理' },
    { path: '/admin/mcp', icon: 'Tools', label: 'MCP工具' },
    { path: '/admin/agent', icon: 'UserFilled', label: '智能体管理' },
    { path: '/admin/knowledge', icon: 'Collection', label: '知识库管理' },
    { path: '/admin/relay-config', icon: 'Connection', label: '中转配置' },
    { path: '/admin/relay-logs', icon: 'DataLine', label: '调用记录' },
    { path: '/admin/content', icon: 'Picture', label: '生成记录' },
    { path: '/admin/usage', icon: 'DataLine', label: '用量统计' }
  ]
})

const isAdminRoute = computed(() => route.path.startsWith('/admin'))

const handleNewChat = async () => {
  const session = await sessionStore.createNewSession()
  if (session) {
    router.push(`/chat/${session.sessionId}`)
  }
}

const handleSelectSession = (session) => {
  sessionStore.setCurrentSession(session)
  router.push(`/chat/${session.sessionId}`)
}

const handleRenameSession = async (session, event) => {
  event.stopPropagation()
  try {
    const { value } = await ElMessageBox.prompt('请输入新标题', '重命名', {
      inputValue: session.title,
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    if (value) {
      await updateSessionTitle(session.sessionId, { sessionId: session.sessionId, title: value })
      session.title = value
      ElMessage.success('修改成功')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleDeleteSession = async (session, event) => {
  event.stopPropagation()
  try {
    await ElMessageBox.confirm('确定删除该会话吗？', '提示', { type: 'warning' })
    await deleteSession(session.sessionId)
    sessionStore.sessions = sessionStore.sessions.filter(s => s.sessionId !== session.sessionId)
    if (sessionStore.currentSession?.sessionId === session.sessionId) {
      sessionStore.currentSession = null
    }
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const toggleAdminMenu = () => {
  adminMenuOpen.value = !adminMenuOpen.value
}
</script>

<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="sidebar-header">
      <div class="logo">
        <img src="/logo.png" alt="元启" class="logo-img" />
        <span v-if="!collapsed" class="logo-text">元启</span>
      </div>
      <el-button
        v-if="!collapsed"
        type="primary"
        class="new-chat-btn"
        @click="handleNewChat"
      >
        <el-icon><Plus /></el-icon>
        新建对话
      </el-button>
    </div>

    <div class="sidebar-scroll">
      <div v-if="!collapsed" class="session-list">
        <div class="session-header">
          <span>最近会话</span>
          <router-link to="/history" class="more-link">
            更多 <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>
        <div class="sessions">
          <div
            v-for="session in sessionStore.sessions.slice(0, 5)"
            :key="session.sessionId"
            class="session-item"
            :class="{ active: route.params.sessionId === session.sessionId, 'is-agent': session.agentId }"
            @click="handleSelectSession(session)"
          >
            <el-icon class="session-icon">
              <UserFilled v-if="session.agentId" />
              <ChatDotRound v-else />
            </el-icon>
            <span class="session-title">{{ session.title || '新对话' }}</span>
            <el-tag v-if="session.agentId" size="small" type="primary" class="agent-tag">智能体</el-tag>
            <div class="session-actions">
              <el-button
                text
                size="small"
                class="action-btn"
                @click="handleRenameSession(session, $event)"
              >
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button
                text
                size="small"
                class="action-btn delete"
                @click="handleDeleteSession(session, $event)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: route.path === item.path }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
        </router-link>
        
        <template v-if="userStore.isAdmin && adminItems.length > 0">
          <div class="nav-divider"></div>
          <div
            v-if="!collapsed"
            class="nav-item admin-header"
            :class="{ active: isAdminRoute }"
            @click="toggleAdminMenu"
          >
            <el-icon><Setting /></el-icon>
            <span class="nav-label">管理后台</span>
            <el-icon class="arrow" :class="{ open: adminMenuOpen }">
              <ArrowDown />
            </el-icon>
          </div>
          <div
            v-else
            class="nav-item admin-header-collapsed"
            :class="{ active: isAdminRoute }"
          >
            <el-icon><Setting /></el-icon>
          </div>
          
          <template v-if="!collapsed && (adminMenuOpen || isAdminRoute)">
            <router-link
              v-for="item in adminItems"
              :key="item.path"
              :to="item.path"
              class="nav-item admin-item"
              :class="{ active: route.path === item.path }"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <span class="nav-label">{{ item.label }}</span>
            </router-link>
          </template>
        </template>
      </nav>
    </div>

    <div class="sidebar-footer">
      <el-button
        text
        class="toggle-btn"
        @click="emit('toggle')"
      >
        <el-icon>
          <Fold v-if="!collapsed" />
          <Expand v-else />
        </el-icon>
      </el-button>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--color-bg-secondary);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid var(--color-glass-border);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  transition: width var(--transition-normal);
  z-index: 100;
  box-shadow: 1px 0 10px rgba(0, 0, 0, 0.02);
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0;
}

.sidebar-header {
  padding: var(--spacing-md);
  border-bottom: 1px solid var(--color-glass-border);
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
}

.logo-img {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-sm);
}

.logo-text {
  font-size: var(--font-size-xl);
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--color-primary);
  user-select: none;
}

.new-chat-btn {
  width: 100%;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  border: none;
  border-radius: var(--radius-md);
  box-shadow: 0 4px 10px rgba(196, 159, 87, 0.25);
  transition: all var(--transition-fast);
}

.new-chat-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(196, 159, 87, 0.35);
}

.session-list {
  flex: 0 0 auto;
  padding: var(--spacing-sm);
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-md);
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  font-weight: 500;
  user-select: none;
}

.more-link {
  display: flex;
  align-items: center;
  gap: 2px;
  color: var(--color-text-muted);
  text-decoration: none;
  cursor: pointer;
  transition: color var(--transition-fast);
}

.more-link:hover {
  color: var(--color-primary);
}

.sessions {
  display: flex;
  flex-direction: column;
}

.session-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 10px var(--spacing-md);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
  margin-bottom: 2px;
}

.session-item:hover {
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 2px 4px rgba(0,0,0,0.01);
}

.session-item.active {
  background: var(--color-white);
  box-shadow: var(--shadow-sm);
  font-weight: 500;
}

.agent-tag {
  margin-left: auto;
  margin-right: var(--spacing-sm);
  font-size: 10px;
  padding: 0 6px;
  height: 20px;
  line-height: 18px;
  border-radius: 10px;
}

.session-icon {
  flex-shrink: 0;
  color: var(--color-text-muted);
  transition: color var(--transition-fast);
}

.session-item.active .session-icon {
  color: var(--color-primary);
}

.session-title {
  flex: 1;
  font-size: var(--font-size-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  user-select: none;
}

.session-actions {
  display: none;
  gap: 2px;
}

.session-item:hover .session-actions {
  display: flex;
}

.action-btn {
  padding: 4px !important;
  color: var(--color-text-muted) !important;
}

.action-btn:hover {
  color: var(--color-primary) !important;
  background: transparent !important;
}

.action-btn.delete:hover {
  color: var(--color-danger) !important;
}

.sidebar-nav {
  padding: var(--spacing-sm);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  border-top: 1px solid var(--color-glass-border);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 10px var(--spacing-md);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  cursor: pointer;
  text-decoration: none;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.5);
  color: var(--color-text);
}

.nav-item.active {
  background: var(--color-white);
  color: var(--color-primary-dark);
  box-shadow: var(--shadow-sm);
  font-weight: 500;
}

.nav-divider {
  height: 1px;
  background: var(--color-glass-border);
  margin: var(--spacing-sm) 0;
}

.admin-header {
  font-weight: 500;
}

.admin-header .arrow {
  margin-left: auto;
  transition: transform var(--transition-fast);
}

.admin-header .arrow.open {
  transform: rotate(180deg);
}

.admin-header-collapsed {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-sm);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  cursor: pointer;
}

.admin-header-collapsed:hover {
  background: rgba(255, 255, 255, 0.5);
}

.admin-header-collapsed.active {
  background: var(--color-white);
  color: var(--color-primary-dark);
  box-shadow: var(--shadow-sm);
}

.admin-item {
  padding-left: var(--spacing-xl);
  font-size: var(--font-size-sm);
}

.nav-label {
  font-size: var(--font-size-sm);
  user-select: none;
}

.sidebar-footer {
  padding: var(--spacing-sm);
  border-top: 1px solid var(--color-glass-border);
}

.toggle-btn {
  width: 100%;
  justify-content: center;
  border-radius: var(--radius-md);
}
.toggle-btn:hover {
  background: rgba(255, 255, 255, 0.5);
}
</style>
