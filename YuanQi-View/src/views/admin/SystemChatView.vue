<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { streamSystemChat, getSystemChatHistory, clearSystemChatHistory } from '@/api/systemChat'
import { renderMarkdown } from '@/utils/markdown'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChartCard from '@/components/common/ChartCard.vue'

const messages = ref([])
const inputMessage = ref('')
const imageUrl = ref('')
const isStreaming = ref(false)
const streamingContent = ref('')
const messagesContainer = ref(null)
const loading = ref(false)
const contextRounds = ref(10)
const showSettings = ref(false)
let abortController = null

const contextRoundsOptions = [
  { label: '不开启纪录', value: 0 },
  { label: '5轮', value: 5 },
  { label: '10轮', value: 10 },
  { label: '15轮', value: 15 },
  { label: '20轮', value: 20 }
]

const allSuggestions = [
  '今天有多少新用户？',
  '本周新增多少用户？',
  '本月新增多少用户？',
  '用户角色分布',
  '用户状态分布',
  '昨日日活多少？',
  '今日活跃用户数',
  '本周活跃用户',
  '本月活跃用户',
  '本周新增用户趋势图表',
  '本月新增用户趋势图表',
  '本周活跃用户趋势图图表',
  '本月活跃用户趋势图图表',
  '今天Token消耗多少？',
  '本周用量统计',
  '本月用量统计',
  '本周Token消耗趋势图表',
  '本月Token消耗趋势图表',
  '本周聊天次数趋势图表',
  '本月聊天次数趋势图表',
  '本月生成了多少图片？',
  '本月生成了多少视频？',
  '本周生成内容趋势图表',
  '本月生成内容趋势图表',
  '有多少个知识库？',
  '知识库状态分布',
  '知识库新增统计',
  '本周新增知识库趋势图表',
  '本月新增知识库趋势图表',
  '有多少个智能体？',
  '智能体公开/私有分布',
  '本周新增智能体趋势图表',
  '本月新增智能体趋势图表',
  '有多少个API Key？',
  'API Key状态分布',
  '本周新增API Key趋势图表',
  '有多少个中转配置？',
  '中转配置公开/私有分布',
  '本周新增中转配置趋势图表',
  '今天API调用多少次？',
  '本周API调用统计',
  '本周API调用趋势图图表',
  '本月API调用趋势图图表',
  '查看MCP工具状态',
  '启用联网搜索工具',
  '禁用天气查询工具',
  '启用天气查询工具',
  '启用一言工具',
  '启用点歌工具',
  '最近一周系统使用情况图表',
  '最近一个月系统概况图表',
  '各模块使用趋势对比图表'
]

const suggestions = ref([])

const randomSuggestions = () => {
  const shuffled = [...allSuggestions].sort(() => 0.5 - Math.random())
  suggestions.value = shuffled.slice(0, 7)
}

const scrollToBottom = () => {
  nextTick(() => {
    setTimeout(() => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    }, 100)
  })
}

