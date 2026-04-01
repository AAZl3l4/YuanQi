<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getMyContent, deleteMyContent, generateImage, submitVideoTask, queryVideoTask } from '@/api/content'
import { uploadFile } from '@/api/file'
import { ElMessage, ElMessageBox } from 'element-plus'

const contents = ref([])
const loading = ref(false)
const generateType = ref('image')
const generating = ref(false)
const uploading = ref(false)

const imageParams = ref({
  prompt: '',
  size: '1024x1024'
})

const videoParams = ref({
  prompt: '',
  imageUrl: '',
  size: '1024x1024',
  quality: 'speed',
  duration: 5,
  fps: 30,
  withAudio: false
})

const imageSizeOptions = [
  { label: '正方形 1024x1024', value: '1024x1024' },
  { label: '竖屏 768x1344', value: '768x1344' },
  { label: '竖屏 864x1152', value: '864x1152' },
  { label: '横屏 1344x768', value: '1344x768' },
  { label: '横屏 1152x864', value: '1152x864' },
  { label: '宽屏 1440x720', value: '1440x720' },
  { label: '竖长 720x1440', value: '720x1440' }
]

const videoSizeOptions = [
  { label: '正方形 1024x1024', value: '1024x1024' },
  { label: '横屏 1280x720', value: '1280x720' },
  { label: '竖屏 720x1280', value: '720x1280' },
  { label: '全高清 1920x1080', value: '1920x1080' },
  { label: '竖屏全高清 1080x1920', value: '1080x1920' },
  { label: '2K宽屏 2048x1080', value: '2048x1080' },
  { label: '4K超清 3840x2160', value: '3840x2160' }
]

const chatContainer = ref(null)
const pendingTasks = ref(new Map())
const fileInputRef = ref(null)

