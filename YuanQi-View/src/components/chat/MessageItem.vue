<script setup>
import { computed, inject, onMounted, onUnmounted, ref } from 'vue'
import { renderMarkdown } from '@/utils/markdown'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  message: {
    type: Object,
    required: true
  },
  streaming: Boolean
})

const userStore = useUserStore()

const isUser = computed(() => props.message.role === 'user')

// 音乐链接正则：匹配QQ音乐等音频链接（支持<a>标签包裹的链接）
const musicRegex = /<a[^>]*href="(https?:\/\/[^"]+\.(mp3|m4a|flac|wav)(\?[^"]*)?)"[^>]*>[^<]*<\/a>|(https?:\/\/[^\s`]+\.(mp3|m4a|flac|wav)(\?[^\s`]*)?)/g

const renderedContent = computed(() => {
  let html = renderMarkdown(props.message.content || '')
  // 将音频链接替换为播放器HTML
  html = html.replace(musicRegex, (match, urlFromAnchor, ext1, query1, urlPlain, ext2, query2) => {
    const url = urlFromAnchor || urlPlain
    if (!url) return match
    return `<audio controls class="music-player" src="${url}" style="width:100%;margin:8px 0;"></audio>`
  })
  return html
})

const userAvatar = computed(() => userStore.user?.avatar)
const userName = computed(() => userStore.user?.username || 'U')

const previewStore = inject('previewStore')

const handlePreview = (code) => {
  if (previewStore) {
    previewStore.showPreview(code)
  }
}

const handleCopy = async (code) => {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('已复制到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败')
  }
}

onMounted(() => {
  window.copyCode = (btn) => {
    const wrapper = btn.closest('.code-block-wrapper')
    const rawDiv = wrapper?.querySelector('.code-raw')
    if (rawDiv) {
      const code = rawDiv.textContent
      handleCopy(code)
    }
  }
  
  window.previewHtmlCode = (btn) => {
    const wrapper = btn.closest('.code-block-wrapper')
    const rawDiv = wrapper?.querySelector('.code-raw')
    if (rawDiv) {
      const code = rawDiv.textContent
      handlePreview(code)
    }
  }
})

onUnmounted(() => {
  // 不在销毁时删除全局函数
  // 避免列表中某个消息的卸载导致后续所有消息都报错 previewHtmlCode is not defined
})
</script>

<template>
  <div class="message-item" :class="{ user: isUser, assistant: !isUser }">
    <div class="message-avatar">
      <el-avatar 
        v-if="isUser" 
        :size="36" 
        class="avatar-user"
        :src="userAvatar"
      >
        {{ userName?.charAt(0)?.toUpperCase() }}
      </el-avatar>
      <el-avatar v-else :size="36" class="avatar-ai" :src="'/logo.png'">
      </el-avatar>
    </div>
    
    <div class="message-content">
      <div v-if="message.imageUrl" class="message-image">
        <el-image
          :src="message.imageUrl"
          fit="contain"
          :preview-src-list="[message.imageUrl]"
        />
      </div>
      
      <div v-if="message.documentUrl" class="message-document">
        <el-icon><Document /></el-icon>
        <span>{{ message.documentUrl.split('/').pop() }}</span>
      </div>
      
      <div
        class="message-text"
        :class="{ streaming }"
        v-html="renderedContent"
      />
      
      <div v-if="streaming" class="streaming-indicator">
        <span class="dot"></span>
        <span class="dot"></span>
        <span class="dot"></span>
      </div>
    </div>
  </div>
</template>

<style scoped>
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
  user-select: none;
  cursor: default;
}

.avatar-ai {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: var(--color-white);
  user-select: none;
  cursor: default;
}

.message-content {
  max-width: 70%;
  padding: var(--spacing-md);
  border-radius: var(--radius-lg);
  background: var(--color-white);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-light);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
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

.message-document {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
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

.message-text :deep(.code-block-wrapper) {
  margin: var(--spacing-sm) 0;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: #1e1e1e;
}

.message-text :deep(.code-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  background: #2d2d2d;
  border-bottom: 1px solid #3d3d3d;
}

.message-text :deep(.code-lang) {
  font-size: 12px;
  color: #888;
  text-transform: lowercase;
}

.message-text :deep(.code-actions) {
  display: flex;
  gap: 8px;
}

.message-text :deep(.code-btn) {
  background: transparent;
  border: none;
  color: #888;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.message-text :deep(.code-btn:hover) {
  background: #3d3d3d;
  color: #fff;
}

.message-text :deep(.code-content) {
  margin: 0;
  padding: var(--spacing-md);
  overflow-x: auto;
  background: #1e1e1e;
}

.message-text :deep(.code-content code) {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: var(--font-size-sm);
  background: transparent;
  color: #d4d4d4;
}

.message-text :deep(pre:not(.code-content)) {
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

.message-text :deep(.music-player) {
  width: 100%;
  height: 40px;
  margin: 8px 0;
  border-radius: var(--radius-md);
  background: var(--color-bg-tertiary);
}

.message-text :deep(.music-player::-webkit-media-controls-panel) {
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-md);
}
</style>
