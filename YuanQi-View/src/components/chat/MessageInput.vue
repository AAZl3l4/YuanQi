<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/file'
import { getEnabledTools } from '@/api/mcp'
import { getMyKnowledgeList } from '@/api/knowledge'
import { usePreviewStore } from '@/stores/preview'
import { storeToRefs } from 'pinia'

const props = defineProps({
  disabled: Boolean,
  isAgentChat: Boolean
})

const emit = defineEmits(['send', 'stop'])

const previewStore = usePreviewStore()
const { pickedElement } = storeToRefs(previewStore)

const message = ref('')
const imageUrl = ref('')
const documentUrl = ref('')
const documentName = ref('')
const knowledgeBaseId = ref(null)
const enabledTools = ref([])
const generateApp = ref(false)
const contextRounds = ref(10)

const showTools = ref(false)
const showKnowledge = ref(false)
const showSettings = ref(false)
const tools = ref([])
const knowledgeBases = ref([])

const imageLoading = ref(false)
const documentLoading = ref(false)
const isDragging = ref(false)

// 图片类型限制：jpg、jpeg、png
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/jpg']

// 文档类型限制：TXT、MD、HTML、XML、JSON、CSV、PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX、RTF、ODT、ODS、ODP
const ALLOWED_DOC_TYPES = [
  // 文本类
  'text/plain',                    // TXT
  'text/markdown',                 // MD
  'text/html',                     // HTML
  'text/xml',                      // XML
  'application/xml',               // XML
  'application/json',              // JSON
  'text/csv',                      // CSV
  'application/csv',               // CSV
  // PDF
  'application/pdf',               // PDF
  // Word
  'application/msword',            // DOC
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document', // DOCX
  // Excel
  'application/vnd.ms-excel',      // XLS
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', // XLSX
  // PowerPoint
  'application/vnd.ms-powerpoint', // PPT
  'application/vnd.openxmlformats-officedocument.presentationml.presentation', // PPTX
  // 其他格式
  'application/rtf',               // RTF
  'text/rtf',                      // RTF
  'application/vnd.oasis.opendocument.text',     // ODT
  'application/vnd.oasis.opendocument.spreadsheet', // ODS
  'application/vnd.oasis.opendocument.presentation' // ODP
]

const contextRoundsOptions = [
  { label: '不开启纪录', value: 0 },
  { label: '5轮', value: 5 },
  { label: '10轮', value: 10 },
  { label: '15轮', value: 15 },
  { label: '20轮', value: 20 }
]

const handleImageUpload = async (file) => {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    ElMessage.error('图片格式不支持，仅支持 jpg、jpeg、png 格式')
    return false
  }
  
  imageLoading.value = true
  try {
    const res = await uploadFile(file)
    if (typeof res === 'string') {
      imageUrl.value = res
      ElMessage.success('图片上传成功')
    }
  } catch (error) {
    ElMessage.error('图片上传失败')
  } finally {
    imageLoading.value = false
  }
  return false
}

const handleDocumentUpload = async (file) => {
  documentLoading.value = true
  try {
    const res = await uploadFile(file)
    if (typeof res === 'string') {
      documentUrl.value = res
      documentName.value = file.name
      ElMessage.success('文档上传成功')
    }
  } catch (error) {
    ElMessage.error('文档上传失败')
  } finally {
    documentLoading.value = false
  }
  return false
}

// 根据文件扩展名获取MIME类型（用于拖拽上传时类型识别）
const getMimeTypeByExtension = (filename) => {
  const ext = filename.split('.').pop().toLowerCase()
  const mimeMap = {
    'txt': 'text/plain',
    'md': 'text/markdown',
    'html': 'text/html',
    'xml': 'text/xml',
    'json': 'application/json',
    'csv': 'text/csv',
    'pdf': 'application/pdf',
    'doc': 'application/msword',
    'docx': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'xls': 'application/vnd.ms-excel',
    'xlsx': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'ppt': 'application/vnd.ms-powerpoint',
    'pptx': 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'rtf': 'application/rtf',
    'odt': 'application/vnd.oasis.opendocument.text',
    'ods': 'application/vnd.oasis.opendocument.spreadsheet',
    'odp': 'application/vnd.oasis.opendocument.presentation',
    'jpg': 'image/jpeg',
    'jpeg': 'image/jpeg',
    'png': 'image/png'
  }
  return mimeMap[ext] || ''
}

