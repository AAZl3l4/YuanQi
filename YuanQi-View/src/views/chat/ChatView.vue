<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import { getAgent } from '@/api/agent'
import ChatBox from '@/components/chat/ChatBox.vue'

const route = useRoute()
const sessionStore = useSessionStore()

const agent = ref(null)
const loading = ref(false)

const sessionId = computed(() => {
  return route.params.sessionId || sessionStore.currentSession?.sessionId
})

const currentSession = computed(() => {
  return sessionStore.sessions.find(s => s.sessionId === sessionId.value) || sessionStore.currentSession
})

const isAgentChat = computed(() => {
  return !!currentSession.value?.agentId
})

const agentId = computed(() => {
  return currentSession.value?.agentId || null
})

const loadAgentInfo = async (aid) => {
  if (!aid) {
    agent.value = null
    return
  }
  
  loading.value = true
  try {
    const res = await getAgent(aid)
    if (res.code === 200) {
      agent.value = res.data
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.sessionId,
  (newId) => {
    if (newId) {
      const session = sessionStore.sessions.find(s => s.sessionId === newId)
      if (session) {
        sessionStore.setCurrentSession(session)
        if (session.agentId) {
          loadAgentInfo(session.agentId)
        } else {
          agent.value = null
        }
      }
    }
  },
  { immediate: true }
)

watch(
  () => currentSession.value?.agentId,
  (newAgentId) => {
    if (newAgentId) {
      loadAgentInfo(newAgentId)
    } else {
      agent.value = null
    }
  },
  { immediate: true }
)

// 阻止页面默认的拖拽行为，防止文件被浏览器打开
const preventDefaultDrag = (e) => {
  e.preventDefault()
}

onMounted(() => {
  document.addEventListener('dragover', preventDefaultDrag)
  document.addEventListener('drop', preventDefaultDrag)
})

onUnmounted(() => {
  document.removeEventListener('dragover', preventDefaultDrag)
  document.removeEventListener('drop', preventDefaultDrag)
})
</script>

<template>
  <div class="chat-view">
    <div v-if="isAgentChat && agent" class="agent-info card">
      <div class="agent-header">
        <el-avatar :size="48" :src="agent.avatar">
          {{ agent.name?.charAt(0) }}
        </el-avatar>
        <div class="agent-meta">
          <h3>{{ agent.name }}</h3>
          <p>{{ agent.description }}</p>
        </div>
      </div>
      <div v-if="agent.welcomeMessage" class="welcome-message">
        {{ agent.welcomeMessage }}
      </div>
    </div>
    
    <ChatBox 
      :session-id="sessionId"
      :agent-id="agentId"
      :is-agent-chat="isAgentChat"
    />
  </div>
</template>

<style scoped>
.chat-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.agent-info {
  padding: var(--spacing-md);
  flex-shrink: 0;
}

.agent-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.agent-meta h3 {
  font-size: var(--font-size-lg);
  font-weight: 500;
  margin-bottom: var(--spacing-xs);
}

.agent-meta p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.welcome-message {
  margin-top: var(--spacing-md);
  padding: var(--spacing-md);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}
</style>
