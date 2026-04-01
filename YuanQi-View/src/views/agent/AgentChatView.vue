<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAgent } from '@/api/agent'
import { createSession } from '@/api/session'
import { useSessionStore } from '@/stores/session'
import ChatBox from '@/components/chat/ChatBox.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()

const agent = ref(null)
const loading = ref(true)
const sessionId = ref(null)
const initialized = ref(false)

const isAgentChat = computed(() => !!route.params.id)

const loadAgent = async () => {
  const agentId = route.params.id
  if (!agentId) {
    loading.value = false
    return
  }
  
  if (initialized.value) {
    loading.value = false
    return
  }
  
  initialized.value = true
  loading.value = true
  
  try {
    const agentRes = await getAgent(agentId)
    if (agentRes.code === 200) {
      agent.value = agentRes.data
      
      const sessionRes = await createSession({ agentId: parseInt(agentId) })
      if (sessionRes.code === 200 && sessionRes.data) {
        sessionId.value = sessionRes.data.sessionId
        sessionStore.fetchSessions(true)
      } else {
        ElMessage.error('创建会话失败')
      }
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载智能体失败')
    router.push('/agent')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAgent()
})
</script>

<template>
  <div class="agent-chat-view" v-loading="loading">
    <div v-if="agent" class="agent-info card">
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
      v-if="!loading && sessionId" 
      :session-id="sessionId"
      :agent-id="agent?.id"
      :is-agent-chat="isAgentChat"
    />
    
    <div v-else-if="!loading && !sessionId" class="error-state">
      <el-empty description="会话创建失败，请重试">
        <el-button type="primary" @click="initialized = false; loadAgent()">重试</el-button>
      </el-empty>
    </div>
  </div>
</template>

<style scoped>
.agent-chat-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.agent-info {
  padding: var(--spacing-md);
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

.error-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