const handleFileDrop = async (e) => {
  e.preventDefault()
  isDragging.value = false
  
  const files = e.dataTransfer?.files
  if (!files || files.length === 0) return
  
  const file = files[0]
  // 拖拽时可能无法正确识别MIME类型，使用文件名扩展名作为备用
  const fileType = file.type || getMimeTypeByExtension(file.name)
  
  if (ALLOWED_IMAGE_TYPES.includes(fileType)) {
    await handleImageUpload(file)
  } else if (ALLOWED_DOC_TYPES.includes(fileType)) {
    await handleDocumentUpload(file)
  } else {
    ElMessage.warning('不支持的文件类型，仅支持：\n图片：jpg、jpeg、png\n文档：txt、md、html、xml、json、csv、pdf、doc、docx、xls、xlsx、ppt、pptx、rtf、odt、ods、odp')
  }
}

const handleDragOver = (e) => {
  e.preventDefault()
  if (!props.disabled) {
    isDragging.value = true
  }
}

const handleDragLeave = (e) => {
  e.preventDefault()
  isDragging.value = false
}

const loadTools = async () => {
  try {
    const res = await getEnabledTools()
    if (res.code === 200) {
      tools.value = res.data || []
    }
  } catch (error) {
    console.error(error)
  }
}

const loadKnowledge = async () => {
  try {
    const res = await getMyKnowledgeList({ page: 1, size: 100 })
    if (res.code === 200) {
      knowledgeBases.value = res.data.records || []
    }
  } catch (error) {
    console.error(error)
  }
}

const handleShowTools = () => {
  showTools.value = !showTools.value
  if (showTools.value && tools.value.length === 0) {
    loadTools()
  }
}

const handleShowKnowledge = () => {
  showKnowledge.value = !showKnowledge.value
  if (showKnowledge.value && knowledgeBases.value.length === 0) {
    loadKnowledge()
  }
}

const handleSend = () => {
  if (!message.value.trim() && !imageUrl.value && !pickedElement.value) {
    ElMessage.warning('请输入消息或上传附件')
    return
  }
  
  let finalMessage = message.value
  if (pickedElement.value) {
    finalMessage = finalMessage 
      ? `${finalMessage}\n\n选中的网页元素：\n\`\`\`html\n${pickedElement.value}\n\`\`\``
      : `请分析这个网页元素：\n\`\`\`html\n${pickedElement.value}\n\`\`\``
  }
  
  emit('send', {
    message: finalMessage,
    imageUrl: imageUrl.value,
    documentUrl: documentUrl.value,
    knowledgeBaseId: knowledgeBaseId.value,
    enabledTools: enabledTools.value,
    generateApp: generateApp.value,
    contextRounds: contextRounds.value
  })
  
  message.value = ''
  imageUrl.value = ''
  documentUrl.value = ''
  documentName.value = ''
  if (pickedElement.value) {
    previewStore.clearPickedElement()
  }
}

const handleKeyDown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

if (!props.isAgentChat) {
  loadTools()
  loadKnowledge()
}
</script>

