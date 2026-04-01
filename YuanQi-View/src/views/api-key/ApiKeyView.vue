<script setup>
import { ref, onMounted } from 'vue'
import { getMyApiKeys, createApiKey, deleteMyApiKey } from '@/api/apiKey'
import { getRelayConfigList } from '@/api/relay'
import { uploadFile } from '@/api/file'
import { ElMessage, ElMessageBox } from 'element-plus'

const apiKeys = ref([])
const configs = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const testDialogVisible = ref(false)
const formRef = ref()

const form = ref({
  configId: null,
  keyName: '',
  expireTime: null
})

const testForm = ref({
  apiKey: '',
  message: '',
  imageUrl: ''
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

const handleCreate = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  try {
    const res = await createApiKey(form.value)
    if (res.code === 200) {
      ElMessage.success('创建成功')
      dialogVisible.value = false
      form.value = { configId: null, keyName: '', expireTime: null }
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
    imageUrl: ''
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
        imageUrl: testForm.value.imageUrl || undefined
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

const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制到剪贴板')
}

const copyResponse = () => {
  if (testResponse.value) {
    navigator.clipboard.writeText(testResponse.value)
    ElMessage.success('已复制')
  }
}

onMounted(() => {
  loadApiKeys()
  loadConfigs()
})
</script>

<template>
  <div class="api-key-view page-container">
    <div class="page-header">
      <h2 class="page-title">API Key 管理</h2>
      <el-button type="primary" @click="dialogVisible = true">
        <el-icon><Plus /></el-icon>
        新建Key
      </el-button>
    </div>
    
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
    
    <!-- 新建API Key弹窗 -->
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
    
    <!-- 测试弹窗 -->
    <el-dialog 
      v-model="testDialogVisible" 
      title="API中转测试" 
      width="650px" 
      class="custom-dialog test-dialog"
      :close-on-click-modal="false"
    >
      <div class="test-content">
        <!-- API Key 显示 -->
        <div class="api-key-display">
          <span class="label">API Key</span>
          <div class="key-value">
            <code>{{ testForm.apiKey }}</code>
            <el-button text size="small" @click="copyToClipboard(testForm.apiKey)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </div>
        
        <!-- 输入区域 -->
        <div class="input-section">
          <div class="section-title">
            <el-icon><EditPen /></el-icon>
            <span>输入内容</span>
            <span class="hint">（消息和图片至少填一项）</span>
          </div>
          
          <div class="input-row">
            <!-- 消息输入 -->
            <div class="message-input">
              <el-input
                v-model="testForm.message"
                type="textarea"
                :rows="4"
                placeholder="请输入消息内容..."
                resize="none"
              />
            </div>
            
            <!-- 图片上传 -->
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
        
        <!-- 发送按钮 -->
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
        
        <!-- 响应结果 -->
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
        
        <!-- 使用提示 -->
        <div class="usage-tips">
          <div class="tips-header">
            <el-icon><InfoFilled /></el-icon>
            <span>外部调用示例</span>
          </div>
          <pre class="code-block">curl -X POST http://47.105.51.84:8080/relay/call \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{"message": "你好"}'</pre>
        </div>
      </div>
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

/* 测试弹窗样式 */
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
