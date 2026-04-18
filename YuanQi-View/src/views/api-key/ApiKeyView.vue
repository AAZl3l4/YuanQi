<script setup>
import { ref, onMounted } from 'vue'
import { getMyApiKeys, createApiKey, deleteMyApiKey } from '@/api/apiKey'
import { getRelayConfigList, getMyRelayLogs } from '@/api/relay'
import { getMyKnowledgeList } from '@/api/knowledge'
import { uploadFile } from '@/api/file'
import { ElMessage, ElMessageBox } from 'element-plus'

const apiKeys = ref([])
const configs = ref([])
const knowledgeBases = ref([])
const logs = ref([])
const loading = ref(false)
const logsLoading = ref(false)
const dialogVisible = ref(false)
const testDialogVisible = ref(false)
const formRef = ref()
const activeTab = ref('keys')
const expandedLogs = ref(new Set())
const imagePreviewVisible = ref(false)
const previewImageUrl = ref('')
const senderSearch = ref('')
const configIdSearch = ref('')
const knowledgeBaseIdSearch = ref('')

const logsPagination = ref({
  page: 1,
  size: 100,
  total: 0
})

const form = ref({
  configId: null,
  knowledgeBaseId: null,
  keyName: '',
  expireTime: null
})

const testForm = ref({
  apiKey: '',
  message: '',
  imageUrl: '',
  sender: '',
  contextRounds: 0,
  useKnowledgeBase: false,
  enableWebSearch: false
})
const testLoading = ref(false)
const testResponse = ref('')
const imageLoading = ref(false)

const rules = {
  configId: [{ required: true, message: '请选择配置', trigger: 'change' }],
  keyName: [{ required: true, message: '请输入Key名称', trigger: 'blur' }]
}

const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/jpg']