const loadContents = async () => {
  loading.value = true
  try {
    const res = await getMyContent({ page: 1, size: 50, type: generateType.value })
    if (res.code === 200) {
      contents.value = (res.data.records || []).reverse()
      nextTick(() => {
        scrollToBottom()
      })
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const scrollToBottom = () => {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const handleFileSelect = () => {
  fileInputRef.value?.click()
}

// 视频生成参考图片支持的类型
const ALLOWED_VIDEO_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/jpg']

const handleFileChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  
  if (!ALLOWED_VIDEO_IMAGE_TYPES.includes(file.type)) {
    ElMessage.warning('图片格式不支持，仅支持 jpg、jpeg、png 格式')
    event.target.value = ''
    return
  }
  
  uploading.value = true
  try {
    const res = await uploadFile(file)
    if (res && res.startsWith('http')) {
      videoParams.value.imageUrl = res
      ElMessage.success('图片上传成功')
    } else {
      ElMessage.error('上传失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

const handleGenerate = async () => {
  const prompt = generateType.value === 'image' ? imageParams.value.prompt : videoParams.value.prompt
  if (!prompt.trim()) {
    ElMessage.warning('请输入提示词')
    return
  }

  generating.value = true
  
  const tempId = 'temp-' + Date.now()
  contents.value.push({
    id: tempId,
    type: generateType.value,
    prompt: prompt,
    status: 0,
    createTime: new Date().toLocaleString(),
    isGenerating: true
  })
  
  nextTick(() => scrollToBottom())

  try {
    if (generateType.value === 'image') {
      const res = await generateImage({
        prompt: imageParams.value.prompt,
        size: imageParams.value.size
      })
      
      if (res.code === 200) {
        const index = contents.value.findIndex(c => c.id === tempId)
        if (index !== -1) {
          contents.value[index] = {
            ...contents.value[index],
            resultUrl: res.data,
            status: 1,
            isGenerating: false
          }
        }
        ElMessage.success('图片生成成功')
        imageParams.value.prompt = ''
      }
    } else {
      const res = await submitVideoTask({
        prompt: videoParams.value.prompt,
        imageUrl: videoParams.value.imageUrl || null,
        size: videoParams.value.size,
        quality: videoParams.value.quality,
        duration: videoParams.value.duration,
        fps: videoParams.value.fps,
        withAudio: videoParams.value.withAudio
      })
      
      if (res.code === 200 && res.data) {
        const taskId = res.data
        console.log('视频任务ID:', taskId)
        if (taskId) {
          pendingTasks.value.set(tempId, taskId)
          startVideoPolling(tempId, taskId)
          videoParams.value.prompt = ''
        } else {
          const index = contents.value.findIndex(c => c.id === tempId)
          if (index !== -1) {
            contents.value[index].isGenerating = false
            contents.value[index].status = 0
          }
          ElMessage.error('视频任务提交失败，未获取到任务ID')
        }
      }
    }
  } catch (error) {
    console.error(error)
    const index = contents.value.findIndex(c => c.id === tempId)
    if (index !== -1) {
      contents.value[index].isGenerating = false
      contents.value[index].status = 0
    }
    ElMessage.error('生成失败')
  } finally {
    generating.value = false
  }
}

const startVideoPolling = (tempId, taskId) => {
  const poll = async () => {
    if (!pendingTasks.value.has(tempId)) return
    
    try {
      const res = await queryVideoTask(taskId)
      if (res.code === 200) {
        const task = res.data
        // 注意：后端返回的状态可能是 SUCCESS、FAIL、PROCESSING 等
        const status = task.status?.toUpperCase()
        if (status === 'SUCCESS') {
          const index = contents.value.findIndex(c => c.id === tempId)
          if (index !== -1) {
            contents.value[index] = {
              ...contents.value[index],
              resultUrl: task.videoUrl,
              status: 1,
              isGenerating: false
            }
          }
          pendingTasks.value.delete(tempId)
          ElMessage.success('视频生成成功')
          nextTick(() => scrollToBottom())
        } else if (status === 'FAIL' || status === 'FAILED') {
          const index = contents.value.findIndex(c => c.id === tempId)
          if (index !== -1) {
            contents.value[index].isGenerating = false
            contents.value[index].status = 0
          }
          pendingTasks.value.delete(tempId)
          ElMessage.error(task.errorMessage || '视频生成失败')
        } else {
          // PROCESSING 或其他状态，继续轮询
          setTimeout(poll, 3000)
        }
      }
    } catch (error) {
      console.error(error)
      pendingTasks.value.delete(tempId)
    }
  }
  
  setTimeout(poll, 3000)
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm('确定删除该记录吗？', '提示', { type: 'warning' })
    const res = await deleteMyContent(item.id)
    if (res.code === 200) {
      contents.value = contents.value.filter(c => c.id !== item.id)
      ElMessage.success('删除成功')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleDownload = (url) => {
  window.open(url, '_blank')
}

const handleTypeChange = () => {
  loadContents()
}

const clearReferenceImage = () => {
  videoParams.value.imageUrl = ''
}

onMounted(() => {
  loadContents()
})

onUnmounted(() => {
  pendingTasks.value.clear()
})
</script>

<template>
  <div class="generate-view">
    <div class="params-panel">
      <div class="panel-header">
        <h3>生成参数</h3>
        <el-radio-group v-model="generateType" @change="handleTypeChange" size="small">
          <el-radio-button value="image">图片</el-radio-button>
          <el-radio-button value="video">视频</el-radio-button>
        </el-radio-group>
      </div>
      
      <div class="panel-content">
        <template v-if="generateType === 'image'">
          <div class="param-group">
            <label class="param-label">图片尺寸</label>
            <el-select v-model="imageParams.size" style="width: 100%">
              <el-option
                v-for="opt in imageSizeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </div>
          
          <div class="param-tips">
            <el-icon><InfoFilled /></el-icon>
            <span>提示词越详细，生成效果越好</span>
          </div>
        </template>
        
        <template v-else>
          <div class="param-group">
            <label class="param-label">视频尺寸</label>
            <el-select v-model="videoParams.size" style="width: 100%">
              <el-option
                v-for="opt in videoSizeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </div>
          
          <div class="param-group">
            <label class="param-label">生成质量</label>
            <el-radio-group v-model="videoParams.quality">
              <el-radio value="speed">速度优先</el-radio>
              <el-radio value="quality">质量优先</el-radio>
            </el-radio-group>
          </div>
          
          <div class="param-group">
            <label class="param-label">视频时长</label>
            <el-radio-group v-model="videoParams.duration">
              <el-radio :value="5">5秒</el-radio>
              <el-radio :value="10">10秒</el-radio>
            </el-radio-group>
          </div>
          
          <div class="param-group">
            <label class="param-label">帧率</label>
            <el-radio-group v-model="videoParams.fps">
              <el-radio :value="30">30 FPS</el-radio>
              <el-radio :value="60">60 FPS</el-radio>
            </el-radio-group>
          </div>
          
          <div class="param-group">
            <label class="param-label">AI音效</label>
            <el-switch v-model="videoParams.withAudio" />
          </div>
          
          <div class="param-group">
            <label class="param-label">参考图片（可选）</label>
            <div class="upload-area">
              <input 
                type="file" 
                ref="fileInputRef" 
                accept=".jpg,.jpeg,.png" 
                style="display: none" 
                @change="handleFileChange"
              />
              <div 
                v-if="!videoParams.imageUrl" 
                class="upload-btn"
                @click="handleFileSelect"
              >
                <el-icon :size="24"><Plus /></el-icon>
                <span>上传图片</span>
              </div>
              <div v-else class="preview-area">
                <el-image :src="videoParams.imageUrl" fit="cover" class="preview-image" />
                <div class="preview-actions">
                  <el-button size="small" @click="handleFileSelect" :loading="uploading">
                    更换
                  </el-button>
                  <el-button size="small" type="danger" @click="clearReferenceImage">
                    清除
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
    
    <div class="chat-area">
      <div class="chat-header">
        <h2>{{ generateType === 'image' ? 'AI 图片生成' : 'AI 视频生成' }}</h2>
        <span class="subtitle">描述你想要的内容，AI为你创造</span>
      </div>
      
      <div class="chat-messages" ref="chatContainer" v-loading="loading">
        <div class="empty-state" v-if="contents.length === 0 && !loading">
          <el-icon :size="64" color="#ddd"><Picture /></el-icon>
          <p>开始创作你的第一个{{ generateType === 'image' ? '图片' : '视频' }}吧</p>
        </div>
        
        <div 
          v-for="item in contents" 
          :key="item.id" 
          class="message-item"
        >
          <div class="message-prompt">
            <div class="prompt-header">
              <el-icon><User /></el-icon>
              <span>你的描述</span>
            </div>
            <p class="prompt-text">{{ item.prompt }}</p>
          </div>
          
          <div class="message-result">
            <div class="result-header">
              <el-icon><Cpu /></el-icon>
              <span>生成结果</span>
              <el-tag v-if="item.isGenerating" type="warning" size="small">
                <el-icon class="is-loading"><Loading /></el-icon>
                生成中...
              </el-tag>
              <el-tag v-else-if="item.status === 1" type="success" size="small">成功</el-tag>
              <el-tag v-else type="danger" size="small">失败</el-tag>
            </div>
            
            <div class="result-content" v-if="item.isGenerating">
              <div class="generating-placeholder">
                <el-icon class="is-loading" :size="48"><Loading /></el-icon>
                <p>正在生成中，请稍候...</p>
              </div>
            </div>
            
            <div class="result-content" v-else-if="item.resultUrl">
              <el-image
                v-if="item.type === 'image'"
                :src="item.resultUrl"
                fit="contain"
                :preview-src-list="[item.resultUrl]"
                class="result-image"
              />
              <video
                v-else
                :src="item.resultUrl"
                controls
                class="result-video"
              />
              
              <div class="result-actions">
                <el-button type="primary" size="small" @click="handleDownload(item.resultUrl)">
                  <el-icon><Download /></el-icon>
                  下载
                </el-button>
                <el-button type="danger" size="small" @click="handleDelete(item)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </div>
            
            <div class="result-content result-failed" v-else>
              <el-icon :size="32" color="#f56c6c"><WarningFilled /></el-icon>
              <p>生成失败，请重试</p>
            </div>
          </div>
        </div>
      </div>
      
      <div class="chat-input">
        <div class="input-wrapper">
          <el-input
            v-if="generateType === 'image'"
            v-model="imageParams.prompt"
            type="textarea"
            :rows="3"
            placeholder="描述你想要生成的图片，例如：一只可爱的橘猫坐在窗台上，阳光洒在它身上，温馨治愈的风格..."
            :disabled="generating"
            @keydown.enter.ctrl="handleGenerate"
          />
          <el-input
            v-else
            v-model="videoParams.prompt"
            type="textarea"
            :rows="3"
            placeholder="描述你想要生成的视频，例如：海浪轻轻拍打着沙滩，夕阳西下，海鸥在天空飞翔..."
            :disabled="generating"
            @keydown.enter.ctrl="handleGenerate"
          />
          <div class="input-footer">
            <span class="tip">Ctrl + Enter 发送</span>
            <el-button 
              type="primary" 
              :loading="generating"
              @click="handleGenerate"
            >
              <el-icon v-if="!generating"><Position /></el-icon>
              {{ generating ? '生成中...' : '开始生成' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.generate-view {
  display: flex;
  height: calc(100vh - var(--header-height) - var(--spacing-lg) * 2);
  background: var(--color-white);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.params-panel {
  width: 280px;
  background: var(--color-bg);
  border-right: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: var(--spacing-md);
  border-bottom: 1px solid var(--color-border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h3 {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: 600;
}

.panel-content {
  padding: var(--spacing-md);
  flex: 1;
  overflow-y: auto;
}

.param-group {
  margin-bottom: var(--spacing-md);
}

.param-label {
  display: block;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-xs);
}

.param-tips {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm);
  background: var(--color-primary-light);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.upload-area {
  width: 100%;
}

.upload-btn {
  width: 100%;
  height: 120px;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.upload-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.preview-area {
  width: 100%;
}

.preview-image {
  width: 100%;
  height: 120px;
  border-radius: var(--radius-md);
}

.preview-actions {
  display: flex;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-sm);
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-light);
}

.chat-header h2 {
  margin: 0 0 var(--spacing-xs) 0;
  font-size: var(--font-size-lg);
  font-weight: 600;
}

.chat-header .subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-text-muted);
}

.empty-state p {
  margin-top: var(--spacing-md);
  font-size: var(--font-size-base);
}

.message-item {
  margin-bottom: var(--spacing-lg);
  max-width: 800px;
}

.message-prompt {
  background: var(--color-primary-light);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-sm);
}

.prompt-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-sm);
  color: var(--color-primary-dark);
  margin-bottom: var(--spacing-xs);
  font-weight: 500;
}

.prompt-text {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text);
  line-height: 1.6;
}

.message-result {
  background: var(--color-bg-secondary);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.result-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-bg-tertiary);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.result-header .el-tag {
  margin-left: auto;
}

.result-content {
  padding: var(--spacing-md);
}

.generating-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-xl);
  color: var(--color-text-muted);
}

.generating-placeholder p {
  margin-top: var(--spacing-sm);
}

.result-image {
  width: 100%;
  border-radius: var(--radius-md);
}

.result-video {
  width: 100%;
  border-radius: var(--radius-md);
}

.result-actions {
  display: flex;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
  justify-content: flex-end;
}

.result-failed {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-xl);
  color: var(--color-danger);
}

.result-failed p {
  margin-top: var(--spacing-sm);
}

.chat-input {
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--color-border-light);
  background: var(--color-bg);
}

.input-wrapper {
  max-width: 800px;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--spacing-sm);
}

.input-footer .tip {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}
</style>
