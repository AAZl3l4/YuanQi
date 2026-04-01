<script setup>
import { ref, computed } from 'vue'
import { extractHtmlCode } from '@/utils/markdown'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

const showPreview = ref(false)
const fullScreen = ref(false)

const htmlCodes = computed(() => extractHtmlCode(props.content || ''))
const hasCode = computed(() => htmlCodes.value.length > 0)

const previewHtml = computed(() => {
  if (htmlCodes.value.length === 0) return ''
  return htmlCodes.value[0]
})

const handlePreview = () => {
  showPreview.value = true
}

const handleFullScreen = () => {
  fullScreen.value = !fullScreen.value
}
</script>

<template>
  <div v-if="hasCode" class="code-preview">
    <div class="preview-actions">
      <el-button size="small" type="primary" @click="handlePreview">
        <el-icon><View /></el-icon>
        预览网页
      </el-button>
    </div>
    
    <el-dialog
      v-model="showPreview"
      title="网页预览"
      :width="fullScreen ? '100%' : '80%'"
      :fullscreen="fullScreen"
      class="preview-dialog"
    >
      <template #header>
        <div class="dialog-header">
          <span>网页预览</span>
          <el-button text @click="handleFullScreen">
            <el-icon>
              <FullScreen v-if="!fullScreen" />
              <Close v-else />
            </el-icon>
          </el-button>
        </div>
      </template>
      
      <div class="preview-container">
        <iframe
          :srcdoc="previewHtml"
          class="preview-iframe"
          sandbox="allow-scripts allow-same-origin"
        />
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.code-preview {
  margin-top: var(--spacing-sm);
}

.preview-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.preview-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: calc(100vh - 120px);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.preview-container {
  width: 100%;
  height: 100%;
  background: var(--color-white);
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