const loadHistory = async () => {
  loading.value = true
  try {
    const res = await getSystemChatHistory()
    if (res.code === 200) {
      messages.value = (res.data || []).map((msg, idx) => ({
        ...msg,
        _key: `history-${idx}-${msg.id}`
      }))
      scrollToBottom()
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSend = async () => {
  const message = inputMessage.value.trim()
  const currentImageUrl = imageUrl.value
  if (!message && !currentImageUrl) {
    ElMessage.warning('请输入消息或上传图片')
    return
  }

  if (isStreaming.value) return

  messages.value.push({
    role: 'user',
    content: message,
    imageUrl: currentImageUrl || null,
    _key: `user-${Date.now()}`
  })

  inputMessage.value = ''
  imageUrl.value = ''
  streamingContent.value = ''
  isStreaming.value = true
  scrollToBottom()

  abortController = streamSystemChat(
    { message, imageUrl: currentImageUrl || null, contextRounds: contextRounds.value },
    (text) => {
      streamingContent.value += text
      scrollToBottom()
    },
    () => {
      if (streamingContent.value) {
        messages.value.push({
          role: 'assistant',
          content: streamingContent.value,
          _key: `assistant-${Date.now()}`
        })
      }
      streamingContent.value = ''
      isStreaming.value = false
      scrollToBottom()
    },
    (error) => {
      messages.value.push({
        role: 'assistant',
        content: `错误: ${error}`,
        _key: `assistant-error-${Date.now()}`
      })
      streamingContent.value = ''
      isStreaming.value = false
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
        _key: `assistant-stopped-${Date.now()}`
      })
    }
    streamingContent.value = ''
    isStreaming.value = false
  }
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确定要清除所有对话记录吗？', '提示', { type: 'warning' })
    await clearSystemChatHistory()
    messages.value = []
    ElMessage.success('已清除')
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleImageUpload = async (file) => {
  const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/jpg']
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    ElMessage.error('图片格式不支持，仅支持 jpg、jpeg、png 格式')
    return false
  }

  try {
    const { uploadFile } = await import('@/api/file')
    const res = await uploadFile(file)
    if (typeof res === 'string') {
      imageUrl.value = res
      ElMessage.success('图片上传成功')
    }
  } catch (error) {
    ElMessage.error('图片上传失败')
  }
  return false
}

const handleSuggestionClick = (text) => {
  inputMessage.value = text
}

const handleKeyDown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

const renderContent = (content) => {
  return renderMarkdown(content || '')
}

// 解析消息中的图表块，返回 [{type:'text',content}, {type:'chart',config}, ...]
const parseMessageContent = (content) => {
  if (!content) return []
  const parts = []
  const regex = /\[CHART\]([\s\S]*?)\[\/CHART\]/g
  let lastIndex = 0
  let match

  while ((match = regex.exec(content)) !== null) {
    if (match.index > lastIndex) {
      const text = content.substring(lastIndex, match.index).trim()
      if (text) parts.push({ type: 'text', content: text })
    }
    try {
      const config = JSON.parse(match[1].trim())
      parts.push({ type: 'chart', config })
    } catch (e) {
      parts.push({ type: 'text', content: match[0] })
    }
    lastIndex = match.index + match[0].length
  }

  if (lastIndex < content.length) {
    const text = content.substring(lastIndex).trim()
    if (text) parts.push({ type: 'text', content: text })
  }

  return parts.length > 0 ? parts : [{ type: 'text', content }]
}

// 流式输出时隐藏图表标记
const renderStreamingContent = (content) => {
  const cleaned = (content || '').replace(/\[CHART\][\s\S]*?(?:\[\/CHART\]|$)/g, '')
  return renderMarkdown(cleaned)
}

onMounted(() => {
  loadHistory()
  randomSuggestions()
})
</script>

<template>
  <div class="system-chat-view">
    <!-- 顶部标题栏 -->
    <div class="chat-header card">
      <div class="header-left">
        <el-avatar :size="40" class="avatar-ai" :src="'/logo.png'" />
        <div class="header-info">
          <h3>系统问答</h3>
          <span class="header-desc">通过自然语言查询系统数据</span>
        </div>
      </div>
      <div class="header-actions">
        <el-tooltip content="对话设置" placement="bottom">
          <el-button text @click="showSettings = !showSettings">
            <el-icon><Setting /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="清除对话" placement="bottom">
          <el-button text @click="handleClear">
            <el-icon><Delete /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- 设置面板 -->
    <div v-if="showSettings" class="settings-panel card">
      <div class="setting-row">
        <span class="setting-label">上下文轮数:</span>
        <el-radio-group v-model="contextRounds" size="small">
          <el-radio-button
            v-for="opt in contextRoundsOptions"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 消息区域 -->
    <div class="chat-box card">
      <div ref="messagesContainer" class="messages-container">
        <div v-if="loading" class="loading-state">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <div v-else-if="messages.length === 0" class="empty-state">
          <el-icon class="empty-icon"><DataAnalysis /></el-icon>
          <p>开始提问吧</p>
          <div class="suggestions">
            <el-tag
              v-for="(item, idx) in suggestions"
              :key="idx"
              class="suggestion-tag"
              @click="handleSuggestionClick(item)"
            >
              {{ item }}
            </el-tag>
          </div>
        </div>

        <template v-else>
          <div
            v-for="msg in messages"
            :key="msg._key"
            class="message-item"
            :class="{ user: msg.role === 'user' }"
          >
            <div class="message-avatar">
              <el-avatar v-if="msg.role === 'user'" :size="36" class="avatar-user">
                管
              </el-avatar>
              <el-avatar v-else :size="36" class="avatar-ai" :src="'/logo.png'" />
            </div>

            <div class="message-content">
              <div v-if="msg.imageUrl" class="message-image">
                <el-image :src="msg.imageUrl" fit="contain" :preview-src-list="[msg.imageUrl]" />
              </div>
              <template v-for="(part, pIdx) in parseMessageContent(msg.content)" :key="pIdx">
                <div v-if="part.type === 'text'" class="message-text" v-html="renderContent(part.content)" />
                <ChartCard v-else-if="part.type === 'chart'" :option="part.config" />
              </template>
            </div>
          </div>

          <div v-if="isStreaming" class="message-item assistant">
            <div class="message-avatar">
              <el-avatar :size="36" class="avatar-ai" :src="'/logo.png'" />
            </div>
            <div class="message-content">
              <div class="message-text streaming" v-html="renderStreamingContent(streamingContent)" />
              <div v-if="!streamingContent" class="streaming-indicator">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
            </div>
          </div>

        </template>
      </div>

      <!-- 输入区域 -->
      <div class="message-input">
        <!-- 快捷提问 - 在输入框上方 -->
        <div v-if="!isStreaming" class="suggestions-above">
          <div class="suggestions-header">
            <span class="suggestions-label">快捷提问</span>
            <el-button text size="small" @click="randomSuggestions">
              <el-icon><Refresh /></el-icon>
              换一批
            </el-button>
          </div>
          <div class="suggestions-list">
            <el-tag
              v-for="(item, idx) in suggestions"
              :key="idx"
              class="suggestion-tag"
              @click="handleSuggestionClick(item)"
            >
              {{ item }}
            </el-tag>
          </div>
        </div>
        <div v-if="imageUrl" class="preview-item">
          <el-image :src="imageUrl" fit="cover" class="preview-image" />
          <el-button
            type="danger"
            size="small"
            circle
            class="remove-btn"
            @click="imageUrl = ''"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <div class="input-row">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 6 }"
            placeholder="输入问题，如：今天有多少新用户？"
            :disabled="isStreaming"
            @keydown="handleKeyDown"
          />
        </div>

        <div class="toolbar">
          <div class="toolbar-left">
            <el-upload
              :show-file-list="false"
              accept=".jpg,.jpeg,.png"
              :before-upload="handleImageUpload"
            >
              <el-tooltip content="上传图片 (jpg/png/jpeg)" placement="top">
                <el-button text>
                  <el-icon><Picture /></el-icon>
                </el-button>
              </el-tooltip>
            </el-upload>
          </div>

          <div class="toolbar-right">
            <el-button
              v-if="isStreaming"
              type="danger"
              @click="handleStop"
            >
              <el-icon><VideoPause /></el-icon>
              停止
            </el-button>
            <el-button
              v-else
              type="primary"
              @click="handleSend"
              :disabled="!inputMessage.trim() && !imageUrl"
            >
              <el-icon><Promotion /></el-icon>
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.system-chat-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