const loadApiKeys = async () => {
  loading.value = true
  try {
    const res = await getMyApiKeys({ page: 1, size: 100 })
    if (res.code === 200) {
      apiKeys.value = res.data.records || []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadConfigs = async () => {
  try {
    const res = await getRelayConfigList({ page: 1, size: 100, onlyMine: false })
    if (res.code === 200) {
      configs.value = res.data.records || []
    }
  } catch (error) {
    console.error(error)
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

const loadLogs = async () => {
  logsLoading.value = true
  try {
    const res = await getMyRelayLogs({ 
      page: logsPagination.value.page, 
      size: logsPagination.value.size,
      sender: senderSearch.value || undefined,
      configId: configIdSearch.value || undefined,
      knowledgeBaseId: knowledgeBaseIdSearch.value || undefined
    })
    if (res.code === 200) {
      logs.value = res.data.records || []
      logsPagination.value.total = res.data.total || 0
    }
  } catch (error) {
    console.error(error)
  } finally {
    logsLoading.value = false
  }
}

const searchBySender = (sender) => {
  senderSearch.value = sender
  logsPagination.value.page = 1
  loadLogs()
}

const searchByConfigId = (configId) => {
  configIdSearch.value = String(configId)
  logsPagination.value.page = 1
  loadLogs()
}

const searchByKnowledgeBaseId = (knowledgeBaseId) => {
  knowledgeBaseIdSearch.value = String(knowledgeBaseId)
  logsPagination.value.page = 1
  loadLogs()
}

const clearSenderSearch = () => {
  senderSearch.value = ''
  logsPagination.value.page = 1
  loadLogs()
}

const clearConfigIdSearch = () => {
  configIdSearch.value = ''
  logsPagination.value.page = 1
  loadLogs()
}

const clearKnowledgeBaseIdSearch = () => {
  knowledgeBaseIdSearch.value = ''
  logsPagination.value.page = 1
  loadLogs()
}

const handleLogsPageChange = (page) => {
  logsPagination.value.page = page
  loadLogs()
}

const handleCreate = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  try {
    // 转换时间格式：Date对象转为 yyyy-MM-dd HH:mm:ss 格式
    const submitData = { ...form.value }
    if (submitData.expireTime) {
      const date = new Date(submitData.expireTime)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      submitData.expireTime = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    }
    const res = await createApiKey(submitData)
    if (res.code === 200) {
      ElMessage.success('创建成功')
      dialogVisible.value = false
      form.value = { configId: null, knowledgeBaseId: null, keyName: '', expireTime: null }
      loadApiKeys()
    }
  } catch (error) {
    console.error(error)
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该API Key吗？', '提示', { type: 'warning' })
    const res = await deleteMyApiKey(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadApiKeys()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const openTestDialog = (apiKey) => {
  testForm.value = {
    apiKey: apiKey,
    message: '',
    imageUrl: '',
    sender: '',
    contextRounds: 0,
    useKnowledgeBase: false,
    enableWebSearch: false
  }
  testResponse.value = ''
  testDialogVisible.value = true
}

const handleImageUpload = async (file) => {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    ElMessage.warning('图片格式不支持，仅支持 jpg、jpeg、png 格式')
    return false
  }
  
  imageLoading.value = true
  try {
    const res = await uploadFile(file)
    if (typeof res === 'string') {
      testForm.value.imageUrl = res
      ElMessage.success('图片上传成功')
    }
  } catch (error) {
    ElMessage.error('图片上传失败')
  } finally {
    imageLoading.value = false
  }
  return false
}

const handleRemoveImage = () => {
  testForm.value.imageUrl = ''
}

const handleTestCall = async () => {
  if (!testForm.value.message && !testForm.value.imageUrl) {
    ElMessage.warning('请输入消息或上传图片')
    return
  }
  
  testLoading.value = true
  testResponse.value = ''
  
  try {
    const res = await fetch('/api/relay/call', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': testForm.value.apiKey
      },
      body: JSON.stringify({
        message: testForm.value.message || undefined,
        imageUrl: testForm.value.imageUrl || undefined,
        sender: testForm.value.sender || undefined,
        contextRounds: testForm.value.contextRounds || undefined,
        useKnowledgeBase: testForm.value.useKnowledgeBase || undefined,
        enableWebSearch: testForm.value.enableWebSearch || undefined
      })
    })
    
    const data = await res.json()
    if (data.code === 200) {
      testResponse.value = data.data
    } else {
      testResponse.value = `错误: ${data.message}`
    }
  } catch (error) {
    testResponse.value = `请求失败: ${error.message}`
  } finally {
    testLoading.value = false
  }
}

const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (err) {
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    try {
      document.execCommand('copy')
      ElMessage.success('已复制到剪贴板')
    } catch (e) {
      ElMessage.error('复制失败，请手动复制')
    }
    document.body.removeChild(textarea)
  }
}

const copyResponse = () => {
  if (testResponse.value) {
    navigator.clipboard.writeText(testResponse.value)
    ElMessage.success('已复制')
  }
}

const handleTabChange = (tab) => {
  if (tab === 'logs' && logs.value.length === 0) {
    loadLogs()
  }
}

const toggleLog = (id) => {
  if (expandedLogs.value.has(id)) {
    expandedLogs.value.delete(id)
  } else {
    expandedLogs.value.add(id)
  }
}

const isLogExpanded = (id) => expandedLogs.value.has(id)

const openImagePreview = (url) => {
  previewImageUrl.value = url
  imagePreviewVisible.value = true
}

const handleImageError = (e) => {
  // 显示默认图片，但保存原地址用于点击跳转
  if (!e.target.dataset.originalSrc) {
    e.target.dataset.originalSrc = e.target.src
  }
  e.target.src = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCIgaGVpZ2h0PSIxMDAiIGZpbGw9IiNlZWUiLz48dGV4dCB4PSI1MCIgeT0iNTAiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxMiIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuWbvueJh+WKoOi9veWksei0pTwvdGV4dD48L3N2Zz4='
}

const handleImageClick = (url, event) => {
  // 检查图片是否加载失败（通过检查是否有 originalSrc）
  const img = event.target
  if (img.dataset.originalSrc) {
    // 加载失败，使用 noreferrer 打开原地址（解决QQ图片防盗链）
    const link = document.createElement('a')
    link.href = url
    link.target = '_blank'
    link.rel = 'noreferrer'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } else {
    // 加载成功，打开预览
    openImagePreview(url)
  }
}

onMounted(() => {
  loadApiKeys()
  loadConfigs()
  loadKnowledgeBases()
})
</script>

<template>
  <div class="api-key-view page-container">
    <div class="page-header">
      <h2 class="page-title">
        <el-icon class="title-icon"><Key /></el-icon>
        API Key 管理
      </h2>
      <el-button type="primary" @click="dialogVisible = true">
        <el-icon><Plus /></el-icon>
        新建Key
      </el-button>
    </div>
    
    <el-tabs v-model="activeTab" class="content-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="API Key" name="keys">
        <el-table :data="apiKeys" v-loading="loading" class="card">
          <el-table-column prop="keyName" label="名称" width="150" />
          <el-table-column prop="apiKey" label="API Key" min-width="200">
            <template #default="{ row }">
              <div class="api-key-cell">
                <code>{{ row.apiKey }}</code>
                <el-button text size="small" @click="copyToClipboard(row.apiKey)">
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="configName" label="关联配置" width="150" />
          <el-table-column prop="expireTime" label="过期时间" width="180">
            <template #default="{ row }">
              {{ row.expireTime || '永不过期' }}
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openTestDialog(row.apiKey)">
                测试
              </el-button>
              <el-button text type="danger" @click="handleDelete(row.id)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="调用记录" name="logs">
        <div class="logs-container" v-loading="logsLoading">
          <div class="logs-toolbar">
            <div class="search-filters">
              <el-input
                v-model="configIdSearch"
                placeholder="配置ID"
                clearable
                class="filter-input"
                @clear="clearConfigIdSearch"
                @keyup.enter="loadLogs"
              />
              <el-input
                v-model="knowledgeBaseIdSearch"
                placeholder="知识库ID"
                clearable
                class="filter-input"
                @clear="clearKnowledgeBaseIdSearch"
                @keyup.enter="loadLogs"
              />
              <el-input
                v-model="senderSearch"
                placeholder="发送者"
                clearable
                class="filter-input"
                @clear="clearSenderSearch"
                @keyup.enter="loadLogs"
              />
              <el-button type="primary" @click="loadLogs">
                <el-icon><Search /></el-icon>
              </el-button>
            </div>
          </div>
          <div class="logs-grid">
            <div v-for="log in logs" :key="log.id" class="log-card card">
              <div class="log-header">
                <div class="header-row">
                  <div class="config-info">
                    <span class="config-label">配置</span>
                    <span class="config-value clickable" @click="searchByConfigId(log.configId)">{{ log.configId || '-' }}</span>
                  </div>
                  <div class="sender-info" v-if="log.sender">
                    <span class="sender-label">发送者</span>
                    <span class="sender-value clickable" @click="searchBySender(log.sender)">{{ log.sender }}</span>
                  </div>
                </div>
                <span class="log-time">{{ log.createTime }}</span>
              </div>
              <div class="log-body">
                <div class="log-message" v-if="log.inputMessage">
                  <div class="message-header" @click="toggleLog('i' + log.id)">
                    <span class="label">消息</span>
                    <el-icon class="expand-icon" :class="{ expanded: isLogExpanded('i' + log.id) }">
                      <ArrowDown />
                    </el-icon>
                  </div>
                  <div class="message-content" :class="{ expanded: isLogExpanded('i' + log.id) }">
                    {{ log.inputMessage }}
                  </div>
                </div>
                <div class="log-response" v-if="log.outputMessage">
                  <div class="response-header" @click="toggleLog('o' + log.id)">
                    <span class="label">响应</span>
                    <el-icon class="expand-icon" :class="{ expanded: isLogExpanded('o' + log.id) }">
                      <ArrowDown />
                    </el-icon>
                  </div>
                  <div class="response-content" :class="{ expanded: isLogExpanded('o' + log.id) }">
                    {{ log.outputMessage }}
                  </div>
                </div>
                <div class="log-image" v-if="log.imageUrl">
                  <span class="label">图片</span>
                  <div class="image-wrapper" @click="handleImageClick(log.imageUrl, $event)">
                    <img :src="log.imageUrl" alt="调用图片" @error="handleImageError" />
                    <div class="image-tip">点击查看大图</div>
                  </div>
                  <div class="image-notice">
                    <el-icon><Warning /></el-icon>
                    <span>QQ图片可能无法正常显示，点击可尝试在新标签页打开</span>
                  </div>
                </div>
              </div>
              <div class="log-footer">
                <div class="token-info">
                  <span class="token-item">
                    <el-icon><Upload /></el-icon>
                    {{ log.inputTokens || 0 }}
                  </span>
                  <span class="token-item">
                    <el-icon><Download /></el-icon>
                    {{ log.outputTokens || 0 }}
                  </span>
                  <span class="kb-tag" v-if="log.knowledgeBaseId" @click="searchByKnowledgeBaseId(log.knowledgeBaseId)">
                    <el-icon><Collection /></el-icon>
                    KB:{{ log.knowledgeBaseId }}
                  </span>
                  <span class="web-tag" v-if="log.enableWebSearch">
                    <el-icon><Search /></el-icon>
                    联网
                  </span>
                  <span class="model-tag" v-if="log.modelUsed">{{ log.modelUsed }}</span>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-if="!logsLoading && logs.length === 0" description="暂无调用记录" />
          <div class="pagination-wrapper" v-if="logsPagination.total > 0">
            <el-pagination
              v-model:current-page="logsPagination.page"
              :page-size="logsPagination.size"
              :total="logsPagination.total"
              layout="total, prev, pager, next"
              @current-change="handleLogsPageChange"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
    
    <el-dialog v-model="dialogVisible" title="新建API Key" width="500px" class="custom-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="keyName">
          <el-input v-model="form.keyName" placeholder="请输入Key名称" />
        </el-form-item>
        <el-form-item label="配置" prop="configId">
          <el-select v-model="form.configId" placeholder="选择配置" style="width: 100%">
            <el-option
              v-for="config in configs"
              :key="config.id"
              :label="config.name"
              :value="config.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="知识库">
          <el-select v-model="form.knowledgeBaseId" placeholder="选择知识库（可选）" style="width: 100%" clearable>
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
          <div class="form-tip">绑定知识库后，调用时会检索知识库内容增强回答</div>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            placeholder="留空则永不过期"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
    
    <el-dialog 
      v-model="testDialogVisible" 
      title="API中转测试" 
      width="650px" 
      class="custom-dialog test-dialog"
      :close-on-click-modal="false"
    >
      <div class="test-content">
        <div class="api-key-display">
          <span class="label">API Key</span>
          <div class="key-value">
            <code>{{ testForm.apiKey }}</code>
            <el-button text size="small" @click="copyToClipboard(testForm.apiKey)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </div>
        
        <div class="context-section">
          <div class="context-row">
            <div class="context-item">
              <el-tooltip content="用于区分不同来源的对话上下文，如QQ号" placement="top">
                <span class="context-label">发送者</span>
              </el-tooltip>
              <el-input
                v-model="testForm.sender"
                placeholder="如QQ号"
                clearable
                class="context-input"
              />
            </div>
            <div class="context-item">
              <el-tooltip content="多轮对话时保留的历史消息轮数" placement="top">
                <span class="context-label">上下文轮数</span>
              </el-tooltip>
              <el-input-number
                v-model="testForm.contextRounds"
                :min="0"
                :max="20"
                controls-position="right"
                class="context-input"
              />
            </div>
            <div class="context-item">
              <el-tooltip content="仅当API Key绑定了知识库时生效" placement="top">
                <span class="context-label">使用知识库</span>
              </el-tooltip>
              <el-switch
                v-model="testForm.useKnowledgeBase"
                class="context-switch"
              />
            </div>
            <div class="context-item">
              <el-tooltip content="启用后可获取实时新闻、热点等信息。注意：联网搜索可能导致人设效果减弱" placement="top">
                <span class="context-label">联网搜索</span>
              </el-tooltip>
              <el-switch
                v-model="testForm.enableWebSearch"
                class="context-switch"
              />
            </div>
          </div>
        </div>
        
        <div class="input-section">
          <div class="section-title">
            <el-icon><EditPen /></el-icon>
            <span>输入内容</span>
            <span class="hint">（消息和图片至少填一项）</span>
          </div>
          
          <div class="input-row">
            <div class="message-input">
              <el-input
                v-model="testForm.message"
                type="textarea"
                :rows="4"
                placeholder="请输入消息内容..."
                resize="none"
              />
            </div>
            
            <div class="image-upload">
              <div v-if="!testForm.imageUrl" class="upload-area">
                <el-upload
                  :show-file-list="false"
                  :before-upload="handleImageUpload"
                  accept=".jpg,.jpeg,.png"
                  class="uploader"
                >
                  <div class="upload-trigger" :class="{ 'is-loading': imageLoading }">
                    <el-icon v-if="!imageLoading" class="upload-icon"><Picture /></el-icon>
                    <el-icon v-else class="is-loading"><Loading /></el-icon>
                    <span class="upload-text">{{ imageLoading ? '上传中...' : '上传图片' }}</span>
                    <span class="upload-hint">jpg/png/jpeg</span>
                  </div>
                </el-upload>
              </div>
              <div v-else class="image-preview">
                <img :src="testForm.imageUrl" alt="已上传图片" />
                <div class="image-actions">
                  <el-button type="danger" size="small" circle @click="handleRemoveImage">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="action-bar">
          <el-button 
            type="primary" 
            :loading="testLoading" 
            @click="handleTestCall"
            class="send-btn"
          >
            <el-icon v-if="!testLoading"><Promotion /></el-icon>
            {{ testLoading ? '请求中...' : '发送请求' }}
          </el-button>
        </div>
        
        <div class="response-section">
          <div class="section-title">
            <el-icon><Document /></el-icon>
            <span>响应结果</span>
            <el-button 
              v-if="testResponse" 
              text 
              size="small" 
              @click="copyResponse"
              class="copy-btn"
            >
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
          </div>
          <div class="response-content">
            <pre v-if="testResponse">{{ testResponse }}</pre>
            <div v-else class="empty-placeholder">
              <el-icon><Document /></el-icon>
              <span>发送请求后显示响应结果</span>
            </div>
          </div>
        </div>
        
        <div class="usage-tips">
          <div class="tips-header">
            <el-icon><InfoFilled /></el-icon>
            <span>外部调用示例</span>
          </div>
          <pre class="code-block">curl -X POST http://47.105.51.84/api/relay/call \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{"message": "你好"}'</pre>
        </div>
      </div>
    </el-dialog>
    
    <el-dialog 
      v-model="imagePreviewVisible" 
      title="图片预览" 
      width="800px"
      class="image-preview-dialog"
    >
      <img :src="previewImageUrl" alt="预览图片" class="preview-image-full" />
    </el-dialog>
  </div>
</template>

<style scoped>
.api-key-view {
  max-width: 1200px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
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

.content-tabs {
  background: var(--color-bg);
}

.api-key-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.api-key-cell code {
  font-family: monospace;
  font-size: var(--font-size-sm);
  background: var(--color-bg-secondary);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

.logs-container {
  min-height: 300px;
}

.logs-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--spacing-md);
}

.search-filters {
  display: flex;
  gap: var(--spacing-xs);
  align-items: center;
}

.filter-input {
  width: 100px;
}

.logs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: var(--spacing-md);
}

.log-card {
  padding: var(--spacing-md);
  transition: all 0.3s ease;
}

.log-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.log-header {
  margin-bottom: var(--spacing-sm);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-border-light);
}

.header-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.config-info,
.sender-info {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.config-label,
.sender-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.config-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-primary);
}

