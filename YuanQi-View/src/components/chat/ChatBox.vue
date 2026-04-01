<script setup>
import { ref, nextTick, watch, computed } from 'vue'
import { getMessageList, streamChat } from '@/api/message'
import { useSessionStore } from '@/stores/session'
import MessageItem from './MessageItem.vue'
import MessageInput from './MessageInput.vue'

const props = defineProps({
  sessionId: [String, Number],
  agentId: [Number, String],
  isAgentChat: Boolean
})

const sessionStore = useSessionStore()

const messages = ref([])
const loading = ref(false)
const messagesContainer = ref(null)
const streamingContent = ref('')
const isStreaming = ref(false)
let abortController = null

const currentSessionId = computed(() => {
  return props.sessionId || sessionStore.currentSession?.sessionId
})

const scrollToBottom = () => {
  nextTick(() => {
    setTimeout(() => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    }, 100)
  })
}

const loadMessages = async (sid) => {
  if (!sid) return
  
  if (abortController) {
    abortController()
    abortController = null
  }
  
  messages.value = []
  streamingContent.value = ''
  isStreaming.value = false
  loading.value = true
  
  try {
    const res = await getMessageList({ sessionId: sid, page: 1, size: 100 })
    if (res.code === 200) {
      messages.value = (res.data.records || []).map((msg, idx) => ({
        ...msg,
        _key: `${sid}-${idx}-${msg.id || Date.now()}`
      }))
      scrollToBottom()
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSend = async (data) => {
  const sid = currentSessionId.value
  if (!sid) return
  
  messages.value.push({
    role: 'user',
    content: data.message,
    imageUrl: data.imageUrl,
    documentUrl: data.documentUrl,
    createTime: new Date().toISOString(),
    _key: `user-${Date.now()}`
  })
  
  streamingContent.value = ''
  isStreaming.value = true
  scrollToBottom()
  
  const chatDTO = {
    sessionId: sid,
    message: data.message,
    imageUrl: data.imageUrl,
    documentUrl: data.documentUrl,
    knowledgeBaseId: props.isAgentChat ? null : data.knowledgeBaseId,
    enabledTools: props.isAgentChat ? [] : data.enabledTools,
    generateApp: props.isAgentChat ? false : data.generateApp,
    contextRounds: data.contextRounds !== undefined ? data.contextRounds : 10
  }
  
  abortController = streamChat(
    chatDTO,
    (text) => {
      streamingContent.value += text
      scrollToBottom()
    },
    () => {
      if (streamingContent.value) {
        messages.value.push({
          role: 'assistant',
          content: streamingContent.value,
          createTime: new Date().toISOString(),
          _key: `assistant-${Date.now()}`
        })
      }
      streamingContent.value = ''
      isStreaming.value = false
      scrollToBottom()
      sessionStore.fetchSessions(true)
    },
    (error) => {
      if (streamingContent.value) {
        messages.value.push({
          role: 'assistant',
          content: streamingContent.value,
          createTime: new Date().toISOString(),
          _key: `assistant-partial-${Date.now()}`
        })
      }
      messages.value.push({
        role: 'assistant',
        content: `错误: ${error}`,
        createTime: new Date().toISOString(),
        _key: `assistant-error-${Date.now()}`
      })
      streamingContent.value = ''
      isStreaming.value = false
      sessionStore.fetchSessions(true)
    }
  )
}

const handleStop = () => {
  if (abortController) {
    abortController()
    if (streamingContent.value) {
      messages.value.push({
        role: 'assistant',
        content: streamingContent.value,
        createTime: new Date().toISOString(),
        _key: `assistant-stopped-${Date.now()}`
      })
    }
    streamingContent.value = ''
    isStreaming.value = false
  }
}

watch(
  () => currentSessionId.value,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      loadMessages(newId)
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="chat-box">
    <div ref="messagesContainer" class="messages-container">
      <div v-if="loading" class="loading-state">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      
      <div v-else-if="messages.length === 0" class="empty-state">
        <el-icon class="empty-icon"><ChatDotRound /></el-icon>
        <p>开始新的对话吧</p>
      </div>
      
      <template v-else>
        <MessageItem
          v-for="msg in messages"
          :key="msg._key"
          :message="msg"
        />
        
        <MessageItem
          v-if="isStreaming"
          :key="'streaming'"
          :message="{ role: 'assistant', content: streamingContent }"
          :streaming="true"
        />
      </template>
    </div>
    
    <MessageInput
      :disabled="isStreaming"
      :is-agent-chat="isAgentChat"
      @send="handleSend"
      @stop="handleStop"
    />
  </div>
</template>

<style scoped>
.chat-box {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--color-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-light);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg);
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-text-muted);
  user-select: none;
}

.loading-icon,
.empty-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-md);
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