<template>
  <div 
    class="message-input"
    :class="{ 'is-dragging': isDragging }"
    @drop="handleFileDrop"
    @dragover="handleDragOver"
    @dragleave="handleDragLeave"
  >
    <div v-if="isDragging" class="drag-overlay">
      <el-icon :size="48"><Upload /></el-icon>
      <span>释放文件以上传</span>
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
    
    <div v-if="documentUrl" class="preview-item document-preview">
      <el-icon><Document /></el-icon>
      <span class="doc-name">{{ documentName || documentUrl.split('/').pop() }}</span>
      <el-button
        type="danger"
        size="small"
        circle
        class="remove-btn"
        @click="documentUrl = ''; documentName = ''"
      >
        <el-icon><Close /></el-icon>
      </el-button>
    </div>

    <div v-if="pickedElement" class="preview-item html-preview">
      <el-icon color="var(--color-primary)"><Platform /></el-icon>
      <div class="html-snippet">
        <span class="html-tag">网页元素</span>
        <code class="html-code">{{ pickedElement.substring(0, 60) }}{{ pickedElement.length > 60 ? '...' : '' }}</code>
      </div>
      <el-button
        type="danger"
        size="small"
        circle
        class="remove-btn"
        @click="previewStore.clearPickedElement()"
      >
        <el-icon><Close /></el-icon>
      </el-button>
    </div>
    
    <div class="input-row">
      <el-input
        v-model="message"
        type="textarea"
        :rows="1"
        :autosize="{ minRows: 1, maxRows: 6 }"
        placeholder="输入消息... (支持拖拽上传图片/文档)"
        :disabled="disabled"
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
            <el-button text :loading="imageLoading">
              <el-icon><Picture /></el-icon>
            </el-button>
          </el-tooltip>
        </el-upload>
        
        <el-upload
          :show-file-list="false"
          accept=".txt,.md,.html,.xml,.json,.csv,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.rtf,.odt,.ods,.odp"
          :before-upload="handleDocumentUpload"
        >
          <el-tooltip content="上传文档 (支持多种格式)" placement="top">
            <el-button text :loading="documentLoading">
              <el-icon><Document /></el-icon>
            </el-button>
          </el-tooltip>
        </el-upload>
        
        <template v-if="!isAgentChat">
          <el-tooltip content="知识库" placement="top">
            <el-button text @click="handleShowKnowledge" :type="knowledgeBaseId ? 'primary' : ''">
              <el-icon><Collection /></el-icon>
            </el-button>
          </el-tooltip>
          
          <el-tooltip content="MCP工具" placement="top">
            <el-button text @click="handleShowTools" :type="enabledTools.length > 0 ? 'primary' : ''">
              <el-icon><Tools /></el-icon>
            </el-button>
          </el-tooltip>
          
          <el-tooltip placement="top">
            <template #content>
              <div>应用生成</div>
              <div style="font-size: 11px; color: #aaa; margin-top: 2px;">推荐使用glm-4.7-flash及以上模型</div>
            </template>
            <el-button text @click="generateApp = !generateApp" :type="generateApp ? 'primary' : ''">
              <el-icon><Monitor /></el-icon>
            </el-button>
          </el-tooltip>
          
          <el-tooltip content="对话设置" placement="top">
            <el-button text @click="showSettings = !showSettings">
              <el-icon><Setting /></el-icon>
            </el-button>
          </el-tooltip>
        </template>
      </div>
      
      <div class="toolbar-right">
        <el-button
          v-if="disabled"
          type="danger"
          @click="emit('stop')"
        >
          <el-icon><VideoPause /></el-icon>
          停止
        </el-button>
        <el-button
          v-else
          type="primary"
          @click="handleSend"
          :disabled="!message.trim() && !imageUrl"
        >
          <el-icon><Promotion /></el-icon>
          发送
        </el-button>
      </div>
    </div>
    
    <div v-if="showSettings && !isAgentChat" class="selector-panel">
      <div class="selector-header">对话设置</div>
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
    
    <div v-if="showKnowledge && !isAgentChat" class="selector-panel">
      <div class="selector-header">选择知识库</div>
      <el-select
        v-model="knowledgeBaseId"
        placeholder="选择知识库"
        clearable
        style="width: 100%"
      >
        <el-option
          v-for="kb in knowledgeBases"
          :key="kb.id"
          :label="kb.name"
          :value="kb.id"
        />
      </el-select>
    </div>
    
    <div v-if="showTools && !isAgentChat" class="selector-panel">
      <div class="selector-header">选择工具</div>
      <el-checkbox-group v-model="enabledTools">
        <el-checkbox
          v-for="tool in tools"
          :key="tool.id"
          :label="tool.id"
        >
          {{ tool.description || tool.name }}
        </el-checkbox>
      </el-checkbox-group>
    </div>
  </div>
</template>

<style scoped>
.message-input {
  border-top: 1px solid var(--color-border-light);
  padding: var(--spacing-md);
  background: var(--color-white);
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  position: relative;
  transition: all var(--transition-fast);
}

.message-input.is-dragging {
  background: var(--color-primary-light);
  border: 2px dashed var(--color-primary);
}

.drag-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(201, 169, 98, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  z-index: 10;
  color: var(--color-primary);
  font-size: var(--font-size-base);
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
}

.preview-image {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-sm);
}

.document-preview {
  padding: var(--spacing-sm) var(--spacing-md);
}

.doc-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
}

.html-preview {
  padding: var(--spacing-sm) var(--spacing-md);
  border-left: 3px solid var(--color-primary);
  background: var(--color-primary-light);
  /* 移除了 overflow: hidden 以防右上角删除按钮被裁边 */
}

.html-snippet {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  overflow: hidden;
}

.html-tag {
  font-size: 12px;
  color: var(--color-primary);
  font-weight: 600;
}

.html-code {
  font-family: monospace;
  font-size: 13px;
  color: var(--color-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background: rgba(0,0,0,0.05);
  padding: 2px 6px;
  border-radius: 4px;
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

.selector-panel {
  margin-top: var(--spacing-sm);
  padding: var(--spacing-md);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
}

.selector-header {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-sm);
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
</style>