.config-value.clickable {
  cursor: pointer;
  transition: color 0.2s;
}

.config-value.clickable:hover {
  color: var(--color-success);
  text-decoration: underline;
}

.sender-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-success);
}

.sender-value.clickable {
  cursor: pointer;
  transition: color 0.2s;
}

.sender-value.clickable:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.log-time {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-left: auto;
}

.log-body {
  margin-bottom: var(--spacing-sm);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.log-message,
.log-response {
  background: var(--color-bg-secondary);
  border-radius: 8px;
  padding: var(--spacing-sm);
}

.message-header,
.response-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.message-header:hover .label,
.response-header:hover .label {
  color: var(--color-primary);
}

.expand-icon {
  font-size: 12px;
  color: var(--color-text-muted);
  transition: transform 0.3s ease;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.log-body .label {
  font-size: 12px;
  color: var(--color-text-muted);
  transition: color 0.2s;
}

.message-content,
.response-content {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-top: var(--spacing-xs);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  transition: all 0.3s ease;
}

.message-content.expanded,
.response-content.expanded {
  -webkit-line-clamp: unset;
  display: block;
}

.log-image {
  background: var(--color-bg-secondary);
  border-radius: 8px;
  padding: var(--spacing-sm);
}

.log-image .label {
  font-size: 12px;
  color: var(--color-text-muted);
  display: block;
  margin-bottom: var(--spacing-xs);
}

.image-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 6px;
  overflow: hidden;
}

.image-wrapper img {
  width: 100%;
  max-height: 120px;
  object-fit: cover;
  border-radius: 6px;
  transition: opacity 0.2s;
}

.image-wrapper:hover img {
  opacity: 0.8;
}

.image-wrapper:hover .image-tip {
  opacity: 1;
}

.image-tip {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-notice {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: var(--spacing-xs);
  font-size: 11px;
  color: var(--color-warning);
}

.image-notice .el-icon {
  font-size: 12px;
}

.log-footer {
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border-light);
}

.token-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.token-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.token-item .el-icon {
  font-size: 12px;
}

.model-tag {
  font-size: 11px;
  color: var(--color-primary);
  background: var(--color-primary-light);
  padding: 2px 8px;
  border-radius: 4px;
  margin-left: auto;
}

.kb-tag {
  font-size: 11px;
  color: var(--color-warning);
  background: var(--color-warning-light);
  padding: 2px 8px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s;
}

.kb-tag:hover {
  background: var(--color-warning);
  color: white;
}

.kb-tag .el-icon {
  font-size: 11px;
}

.web-tag {
  font-size: 11px;
  color: var(--color-success);
  background: var(--color-success-light);
  padding: 2px 8px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.web-tag .el-icon {
  font-size: 11px;
}

.preview-image-full {
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-lg);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--color-border-light);
}