/* 顶部标题栏 */
.chat-header {
  padding: var(--spacing-md);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.header-info h3 {
  font-size: var(--font-size-lg);
  font-weight: 500;
  margin-bottom: var(--spacing-xs);
}

.header-desc {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.header-actions {
  display: flex;
  gap: var(--spacing-xs);
}

/* 设置面板 */
.settings-panel {
  padding: var(--spacing-md);
  flex-shrink: 0;
}

.setting-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.setting-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* 聊天区域 */
.chat-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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

.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-lg);
  max-width: 600px;
  justify-content: center;
}

.suggestion-tag {
  cursor: pointer;
  transition: all var(--transition-fast);
}

.suggestion-tag:hover {
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  border-color: var(--color-primary);
}

/* 输入框上方推荐问题 */
.suggestions-above {
  padding: var(--spacing-sm) 0;
  margin-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-border-light);
}

.suggestions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.suggestions-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.suggestions-above .suggestions-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
  justify-content: flex-start;
}

/* 消息样式 */
.message-item {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  user-select: none;
}

.avatar-user {
  background: var(--color-primary);
  color: var(--color-white);
}

.avatar-ai {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: var(--color-white);
}

.message-content {
  max-width: 70%;
  padding: var(--spacing-md);
  border-radius: var(--radius-lg);
  background: var(--color-white);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-light);
}

.message-item.user .message-content {
  background: linear-gradient(135deg, var(--color-primary-light) 0%, #e8d09f 100%);
  color: #111827;
  border: none;
  box-shadow: 0 4px 10px rgba(196, 159, 87, 0.15);
}

.message-image {
  margin-bottom: var(--spacing-sm);
}

.message-image :deep(.el-image) {
  max-width: 300px;
  max-height: 300px;
  border-radius: var(--radius-md);
}

.message-text {
  line-height: 1.6;
  word-break: break-word;
}

.message-text.streaming::after {
  content: '▌';
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.message-text :deep(p) {
  margin: var(--spacing-sm) 0;
}

.message-text :deep(ul),
.message-text :deep(ol) {
  padding-left: var(--spacing-lg);
  margin: var(--spacing-sm) 0;
}

.message-text :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  overflow-x: auto;
  margin: var(--spacing-sm) 0;
}

.message-text :deep(code:not([class])) {
  font-family: 'Fira Code', monospace;
  font-size: var(--font-size-sm);
  background: var(--color-bg-tertiary);
  padding: 2px 6px;
  border-radius: 4px;
}

.streaming-indicator {
  display: flex;
  gap: 4px;
  margin-top: var(--spacing-sm);
}

.streaming-indicator .dot {
  width: 6px;
  height: 6px;
  background: var(--color-primary);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.streaming-indicator .dot:nth-child(1) { animation-delay: -0.32s; }
.streaming-indicator .dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* 输入区域 */
.message-input {
  border-top: 1px solid var(--color-border-light);
  padding: var(--spacing-md);
  background: var(--color-white);
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
}

.preview-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-sm);
  position: relative;
  width: fit-content;
}

.preview-image {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-sm);
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
}

.input-row {
  margin-bottom: var(--spacing-sm);
}

.input-row :deep(.el-textarea__inner) {
  border: none;
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  padding: var(--spacing-sm) var(--spacing-md);
  resize: none;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  gap: var(--spacing-xs);
}

.toolbar-right {
  display: flex;
  gap: var(--spacing-sm);
}
</style>