.test-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.api-key-display {
  background: linear-gradient(135deg, var(--color-primary-light) 0%, var(--color-bg-secondary) 100%);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
}

.api-key-display .label {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  display: block;
  margin-bottom: var(--spacing-xs);
}

.context-section {
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
}

.context-row {
  display: flex;
  gap: var(--spacing-md);
}

.context-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.context-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.context-input {
  width: 100%;
}

.context-switch {
  margin-top: var(--spacing-xs);
}

.context-hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin-top: var(--spacing-xs);
}

.key-value {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.key-value code {
  font-family: monospace;
  font-size: var(--font-size-sm);
  color: var(--color-primary-dark);
  word-break: break-all;
}

.input-section {
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
}

.section-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-sm);
  font-weight: 500;
  margin-bottom: var(--spacing-md);
  color: var(--color-text);
}

.section-title .el-icon {
  color: var(--color-primary);
}

.section-title .hint {
  font-weight: normal;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  margin-left: auto;
}

.form-tip {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin-top: var(--spacing-xs);
}

.input-row {
  display: flex;
  gap: var(--spacing-md);
}

.message-input {
  flex: 1;
}

.image-upload {
  width: 140px;
  flex-shrink: 0;
}

.upload-area {
  height: 100%;
  min-height: 100px;
}

.uploader {
  width: 100%;
  height: 100%;
}

.uploader :deep(.el-upload) {
  width: 100%;
  height: 100%;
}

.upload-trigger {
  width: 100%;
  height: 100%;
  min-height: 100px;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
  cursor: pointer;
  transition: all var(--transition-fast);
  background: var(--color-bg);
}

.upload-trigger:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.upload-trigger.is-loading {
  cursor: not-allowed;
  opacity: 0.7;
}

.upload-icon {
  font-size: 24px;
  color: var(--color-text-muted);
}

.upload-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.upload-hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.image-preview {
  position: relative;
  width: 100%;
  height: 100px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border-light);
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-actions {
  position: absolute;
  top: var(--spacing-xs);
  right: var(--spacing-xs);
}

.action-bar {
  display: flex;
  justify-content: flex-end;
}

.send-btn {
  min-width: 120px;
}

.response-section {
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
}

.response-section .section-title {
  margin-bottom: var(--spacing-sm);
}

.copy-btn {
  margin-left: auto;
}

.response-content {
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  padding: var(--spacing-md);
  min-height: 80px;
  max-height: 200px;
  overflow-y: auto;
}

.response-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: monospace;
  font-size: var(--font-size-sm);
  line-height: 1.6;
  color: var(--color-text);
}

.empty-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  min-height: 60px;
}

.empty-placeholder .el-icon {
  font-size: 24px;
  opacity: 0.5;
}

.usage-tips {
  border-top: 1px solid var(--color-border-light);
  padding-top: var(--spacing-md);
}

.tips-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-sm);
  font-weight: 500;
  margin-bottom: var(--spacing-sm);
  color: var(--color-text-secondary);
}

.tips-header .el-icon {
  color: var(--color-primary);
}

.code-block {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  font-size: var(--font-size-xs);
  overflow-x: auto;
  margin: 0;
  line-height: 1.6;
  font-family: 'Consolas', 'Monaco', monospace;
}
</style>
